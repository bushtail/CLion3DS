package actions.dev3ds

import actions.ActionDialogs.input
import actions.ActionDialogs.report
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import toolchain.Dev3DSToolchain
import java.nio.file.Files
import java.nio.file.Path

class UseEmulatorAction : AnAction() {
    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    override fun actionPerformed(event: AnActionEvent) {
        val value = input("Emulator executable:", Dev3DSToolchain.emulator()?.toString().orEmpty()) ?: return
        val executable = Path.of(value).toAbsolutePath().normalize()
        if (!Files.isRegularFile(executable)) {
            report("Emulator executable was not found: $executable", true)
            return
        }
        Dev3DSToolchain.saveEmulator(executable)
        report("Emulator set to $executable")
    }
}
