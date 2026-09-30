package actions.cia

import actions.BackgroundActionRunner.background
import build.ActiveBuild
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import deploy.CiaPublisher
import java.nio.file.Path

class BuildCiaAction : AnAction() {
    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val root = Path.of(project.basePath ?: return)
        background(project, "Building Nintendo 3DS CIA") {
            val cia = CiaPublisher.buildAndExport(root, ActiveBuild.directory(project))
            "CIA exported to $cia"
        }
    }
}
