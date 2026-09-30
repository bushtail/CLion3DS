package actions.dev3ds

import actions.BackgroundActionRunner.background
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import toolchain.Dev3DSToolchain
import toolchain.ToolProvisioner
import toolchain.bootstrap.AzaharInstaller
import java.nio.file.Files

class InstallToolsAction : AnAction() {
    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    override fun actionPerformed(event: AnActionEvent) {
        background(event.project, "Installing Nintendo 3DS development tools") {
            val installation = ToolProvisioner.ensureToolchain()
            val emulator = if (System.getProperty("os.name").lowercase().contains("win")) {
                Dev3DSToolchain.emulator()?.takeIf(Files::isRegularFile)
                    ?: AzaharInstaller().install().also(Dev3DSToolchain::saveEmulator)
            } else {
                null
            }
            "devkitPro is ready at ${installation.root}.${emulator?.let { "\nAzahar is ready at $it." }.orEmpty()}\nCMake is provided by CLion."
        }
    }
}
