package actions.dev3ds

import actions.ActionDialogs.input
import actions.ActionDialogs.report
import actions.BackgroundActionRunner.background
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import toolchain.Dev3DSToolchain
import toolchain.ToolProvisioner
import toolchain.validation.DevkitProValidator
import java.nio.file.Path

class ToolchainAction : AnAction() {
    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    override fun actionPerformed(event: AnActionEvent) {
        val current = Dev3DSToolchain.installation()?.root?.toString().orEmpty()
        val value = input("devkitPro installation directory:", current) ?: return
        val root = try { Path.of(value).toAbsolutePath().normalize() } catch (e: Exception) {
            report("Invalid path: ${e.message}", true)
            return
        }
        Dev3DSToolchain.saveRoot(root)
        val installation = Dev3DSToolchain.installation() ?: return
        val result = DevkitProValidator().validate(installation)
        if (result.isValid) {
            report("devkitPro is ready at $root")
            return
        }
        background(event.project, "Installing Nintendo 3DS toolchain") {
            val installed = ToolProvisioner.ensureToolchain()
            "devkitPro is ready at ${installed.root}."
        }
    }
}
