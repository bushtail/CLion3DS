package actions.dev3ds

import actions.BackgroundActionRunner.background
import build.ActiveBuild
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import toolchain.Dev3DSToolchain
import toolchain.bootstrap.AzaharInstaller
import java.nio.file.Files
import java.nio.file.Path

class EmulatorAction : AnAction() {
    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
    override fun update(event: AnActionEvent) {
        event.presentation.isEnabledAndVisible = event.project?.basePath != null
    }

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val root = Path.of(project.basePath ?: return)
        background(project, "Launching Nintendo 3DS emulator") {
            val directory = ActiveBuild.directory(project)
            ActiveBuild.build(root, directory)
            val artifact = ActiveBuild.artifact(root, directory)
            val executable = Dev3DSToolchain.emulator()?.takeIf(Files::isRegularFile)
                ?: AzaharInstaller().install().also(Dev3DSToolchain::saveEmulator)
            ProcessBuilder(executable.toString(), artifact.toString()).directory(root.toFile()).start()
            "Launched ${artifact.fileName} from $directory in Emulator."
        }
    }
}
