package project

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.startup.ProjectActivity
import java.nio.file.Path

class Dev3DSProjectStartup : ProjectActivity, DumbAware {
    override suspend fun execute(project: Project) {
        project.basePath?.let {
            Dev3DSProjectSetup.start(project, Path.of(it))
        }
    }
}
