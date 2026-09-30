package actions.cia

import actions.ActionDialogs.input
import actions.BackgroundActionRunner.background
import build.ActiveBuild
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import deploy.CiaPublisher
import java.nio.file.Path

class SendCiaAction : AnAction() {
    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val root = Path.of(project.basePath ?: return)
        val address = input("3DS IP address (FBI > Remote Install > Receive URLs over the network):") ?: return
        background(project, "Sending Nintendo 3DS CIA") {
            val cia = CiaPublisher.buildAndExport(root, ActiveBuild.directory(project))
            CiaPublisher.sendToFbi(cia, address)
            "Sent ${cia.fileName} to FBI at $address. Confirm installation on the console."
        }
    }
}
