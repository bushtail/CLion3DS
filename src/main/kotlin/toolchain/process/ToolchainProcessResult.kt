package toolchain.process

data class ToolchainProcessResult(
    val exitCode: Int,
    val standardOutput: String,
    val standardError: String
) {
    val success: Boolean
        get() {
            return exitCode == 0
        }
}
