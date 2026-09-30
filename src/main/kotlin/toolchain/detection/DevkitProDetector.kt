package toolchain.detection

import toolchain.environment.EHostPlatform
import toolchain.environment.HostPlatformDetector
import toolchain.installation.DevkitProInstallation
import toolchain.installation.EDevkitProInstallationKind
import toolchain.packages.DevkitProConstants
import java.nio.file.Files
import java.nio.file.Path

class DevkitProDetector {

    fun detectRootEvenIfIncomplete(): DevkitProInstallation? {
        val host = HostPlatformDetector.detect()

        val candidates = buildList {
            environmentRoot()?.let(::add)

            when (host) {
                EHostPlatform.Windows -> {
                    add(Path.of(DevkitProConstants.DEFAULT_ROOT_WINDOWS))
                }
                EHostPlatform.Linux,
                EHostPlatform.MacOS -> {
                    add(Path.of(DevkitProConstants.DEFAULT_ROOT_UNIX))
                }
            }
        }.distinct()

        val root = candidates.firstOrNull(Files::isDirectory) ?: return null

        return DevkitProInstallation(
            root = root.toAbsolutePath().normalize(),
            host = host,
            kind = EDevkitProInstallationKind.Existing
        )
    }

    private fun environmentRoot(): Path? {
        val value = System.getenv(DevkitProConstants.ENV_DEVKIT_PRO)

        if (value.isNullOrBlank()) {
            return null
        }

        return Path.of(value)
    }
}
