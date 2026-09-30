@file:Suppress("UnstableApiUsage")

package toolchain.bootstrap

import com.intellij.platform.eel.fs.EelFiles
import toolchain.detection.DevkitProDetector
import toolchain.environment.EHostPlatform
import toolchain.environment.HostPlatformDetector
import toolchain.installation.DevkitProInstallation
import toolchain.installation.DevkitProInstallRequest
import toolchain.installation.DevkitProInstallResult
import toolchain.installation.EDevkitProInstallationKind
import toolchain.packages.DevkitProConstants
import toolchain.process.ToolchainProcessRequest
import toolchain.process.ToolchainProcessRunner
import toolchain.validation.DevkitProValidator
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import org.apache.commons.compress.archivers.sevenz.SevenZFile

class DevkitProBootstrapper(
    private val detector: DevkitProDetector = DevkitProDetector(),
    private val validator: DevkitProValidator = DevkitProValidator(),
    private val packageManager: DevkitProPackageManager = DevkitProPackageManager(),
    private val downloadService: DevkitProDownloadService = DevkitProDownloadService(),
    private val processRunner: ToolchainProcessRunner = ToolchainProcessRunner()
) {
    fun install(request: DevkitProInstallRequest = DevkitProInstallRequest()): DevkitProInstallResult {
        val host = HostPlatformDetector.detect()

        var installation = request.destination?.toAbsolutePath()?.normalize()
            ?.takeIf(Files::isDirectory)
            ?.let { DevkitProInstallation(it, host, EDevkitProInstallationKind.Existing) }
            ?: if (request.destination == null) {
                detector.detectRootEvenIfIncomplete()
            } else {
                null
            }

        if (installation == null || (host == EHostPlatform.Windows && !Files.isRegularFile(installation.layout.windowsPacman))) {
            installation = when (host) {
                EHostPlatform.Windows -> {
                    bootstrapWindows(request)
                }

                EHostPlatform.Linux,
                EHostPlatform.MacOS -> {
                    bootstrapUnix(request, host)
                }
            }
        }

        if (request.updatePackages) {
            val update = packageManager.update(installation)

            if (!update.success) {
                return failure(installation, "Failed to update devkitPro packages:\n${update.standardError}")
            }

            if (host == EHostPlatform.Windows) {
                val remaining = packageManager.update(installation)
                if (!remaining.success) {
                    return failure(installation, "Failed to finish MSYS2 update:\n${remaining.standardError}")
                }
            }
        }

        val install = packageManager.installDev3DSToolchain(installation)

        if (!install.success) {
            return failure(
                installation,
                "Failed to install ${DevkitProConstants.ENV_DEVKIT_PRO} 3DS packages:\n${install.standardError}"
            )
        }

        val managedInstallation = installation.copy(kind = EDevkitProInstallationKind.Managed)

        val validation = validator.validate(managedInstallation)

        if (!validation.isValid) {
            return DevkitProInstallResult(
                installation = managedInstallation,
                validation = validation,
                success = false,
                message = validation.issues.joinToString("\n") {
                    it.message
                }
            )
        }

        return DevkitProInstallResult(
            installation = managedInstallation,
            validation = validation,
            success = true,
            message = "Nintendo 3DS toolchain installed successfully."
        )
    }

    private fun bootstrapWindows(request: DevkitProInstallRequest): DevkitProInstallation {
        val destination = (request.destination ?: Path.of(DevkitProConstants.DEFAULT_ROOT_WINDOWS))
            .toAbsolutePath().normalize()

        require(!destination.toString().contains(' ')) {
            "The devkitPro Windows installer does not support an installation path containing spaces: $destination"
        }

        val temporaryDirectory = Files.createTempDirectory("clion3ds-devkitpro-")
        try {
            val archiveName = currentMsysArchiveName()
            val archive = temporaryDirectory.resolve(archiveName)
            downloadService.download(URI.create("https://downloads.devkitpro.org/$archiveName"), archive)
            extractMsysArchive(archive, destination)
            configureMsysMounts(destination)
            val initialize = processRunner.run(ToolchainProcessRequest(
                executable = destination.resolve("msys2/usr/bin/bash.exe"),
                arguments = listOf("--login", "-c", "exit"),
                environment = mapOf("CHERE_INVOKING" to "1"),
                timeout = Duration.ofMinutes(10)
            ))
            if (!initialize.success) {
                error("MSYS2 initialization failed:\n${initialize.standardError}")
            }
        } finally {
            Files.list(temporaryDirectory).use { entries -> entries.forEach(Files::deleteIfExists) }
            Files.deleteIfExists(temporaryDirectory)
        }

        return DevkitProInstallation(
            root = destination,
            host = EHostPlatform.Windows,
            kind = EDevkitProInstallationKind.Managed
        )
    }

    private fun currentMsysArchiveName(): String {
        val request = HttpRequest.newBuilder(URI.create("https://downloads.devkitpro.org/devkitProUpdate.ini"))
            .timeout(Duration.ofSeconds(30)).GET().build()
        val response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString())
        require(response.statusCode() in 200..299) { "Unable to read devkitPro package manifest: HTTP ${response.statusCode()}" }
        val section = response.body().substringAfter("[msys2]", "").substringBefore("[")
        val name = Regex("(?m)^File\\s*=\\s*([^\\r\\n]+)").find(section)?.groupValues?.get(1)?.trim()
            ?: error("devkitPro package manifest does not name an MSYS2 archive")
        require(Regex("[A-Za-z0-9._-]+\\.7z").matches(name)) { "Invalid MSYS2 archive name: $name" }
        return name
    }

    private fun extractMsysArchive(archive: Path, destination: Path) {
        Files.createDirectories(destination)
        SevenZFile.builder().setFile(archive.toFile()).get().use { sevenZ ->
            val buffer = ByteArray(65536)
            while (true) {
                val entry = sevenZ.nextEntry ?: break
                val target = destination.resolve(entry.name.replace('\\', '/')).normalize()
                require(target.startsWith(destination)) { "Unsafe MSYS2 archive entry: ${entry.name}" }
                if (entry.isDirectory) {
                    Files.createDirectories(target)
                } else {
                    Files.createDirectories(target.parent)
                    Files.newOutputStream(target).use { output ->
                        var remaining = entry.size
                        while (remaining > 0) {
                            val count = sevenZ.read(buffer, 0, minOf(buffer.size.toLong(), remaining).toInt())
                            if (count < 0) {
                                error("Truncated MSYS2 archive entry: ${entry.name}")
                            }
                            output.write(buffer, 0, count)
                            remaining -= count
                        }
                    }
                }
            }
        }
    }

    private fun configureMsysMounts(destination: Path) {
        val fstab = destination.resolve("msys2/etc/fstab")
        val root = destination.toString().replace('\\', '/')
        val users = Path.of(System.getProperty("user.home")).parent.toString().replace('\\', '/')
        Files.writeString(fstab, EelFiles.readString(fstab)
            .replace("#{DEVKITPRO}", root)
            .replace("#{PROFILES_ROOT}", users))
    }

    private fun bootstrapUnix(request: DevkitProInstallRequest, host: EHostPlatform): DevkitProInstallation {
        val existing = detector.detectRootEvenIfIncomplete()

        if (existing != null) {
            return existing
        }

        val root = (request.destination ?: Path.of(DevkitProConstants.DEFAULT_ROOT_UNIX))
            .toAbsolutePath().normalize()

        require(root == Path.of(DevkitProConstants.DEFAULT_ROOT_UNIX) || Files.isDirectory(root)) {
            "On Unix, install devkitPro with its package manager first, then choose its existing directory: $root"
        }

        packageManager.pacmanExecutable(
            DevkitProInstallation(
                root = root,
                host = host,
                kind = EDevkitProInstallationKind.Managed
            )
        )

        return DevkitProInstallation(
            root = root,
            host = host,
            kind = EDevkitProInstallationKind.Managed
        )
    }

    private fun failure(installation: DevkitProInstallation, message: String): DevkitProInstallResult {
        return DevkitProInstallResult(
            installation = installation,
            validation = validator.validate(installation),
            success = false,
            message = message
        )
    }
}
