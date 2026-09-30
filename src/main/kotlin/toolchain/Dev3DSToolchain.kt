package toolchain

import com.intellij.ide.util.PropertiesComponent
import toolchain.detection.DevkitProDetector
import toolchain.environment.HostPlatformDetector
import toolchain.installation.DevkitProInstallation
import toolchain.installation.EDevkitProInstallationKind
import java.nio.file.Path

object Dev3DSToolchain {
    private const val ROOT_KEY = "ca.bushtail.clion3ds.devkitpro.root"
    private const val EMULATOR_KEY = "ca.bushtail.clion3ds.emulator"

    fun configuredRoot(): Path? {
        return PropertiesComponent.getInstance().getValue(ROOT_KEY)?.takeIf(String::isNotBlank)?.let(Path::of)
    }

    fun saveRoot(root: Path) {
        PropertiesComponent.getInstance().setValue(ROOT_KEY, root.toAbsolutePath().normalize().toString())
    }

    fun emulator(): Path? {
        return PropertiesComponent.getInstance().getValue(EMULATOR_KEY)
            ?.takeIf(String::isNotBlank)?.let(Path::of)
            ?.takeUnless { path ->
                val parts = path.toAbsolutePath().normalize().iterator().asSequence().map { it.toString() }.toList()
                parts.zipWithNext().any { (first, second) ->
                    first.equals("clion3ds", true) && second.equals("azahar", true)
                }
            }
    }

    fun saveEmulator(path: Path) {
        PropertiesComponent.getInstance().setValue(EMULATOR_KEY, path.toAbsolutePath().normalize().toString())
    }

    fun installation(): DevkitProInstallation? {
        val root = configuredRoot() ?: DevkitProDetector().detectRootEvenIfIncomplete()?.root ?: return null
        return DevkitProInstallation(
            root.toAbsolutePath().normalize(),
            HostPlatformDetector.detect(),
            EDevkitProInstallationKind.Existing
        )
    }
}
