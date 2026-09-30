package actions.dev3ds

import actions.ActionDialogs.input
import actions.ActionDialogs.report
import actions.BackgroundActionRunner.background
import actions.dev3ds.Dev3DSArtifactFinder.findArtifact
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
        val root = Path.of(event.project?.basePath ?: return)
        val artifactText = input("3DSX file to run:", findArtifact(root)?.toString().orEmpty()) ?: return
        val artifact = Path.of(artifactText).toAbsolutePath().normalize()
        if (!Files.isRegularFile(artifact) || !artifact.fileName.toString().endsWith(".3dsx")) {
            report("Choose an existing .3dsx file.", true)
            return
        }
        background(event.project, "Launching Nintendo 3DS emulator") {
            val executable = Dev3DSToolchain.emulator()?.takeIf(Files::isRegularFile)
                ?: AzaharInstaller().install().also(Dev3DSToolchain::saveEmulator)
            ProcessBuilder(executable.toString(), artifact.toString()).directory(root.toFile()).start()
            "Launched ${artifact.fileName} in ${executable.fileName}."
        }
    }
}
