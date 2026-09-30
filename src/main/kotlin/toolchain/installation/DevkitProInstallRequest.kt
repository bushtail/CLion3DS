package toolchain.installation

import java.nio.file.Path

data class DevkitProInstallRequest(
    val destination: Path? = null,
    val updatePackages: Boolean = true
)