package build

import com.intellij.openapi.application.PathManager
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import toolchain.installation.DevkitProInstallation
import toolchain.environment.EHostPlatform
import toolchain.process.ToolchainProcessRequest

/** CLion ships CMake (and usually Ninja), so use its copy before searching PATH. */
object CMakeTools {
    fun cmake(): Path {
        val executable = if (isWindows()) {
            "cmake.exe"
        } else {
            "cmake"
        }
        return bundled("cmake", executable) ?: Path.of(executable)
    }

    fun ninja(): Path? {
        val executable = if (isWindows()) {
            "ninja.exe"
        } else {
            "ninja"
        }
        return bundled("ninja", executable)
    }

    fun environment(base: Map<String, String>, vararg executables: Path?): Map<String, String> {
        val result = base.toMutableMap()
        val added = executables.filterNotNull().mapNotNull { it.parent?.toString() }.distinct()
        result["PATH"] = (added + base["PATH"].orEmpty()).joinToString(File.pathSeparator)
        return result
    }

    fun configure(installation: DevkitProInstallation, source: Path, build: Path): ToolchainProcessRequest {
        val cmake = cmake()
        val ninja = ninja()
        val toolchainOption = if (installation.host == EHostPlatform.Windows) {
            emptyList()
        } else {
            listOf("-DCMAKE_TOOLCHAIN_FILE=${installation.layout.dev3DSCMakeToolchain}")
        }
        return ToolchainProcessRequest(cmake, listOf("-S", source.toString(), "-B", build.toString()) +
            toolchainOption +
            if (ninja != null) {
                listOf("-G", "Ninja")
            } else {
                emptyList()
            },
            source, environment(installation.environment.variables(), cmake, ninja), Duration.ofMinutes(10))
    }

    fun build(installation: DevkitProInstallation, source: Path, build: Path): ToolchainProcessRequest {
        val cmake = cmake()
        return ToolchainProcessRequest(cmake, listOf("--build", build.toString()),
            source, environment(installation.environment.variables(), cmake, ninja()), Duration.ofMinutes(30))
    }

    private fun bundled(tool: String, filename: String): Path? {
        val directory = Path.of(PathManager.getHomePath(), "bin", tool)
        if (!Files.isDirectory(directory)) {
            return null
        }
        return Files.walk(directory, 6).use { paths ->
            paths.filter { Files.isRegularFile(it) && it.fileName.toString().equals(filename, ignoreCase = isWindows()) }
                .sorted(compareBy { path ->
                    if (isWindows() && path.toString().contains("\\win\\", ignoreCase = true)) {
                        0
                    } else {
                        1
                    }
                })
                .findFirst().orElse(null)
        }
    }

    private fun isWindows(): Boolean {
        return System.getProperty("os.name").lowercase().contains("win")
    }
}
