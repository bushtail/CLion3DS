package toolchain.bootstrap

import com.intellij.openapi.application.PathManager
import java.nio.file.Files
import java.nio.file.Path
import java.util.Comparator
import java.util.zip.ZipInputStream

class AzaharInstaller(
    private val resolver: GithubReleaseResolver = GithubReleaseResolver(),
    private val downloader: DevkitProDownloadService = DevkitProDownloadService()
) {
    fun install(): Path {
        require(System.getProperty("os.name").lowercase().contains("win")) {
            "Automatic emulator installation currently supports Windows only. Choose an existing emulator executable."
        }
        val asset = resolver.latestAsset("AzaharPlus/AzaharPlus") {
            it.matches(Regex("azaharplus-[0-9.]+-[A-Z]-windows\\.zip"))
        }
        val version = asset.name.removePrefix("azaharplus-").removeSuffix("-windows.zip")
        val parent = Path.of(PathManager.getSystemPath(), "clion3ds", "emulator")
        val destination = parent.resolve(version)
        findExecutable(destination)?.let {
            return it
        }

        Files.createDirectories(parent)
        val archive = Files.createTempFile(parent, "emulator-", ".zip")
        val staging = Files.createTempDirectory(parent, "emulator-extract-")
        try {
            downloader.download(asset.downloadUri, archive, asset.sha256)
            extract(archive, staging)
            val executable = findExecutable(staging)
                ?: error("The emulator archive did not contain its executable")
            if (Files.exists(destination)) {
                check(destination.normalize().startsWith(parent.normalize()) && destination != parent)
                deleteTree(destination)
            }
            Files.move(staging, destination)
            return destination.resolve(staging.relativize(executable))
        } finally {
            Files.deleteIfExists(archive)
            if (Files.exists(staging)) {
                deleteTree(staging)
            }
        }
    }

    private fun deleteTree(directory: Path) {
        Files.walk(directory).use { paths ->
            paths.sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists)
        }
    }

    private fun findExecutable(directory: Path): Path? {
        if (!Files.isDirectory(directory)) {
            return null
        }
        return Files.walk(directory, 5).use { paths ->
            paths.filter { Files.isRegularFile(it) && it.fileName.toString().equals("azahar.exe", ignoreCase = true) }
                .findFirst().orElse(null)
        }
    }

    internal fun extract(archive: Path, destination: Path) {
        var expandedBytes = 0L
        ZipInputStream(Files.newInputStream(archive)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                val target = destination.resolve(entry.name).normalize()
                require(target.startsWith(destination)) {
                    "Unsafe path in emulator archive: ${entry.name}"
                }
                if (entry.isDirectory) {
                    Files.createDirectories(target)
                } else {
                    Files.createDirectories(target.parent)
                    Files.newOutputStream(target).use { output ->
                        val buffer = ByteArray(8192)
                        while (true) {
                            val count = zip.read(buffer)
                            if (count < 0) {
                                break
                            }
                            expandedBytes += count
                            require(expandedBytes <= 2_000_000_000L) { "Emulator archive is too large" }
                            output.write(buffer, 0, count)
                        }
                    }
                }
                zip.closeEntry()
            }
        }
    }
}
