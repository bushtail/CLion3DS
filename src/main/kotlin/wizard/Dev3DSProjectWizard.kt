package wizard

import project.Dev3DSProjectGenerator
import project.Dev3DSProjectSetup
import toolchain.Dev3DSToolchain
import com.intellij.ide.setToolTipText
import com.intellij.icons.AllIcons
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.util.text.HtmlChunk
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.GeneratorPeerImpl
import com.intellij.platform.ProjectGeneratorPeer
import com.intellij.ui.DocumentAdapter
import com.intellij.util.ui.JBUI
import com.jetbrains.cidr.cpp.cmake.projectWizard.generators.CMakeProjectGenerator
import com.jetbrains.cidr.cpp.cmake.projectWizard.generators.settings.CMakeProjectSettings
import model.EGraphicsLibrary
import model.ProjectOptions
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.io.IOException
import java.nio.file.Path
import javax.swing.Icon
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextField
import javax.swing.event.DocumentEvent

class Dev3DSProjectWizard : CMakeProjectGenerator() {
    private val projectName = JTextField(26).apply {
        setToolTipText(HtmlChunk.text("Use letters, digits, and underscores; start with a letter"))
    }

    private val appTitle = JTextField(26).apply {
        setToolTipText(HtmlChunk.text("Name shown on the 3DS Home Menu"))
    }

    private val description = JTextField(26).apply {
        setToolTipText(HtmlChunk.text("Home Menu description, up to 128 characters"))
    }

    private val publisher = JTextField("Homebrew", 26)

    private val productCode = JTextField(10).apply {
        setToolTipText(HtmlChunk.text("Optional four letter or digit code; blank generates one"))
    }

    private val uniqueId = JTextField(10).apply {
        setToolTipText(HtmlChunk.text("Optional five digit hexadecimal CIA ID; blank generates one"))
    }

    private val graphicsSelector = ComboBox(EGraphicsLibrary.entries.toTypedArray())

    private val panel = JPanel(GridBagLayout()).apply {
        val constraints = GridBagConstraints().apply {
            anchor = GridBagConstraints.WEST
            insets = JBUI.insets(3, 0, 3, 12)
        }

        val fields = listOf(
            "Project name:" to projectName,
            "Home Menu title:" to appTitle,
            "Description:" to description,
            "Publisher:" to publisher,
            "Product code (optional):" to productCode,
            "Unique ID (optional):" to uniqueId,
            "Graphics support:" to graphicsSelector
        )

        fields.forEachIndexed { row, (label, field) ->
            constraints.gridy = row
            constraints.gridx = 0
            constraints.weightx = 0.0
            constraints.fill = GridBagConstraints.NONE
            add(JLabel(label), constraints)
            constraints.gridx = 1
            constraints.weightx = 1.0
            constraints.fill = GridBagConstraints.HORIZONTAL
            add(field, constraints)
        }

        constraints.gridy = fields.size
        constraints.gridx = 0
        constraints.gridwidth = 2
        constraints.weighty = 1.0
        add(JPanel(), constraints)
    }

    override fun getName(): String {
        return "Nintendo 3DS Application"
    }

    override fun getLogo(): Icon {
        return AllIcons.Nodes.Module
    }

    override fun getGroupName(): String {
        return "Nintendo 3DS"
    }

    override fun getGroupDisplayName(): String {
        return "Nintendo 3DS"
    }

    override fun getGroupOrder(): Int {
        return 20
    }

    override fun getSettingsPanel(): JComponent {
        return panel
    }

    override fun createPeer(): ProjectGeneratorPeer<CMakeProjectSettings> {
        val peer = GeneratorPeerImpl(createProjectSettings(), panel)
        return object : ProjectGeneratorPeer<CMakeProjectSettings> by peer {
            override fun getComponent(locationField: TextFieldWithBrowseButton, checkValid: Runnable): JComponent {
                val component = peer.getComponent(locationField, checkValid)
                val initialName = runCatching {
                    Path.of(locationField.text).fileName?.toString()
                }.getOrNull()

                if (projectName.text.isBlank() && !initialName.isNullOrBlank()) {
                    projectName.text = initialName
                }

                projectName.document.addDocumentListener(object : DocumentAdapter() {
                    override fun textChanged(e: DocumentEvent) {
                        val newName = projectName.text.trim()
                        if (Regex("[A-Za-z][A-Za-z0-9_]*").matches(newName)) {
                            val currentPath = runCatching { Path.of(locationField.text) }.getOrNull()
                            val parent = currentPath?.parent
                            if (parent != null) {
                                locationField.text = parent.resolve(newName).toString()
                            }
                        }
                        checkValid.run()
                    }
                })
                return component
            }
        }
    }

    override fun createProjectSettings(): CMakeProjectSettings {
        return object : CMakeProjectSettings() {
            override fun getEnabledProjectLanguages(): String {
                return "C"
            }

            override fun getLanguageVersionLineForCMake(): String {
                return ""
            }
        }
    }

    override fun createCMakeFile(project: Project, name: String, directory: VirtualFile): VirtualFile {
        val path = Path.of(directory.path)
        val targetName = projectName.text.trim()
        val options = ProjectOptions(
            graphicsLibrary = graphicsSelector.selectedItem as EGraphicsLibrary,
            appTitle = appTitle.text.trim(),
            description = description.text.trim(),
            publisher = publisher.text.trim(),
            productCode = productCode.text.trim(),
            uniqueId = uniqueId.text.trim()
        )

        try {
            Dev3DSProjectGenerator.createFromWizard(path, targetName, options, Dev3DSToolchain.installation()?.root)
        } catch (exception: IllegalArgumentException) {
            throw IOException(exception.message, exception)
        }

        directory.refresh(false, true)

        return directory.findChild("CMakeLists.txt") ?: error("3DS CMake project was not created")
    }

    override fun createSourceFiles(project: Project, name: String, directory: VirtualFile): Array<VirtualFile> {
        return listOfNotNull(directory.findFileByRelativePath("source/main.c")).toTypedArray()
    }

    override fun generateProject(project: Project, baseDir: VirtualFile, settings: CMakeProjectSettings, module: Module) {
        super.generateProject(project, baseDir, settings, module)
        Dev3DSProjectSetup.start(project, Path.of(baseDir.path))
    }
}
