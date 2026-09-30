package actions.dev3ds

import actions.BackgroundActionRunner.background
import build.ActiveBuild
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import java.nio.file.Path

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
        background(project, "Building Nintendo 3DS project") {
            val directory = ActiveBuild.directory(project)
            val output = ActiveBuild.build(root, directory)
            val artifact = ActiveBuild.artifact(root, directory)
            "Built $artifact\n$output"
        }
    }
}
