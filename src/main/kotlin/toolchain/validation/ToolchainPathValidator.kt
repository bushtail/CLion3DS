package toolchain.validation

import toolchain.environment.EHostPlatform
import java.nio.file.Files
import java.nio.file.Path

object ToolchainPathValidator {
    fun validateDirectory(
        path: Path,
        component: EToolchainComponent,
        displayName: String
    ): ToolchainValidationIssue? {
        if (!Files.exists(path)) {
            return ToolchainValidationIssue(
                component = component,
                error = EToolchainValidationError.Missing,
                path = path,
                message = "$displayName was not found at $path"
            )
        }

        if (!Files.isDirectory(path)) {
            return ToolchainValidationIssue(
                component = component,
                error = EToolchainValidationError.InvalidPath,
                path = path,
                message = "$displayName is not a directory: $path"
            )
        }

        return null
    }

    fun validateExecutable(
        path: Path,
        host: EHostPlatform,
        component: EToolchainComponent,
        displayName: String
    ): ToolchainValidationIssue? {
        if (!Files.exists(path)) {
            return ToolchainValidationIssue(
                component = component,
                error = EToolchainValidationError.Missing,
                path = path,
                message = "$displayName was not found at $path"
            )
        }

        if (!Files.isRegularFile(path)) {
            return ToolchainValidationIssue(
                component = component,
                error = EToolchainValidationError.InvalidPath,
                path = path,
                message = "$displayName is not a regular file: $path"
            )
        }

        if (!isExecutable(path, host)) {
            return ToolchainValidationIssue(
                component = component,
                error = EToolchainValidationError.NotExecutable,
                path = path,
                message = "$displayName is not executable: $path"
            )
        }

        return null
    }

    private fun isExecutable(
        path: Path,
        host: EHostPlatform
    ): Boolean {
        return when (host) {
            EHostPlatform.Windows -> {
                when (path.fileName.toString().substringAfterLast('.', "")) {
                    "exe", "EXE",
                    "com", "COM",
                    "bat", "BAT",
                    "cmd", "CMD" -> true

                    else -> false
                }
            }

            EHostPlatform.Linux,
            EHostPlatform.MacOS -> {
                Files.isExecutable(path)
            }
        }
    }
}
