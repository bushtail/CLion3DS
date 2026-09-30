package actions.dev3ds

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import project.Dev3DSProjectSetup
import java.nio.file.Path

class RepairProjectAction : AnAction() {
    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
    override fun update(event: AnActionEvent) {
        val base = event.project?.basePath
        event.presentation.isEnabled = base != null && Dev3DSProjectSetup.isDev3DSProject(Path.of(base))
    }

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val base = project.basePath ?: return
        Dev3DSProjectSetup.start(project, Path.of(base))
    }
}
