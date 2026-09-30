package actions.dev3ds

import actions.ActionDialogs.TITLE
import actions.ActionDialogs.input
import actions.ActionDialogs.report
import actions.BackgroundActionRunner.background
import actions.dev3ds.Dev3DSArtifactFinder.findArtifact
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages
import toolchain.ToolProvisioner
import toolchain.process.ToolchainProcessRequest
import toolchain.process.ToolchainProcessRunner
import java.nio.file.Files
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
        val artifact = findArtifact(root)
        val fileText = input("3DSX file to deploy:", artifact?.toString().orEmpty()) ?: return
        val file = Path.of(fileText).toAbsolutePath().normalize()
        if (!Files.isRegularFile(file) || !file.fileName.toString().endsWith(".3dsx")) {
            report("Choose an existing .3dsx file.", true)
            return
        }
        val address = Messages.showInputDialog("3DS IP address (leave blank for broadcast):", TITLE, Messages.getQuestionIcon())?.trim() ?: return
        val args = if (address.isBlank()) {
            listOf(file.toString())
        } else {
            listOf("-a", address, file.toString())
        }
        background(project, "Deploying to Nintendo 3DS") {
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
