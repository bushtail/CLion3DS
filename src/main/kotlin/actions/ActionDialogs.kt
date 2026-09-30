package actions

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.ui.Messages

internal object ActionDialogs {
    const val TITLE = "Nintendo 3DS"

    fun input(message: String, initial: String = ""): String? {
        return Messages.showInputDialog(message, TITLE, Messages.getQuestionIcon(), initial, null)
            ?.trim()
            ?.takeIf(String::isNotEmpty)
    }

    fun report(message: String, error: Boolean = false) {
        ApplicationManager.getApplication().invokeLater {
            if (error) {
                Messages.showErrorDialog(message, TITLE)
            } else {
                Messages.showInfoMessage(message, TITLE)
            }
        }
    }
}
