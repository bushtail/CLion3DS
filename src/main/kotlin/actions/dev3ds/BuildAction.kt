package actions.dev3ds

import actions.ActionDialogs.input
import actions.ActionDialogs.report
import actions.BackgroundActionRunner.background
import build.CMakeTools
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import toolchain.ToolProvisioner
import toolchain.process.ToolchainProcessRequest
import toolchain.process.ToolchainProcessRunner
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration

class BuildAction : AnAction() {
    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
    override fun update(event: AnActionEvent) {
        event.presentation.isEnabledAndVisible = event.project?.basePath != null
    }

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val root = Path.of(project.basePath ?: return)
        if (!Files.isRegularFile(root.resolve("CMakeLists.txt"))) {
            report("No CMakeLists.txt found at $root.", true)
            return
        }
        val defaultBuild = root.resolve("build/3ds")
        val buildText = input("CMake build directory:", defaultBuild.toString()) ?: return
        val buildDirectory = Path.of(buildText).toAbsolutePath().normalize()
        background(project, "Building Nintendo 3DS project") {
            val installation = ToolProvisioner.ensureToolchain()
            val cmake = CMakeTools.cmake()
            val ninja = CMakeTools.ninja()
            val environment = CMakeTools.environment(installation.environment.variables(), cmake, ninja)
            if (!Files.isRegularFile(buildDirectory.resolve("CMakeCache.txt"))) {
                val configureArgs = mutableListOf("-S", root.toString(), "-B", buildDirectory.toString(),
                    "-DCMAKE_TOOLCHAIN_FILE=${installation.root.resolve("cmake/3DS.cmake")}")
                if (ninja != null) {
                    configureArgs.addAll(listOf("-G", "Ninja"))
                }
                val configure = ToolchainProcessRunner().run(
                    ToolchainProcessRequest(
                        executable = cmake, arguments = configureArgs,
                        workingDirectory = root, environment = environment, timeout = Duration.ofMinutes(10)
                    )
                )
                if (!configure.success) {
                    error("CMake configuration failed (${configure.exitCode}):\n${configure.standardError.ifBlank { configure.standardOutput }}")
                }
            }
            val result = ToolchainProcessRunner().run(
                ToolchainProcessRequest(
                    executable = cmake, arguments = listOf("--build", buildDirectory.toString()),
                    workingDirectory = root, environment = environment,
                    timeout = Duration.ofMinutes(30)
                )
            )
            if (!result.success) {
                error("Build failed (${result.exitCode}):\n${result.standardError.ifBlank { result.standardOutput }}")
            }
            "Build finished.\n${result.standardOutput.takeLast(2000)}"
        }
    }
}
