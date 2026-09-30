package toolchain.validation

import toolchain.installation.DevkitProInstallation

data class ToolchainValidationResult(val installation: DevkitProInstallation, val issues: List<ToolchainValidationIssue>) {
    val isValid: Boolean
        get() {
            return issues.isEmpty()
        }
}