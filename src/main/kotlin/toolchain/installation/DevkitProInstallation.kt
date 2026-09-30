package toolchain.installation

import toolchain.environment.DevkitProEnvironment
import toolchain.environment.EHostPlatform
import toolchain.packages.DevkitProLayout
import java.nio.file.Path

data class DevkitProInstallation(val root: Path, val host: EHostPlatform, val kind: EDevkitProInstallationKind) {
    val layout: DevkitProLayout
        get() {
            return DevkitProLayout(root, host)
        }

    val environment: DevkitProEnvironment
        get() {
            return layout.environment()
        }
}