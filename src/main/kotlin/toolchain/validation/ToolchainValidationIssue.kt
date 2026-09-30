package toolchain.validation

import java.nio.file.Path

data class ToolchainValidationIssue(val component: EToolchainComponent, val error: EToolchainValidationError, val path: Path?, val message: String)