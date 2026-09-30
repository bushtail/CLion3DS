package actions.firmware

import actions.BackgroundActionRunner.background
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import toolchain.Dev3DSToolchain
import toolchain.bootstrap.AzaharInstaller
import java.nio.file.Files

class DownloadEmulatorFirmwareAction : AnAction() {
    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    override fun actionPerformed(event: AnActionEvent) {
        background(event.project, "Opening emulator firmware setup") {
            val executable = Dev3DSToolchain.emulator()?.takeIf(Files::isRegularFile)
                ?: AzaharInstaller().install().also(Dev3DSToolchain::saveEmulator)
            ProcessBuilder(executable.toString()).start()
            "In the emulator, choose File > Download System Files, then select a region."
        }
    }
}
