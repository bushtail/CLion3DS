package ui

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.content.ContentFactory
import com.intellij.util.ui.JBUI

import project.Dev3DSProjectSetup

import java.awt.BorderLayout
import java.awt.Font
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.nio.file.Path

import javax.swing.JPanel

class Dev3DSToolWindowFactory : ToolWindowFactory, DumbAware {
    override suspend fun isApplicableAsync(project: Project): Boolean {
        return project.basePath?.let {
            Dev3DSProjectSetup.isDev3DSProject(Path.of(it))
        } == true
    }

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = JPanel(BorderLayout())
        val sections = JPanel().apply {
            layout = GridBagLayout()
            border = JBUI.Borders.empty(12)
        }
        val manager = ActionManager.getInstance()

        addSection(
            sections,
            panel,
            manager,
            "Build",
            listOf(
                "Build",
                "BuildCia"
            )
        )

        addSection(
            sections,
            panel,
            manager,
            "Run",
            listOf(
                "SendCia",
                "Deploy",
                "Emulator"
            )
        )

        addSection(
            sections,
            panel,
            manager,
            "Tools",
            listOf(
                "InstallTools",
                "RepairProject",
                "Toolchain",
                "UseEmulator",
                "DownloadEmulatorFirmware",
                "ImportSystemFiles",
                "BackUpSystemFiles"
            )
        )

        sections.add(
            JPanel().apply {
                isOpaque = false
            },
            GridBagConstraints().apply {
                gridx = 0
                gridy = sections.componentCount
                weightx = 1.0
                weighty = 1.0
                fill = GridBagConstraints.BOTH
            }
        )

        panel.add(JBScrollPane(sections), BorderLayout.CENTER)
        toolWindow.contentManager.addContent(ContentFactory.getInstance().createContent(panel, null, false))
    }

    private fun addSection(
        container: JPanel,
        target: JPanel,
        manager: ActionManager,
        heading: String,
        names: List<String>
    ) {
        val label = JBLabel(heading).apply {
            font = font.deriveFont(Font.BOLD)
            border = JBUI.Borders.emptyTop(if (container.componentCount == 0) {
                0
            } else {
                12
            })
        }

        container.add(label, rowConstraints(container.componentCount))

        val actions = DefaultActionGroup()
        for (name in names) {
            val id = "ca.bushtail.CLion3DS.$name"
            val action = checkNotNull(manager.getAction(id)) {
                "3DS action is not registered: $id"
            }

            action.templatePresentation.putClientProperty(ActionUtil.SHOW_TEXT_IN_TOOLBAR, true)
            actions.add(action)
        }

        val toolbar = manager.createActionToolbar("Nintendo3DSToolWindow", actions, false)
        toolbar.targetComponent = target
        container.add(toolbar.component, rowConstraints(container.componentCount))
    }

    private fun rowConstraints(row: Int): GridBagConstraints {
        return GridBagConstraints().apply {
            gridx = 0
            gridy = row
            weightx = 1.0
            anchor = GridBagConstraints.NORTHWEST
            fill = GridBagConstraints.HORIZONTAL
        }
    }
}
