package toolchain.bootstrap

import toolchain.environment.EHostPlatform
import toolchain.installation.DevkitProInstallation
import toolchain.packages.DevkitProExecutableNames
import toolchain.packages.DevkitProPackageNames
import toolchain.process.ToolchainProcessRequest
import toolchain.process.ToolchainProcessResult
import toolchain.process.ToolchainProcessRunner
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration

class DevkitProPackageManager(private val processRunner: ToolchainProcessRunner = ToolchainProcessRunner()) {
    fun update(installation: DevkitProInstallation): ToolchainProcessResult {
        return runPacman(
            installation = installation,
            arguments = listOf("-Syu", "--noconfirm")
        )
    }

    fun installDev3DSToolchain(installation: DevkitProInstallation): ToolchainProcessResult {
        val hostPackages = if (installation.host == EHostPlatform.Windows) {
            listOf("cmake", "ninja")
        } else {
            emptyList()
        }
        return runPacman(
            installation = installation,
            arguments = listOf(
                "-S",
                "--needed",
                "--noconfirm",
                DevkitProPackageNames.DEV_3DS_DEVELOPMENT_GROUP
            ) + hostPackages
        )
    }

    fun pacmanExecutable(installation: DevkitProInstallation): Path {
        return when (installation.host) {
            EHostPlatform.Windows -> {
                installation.layout.windowsPacman
            }

            EHostPlatform.Linux,
            EHostPlatform.MacOS -> {
                findPacmanCommand() ?: error("Neither dkp-pacman nor pacman was found in PATH.")
            }
        }
    }

    private fun runPacman(installation: DevkitProInstallation, arguments: List<String>): ToolchainProcessResult {
        val executable = pacmanExecutable(installation)

        return processRunner.run(
            ToolchainProcessRequest(
                executable = executable,
                arguments = arguments,
                environment = installation.environment.variables(),
                timeout = Duration.ofMinutes(30)
            )
        )
    }

    private fun findPacmanCommand(): Path? {
        val path = System.getenv("PATH") ?: return null

        val extensions = if (System.getProperty("os.name").lowercase().contains("win")) {
            listOf(".exe", ".cmd", ".bat", "")
        } else {
            listOf("")
        }

        for (directory in path.split(File.pathSeparator)) {
            if (directory.isBlank()) {
                continue
            }

            for (name in listOf(DevkitProExecutableNames.DKP_PACMAN, DevkitProExecutableNames.PACMAN)) {
                for (extension in extensions) {
                    val candidate = Path.of(directory, "$name$extension")

                    if (Files.isRegularFile(candidate) && (extension.isNotEmpty() || Files.isExecutable(candidate))) {
                        return candidate
                    }
                }
            }
        }

        return null
    }
}
