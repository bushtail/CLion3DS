package build

import com.intellij.execution.ExecutionTargetManager
import com.intellij.openapi.project.Project
import com.intellij.platform.eel.fs.EelFiles
import com.jetbrains.cidr.cpp.cmake.CMakeSettings
import toolchain.ToolProvisioner
import toolchain.process.ToolchainProcessRunner
import java.nio.file.Files
import java.nio.file.Path

/** Use the same CMake profile directory as CLion's Build and Clean actions. */
object ActiveBuild {
    fun directory(project: Project): Path {
        val profiles = CMakeSettings.getInstance(project).activeProfiles
        require(profiles.isNotEmpty()) { "Enable a CMake profile for this project first." }
        val selected = ExecutionTargetManager.getActiveTarget(project).id
            .removePrefix("CMakeBuildProfile:")
        val index = profiles.indexOfFirst { it.name == selected }
            .takeIf { it >= 0 }
            ?: if (profiles.size == 1) {
                0
            } else {
                error("Select a CMake profile in CLion before building.")
            }
        return CMakeSettings.getEffectiveProfileGenerationDirs(project, profiles)[index]
            .toPath().toAbsolutePath().normalize()
    }

    fun build(root: Path, directory: Path): String {
        val installation = ToolProvisioner.ensureToolchain()
        val runner = ToolchainProcessRunner()
        if (!Files.isRegularFile(directory.resolve("CMakeCache.txt"))) {
            val configure = runner.run(CMakeTools.configure(installation, root, directory))
            if (!configure.success) {
                error("CMake configuration failed (${configure.exitCode}):\n${configure.standardError.ifBlank { configure.standardOutput }.takeLast(4000)}")
            }
        }
        val result = runner.run(CMakeTools.build(installation, root, directory))
        if (!result.success) {
            error("Build failed (${result.exitCode}):\n${result.standardError.ifBlank { result.standardOutput }.takeLast(4000)}")
        }
        return result.standardOutput.takeLast(2000)
    }

    fun artifact(root: Path, directory: Path): Path {
        val file = directory.resolve("${projectName(root)}.3dsx")
        require(Files.isRegularFile(file)) { "Build did not produce $file" }
        return file
    }

    fun projectName(root: Path): String {
        val cmake = root.resolve("CMakeLists.txt")
        require(Files.isRegularFile(cmake)) { "No CMakeLists.txt found at $root" }
        return Regex("(?im)^\\s*project\\(\\s*([A-Za-z][A-Za-z0-9_]*)\\b")
            .find(EelFiles.readString(cmake))?.groupValues?.get(1)
            ?: error("Could not find the CMake project name in $cmake")
    }

}
