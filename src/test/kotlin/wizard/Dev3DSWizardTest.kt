package wizard

import com.intellij.openapi.ui.TextFieldWithBrowseButton
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import javax.swing.JComboBox
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextField

class Dev3DSWizardTest {
    @Test fun exposesGraphicsSettingsInsteadOfCppStandard() {
        val panel = Dev3DSProjectWizard().settingsPanel as JPanel
        assertTrue(panel.components.filterIsInstance<JLabel>().any {
            it.text == "Graphics support:"
        })

        assertTrue(panel.components.filterIsInstance<JLabel>().any {
            it.text.startsWith("Project name")
        })

        assertTrue(panel.components.filterIsInstance<JLabel>().any {
            it.text == "Home Menu title:"
        })

        assertTrue(panel.components.filterIsInstance<JLabel>().any {
            it.text == "Description:"
        })

        assertTrue(panel.components.filterIsInstance<JLabel>().any {
            it.text == "Publisher:"
        })

        assertEquals(3, panel.components.filterIsInstance<JComboBox<*>>().single().itemCount)

        assertTrue(panel.components.filterIsInstance<JLabel>().none {
            it.text == "Language standard:"
        })
    }

    @Test fun projectNameControlsNewProjectFolder() {
        val wizard = Dev3DSProjectWizard()
        val location = TextFieldWithBrowseButton()
        location.text = "C:\\Users\\developer\\CLionProjects\\untitled"
        wizard.createPeer().getComponent(location) { }

        val name = (wizard.settingsPanel as JPanel).components.filterIsInstance<JTextField>().first()
        assertEquals("untitled", name.text)
        name.text = "My3DSGame"
        assertEquals("C:\\Users\\developer\\CLionProjects\\My3DSGame", location.text)
    }
}
