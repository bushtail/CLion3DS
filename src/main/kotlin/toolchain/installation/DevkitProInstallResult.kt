package toolchain.installation

import toolchain.validation.ToolchainValidationResult

data class DevkitProInstallResult(
    val installation: DevkitProInstallation?,
    val validation: ToolchainValidationResult?,
    val success: Boolean,
    val message: String
)