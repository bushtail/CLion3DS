package actions.dev3ds

import actions.ActionDialogs.TITLE
import actions.BackgroundActionRunner.background
import build.ActiveBuild
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages
import toolchain.ToolProvisioner
import toolchain.process.ToolchainProcessRequest
import toolchain.process.ToolchainProcessRunner
import java.nio.file.Path
import java.time.Duration

class DeployAction : AnAction() {
    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
    override fun update(event: AnActionEvent) {
        event.presentation.isEnabledAndVisible = event.project?.basePath != null
    }

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val root = Path.of(project.basePath ?: return)
        val address = Messages.showInputDialog("3DS IP address (leave blank for broadcast):", TITLE, Messages.getQuestionIcon())?.trim() ?: return
        background(project, "Deploying to Nintendo 3DS") {
            val directory = ActiveBuild.directory(project)
            ActiveBuild.build(root, directory)
            val file = ActiveBuild.artifact(root, directory)
            val args = if (address.isBlank()) listOf(file.toString())
                else listOf("-a", address, file.toString())
            val installation = ToolProvisioner.ensureToolchain()
            val result = ToolchainProcessRunner().run(
                ToolchainProcessRequest(
                    executable = installation.layout.dev3DSLink, arguments = args,
                    environment = installation.environment.variables(), timeout = Duration.ofMinutes(2)
                )
            )
            if (!result.success) {
                error("3dslink failed (${result.exitCode}):\n${result.standardError.ifBlank { result.standardOutput }}")
            }
            "Deployed ${file.fileName}.\n${result.standardOutput.takeLast(2000)}"
        }
    }
}
