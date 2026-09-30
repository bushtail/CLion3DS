package toolchain

import toolchain.bootstrap.DevkitProBootstrapper
import toolchain.installation.DevkitProInstallRequest
import toolchain.installation.DevkitProInstallation
import toolchain.validation.DevkitProValidator

object ToolProvisioner {
    @Synchronized
    fun ensureToolchain(): DevkitProInstallation {
        val current = Dev3DSToolchain.installation()
        if (current != null && DevkitProValidator().validate(current).isValid) {
            return current
        }

        val result = DevkitProBootstrapper().install(
            DevkitProInstallRequest(destination = current?.root ?: Dev3DSToolchain.configuredRoot())
        )

        if (!result.success) {
            error(result.message)
        }

        val installed = result.installation ?: error("devkitPro setup completed without an installation path")
        Dev3DSToolchain.saveRoot(installed.root)

        return installed
    }
}
