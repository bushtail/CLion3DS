package actions

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project

internal object BackgroundActionRunner {
    fun background(project: Project?, title: String, operation: () -> String) {
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, title, false) {
            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = true
                try {
                    ActionDialogs.report(operation())
                } catch (exception: Exception) {
                    ActionDialogs.report(exception.message ?: exception.javaClass.simpleName, true)
                }
            }
        })
    }
}
