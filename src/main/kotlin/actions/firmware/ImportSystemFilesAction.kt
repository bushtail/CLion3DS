package actions.firmware

import actions.ActionDialogs.input
import actions.BackgroundActionRunner.background
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import run.AzaharSystemFiles
import java.nio.file.Path

class ImportSystemFilesAction : AnAction() {
    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    override fun actionPerformed(event: AnActionEvent) {
        val source = input("Emulator data folder or ZIP containing nand and/or sysdata:") ?: return
        background(event.project, "Importing emulator firmware") {
            val backup = AzaharSystemFiles.import(Path.of(source))
            "Emulator firmware imported.${backup?.let { " Previous files backed up to $it." }.orEmpty()}"
        }
    }
}
