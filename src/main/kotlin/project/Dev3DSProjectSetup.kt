package project

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.diagnostic.Logger
import com.jetbrains.cidr.cpp.cmake.workspace.CMakeWorkspace
import com.jetbrains.cidr.cpp.cmake.CMakeSettings
import build.CiaTools
import com.intellij.platform.eel.fs.EelFiles
import toolchain.ToolProvisioner
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import javax.swing.JOptionPane

object Dev3DSProjectSetup {
    private val logger = Logger.getInstance(Dev3DSProjectSetup::class.java)
    private val pending = ConcurrentHashMap.newKeySet<Project>()

    fun isDev3DSProject(directory: Path): Boolean {
        val cmake = directory.resolve("CMakeLists.txt")
        return Files.isRegularFile(cmake) && runCatching {
            EelFiles.readString(cmake).contains("3DS.cmake")
        }.getOrDefault(false)
    }

    fun start(project: Project, directory: Path) {
        if (!isDev3DSProject(directory) || !pending.add(project)) {
            return
        }
        logger.info("Setting up Nintendo 3DS project at $directory")
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Setting up Nintendo 3DS project", false) {
            override fun run(indicator: ProgressIndicator) {
                indicator.text = "Installing Nintendo 3DS development tools"
                indicator.isIndeterminate = true
                try {
                    ToolProvisioner.ensureToolchain()
                    logger.info("Nintendo 3DS toolchain is ready")
                    runCatching { CiaTools.ensureInstalled() }.onFailure { logger.warn("CIA tool download failed", it) }
                    // postStartupActivity can run before CLion has loaded its CMake workspace.
                    // linkCMakeProjectAsync schedules a reload immediately and throws in its
                    // coroutine if the watcher has not been initialized yet.
                    val workspace = CMakeWorkspace.getInstance(project)
                    logger.info("CMake profiles: ${CMakeSettings.getInstance(project).activeProfiles.size}; linked directory: ${workspace.modelProjectDir}")
                    var attempts = 0
                    while (!project.isDisposed && !workspace.isWatcherInitialized && attempts++ < 120) {
                        Thread.sleep(500)
                    }
                    if (!project.isDisposed && !workspace.isWatcherInitialized) {
                        throw IllegalStateException("CLion's CMake workspace did not initialize")
                    }
                    ApplicationManager.getApplication().invokeLater {
                        try {
                            logger.info("Attaching Nintendo 3DS CMake project at $directory")
                            if (!project.isDisposed) {
                                directory.toFile()
                            }
                        } catch (exception: Exception) {
                            logger.warn("Unable to attach 3DS CMake project", exception)
                            if (!project.isDisposed) {
                                showError(project, exception)
                            }
                        } finally {
                            pending.remove(project)
                        }
                    }
                } catch (exception: Exception) {
                    pending.remove(project)
                    ApplicationManager.getApplication().invokeLater {
                        if (!project.isDisposed) {
                            showError(project, exception)
                        }
                    }
                }
            }
        })
    }

    private fun showError(project: Project, exception: Exception) {
        logger.warn("Nintendo 3DS setup failed in ${project.name}", exception)
        JOptionPane.showMessageDialog(null,
            "Nintendo 3DS setup failed: ${exception.message}\nUse Tools > Nintendo 3DS > Set Up Current 3DS Project to retry.",
            "Nintendo 3DS", JOptionPane.ERROR_MESSAGE)
    }
}

