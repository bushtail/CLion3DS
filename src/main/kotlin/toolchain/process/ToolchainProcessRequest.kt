package toolchain.process

import java.nio.file.Path
import java.time.Duration

data class ToolchainProcessRequest(
    val executable: Path,
    val arguments: List<String> = emptyList(),
    val workingDirectory: Path? = null,
    val environment: Map<String, String> = emptyMap(),
    val timeout: Duration = Duration.ofMinutes(15)
)