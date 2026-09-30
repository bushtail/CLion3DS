package actions.firmware

import actions.BackgroundActionRunner.background
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import run.AzaharSystemFiles

class BackUpSystemFilesAction : AnAction() {
    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    override fun actionPerformed(event: AnActionEvent) {
        background(event.project, "Backing up emulator firmware") {
            "Emulator firmware backed up to ${AzaharSystemFiles.backupCurrent()}."
        }
    }
}
