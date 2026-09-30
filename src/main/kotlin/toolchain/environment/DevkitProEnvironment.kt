package toolchain.environment

import toolchain.packages.DevkitProConstants
import java.io.File
import java.nio.file.Path

data class DevkitProEnvironment(val devkitPro: Path, val devkitArm: Path, val pathEntries: List<Path>) {
    fun variables(inheritedEnvironment: Map<String, String> = System.getenv()): Map<String, String> {
        val variables = inheritedEnvironment.toMutableMap()

        variables[DevkitProConstants.ENV_DEVKIT_PRO] =
            devkitPro.toString()

        variables[DevkitProConstants.ENV_DEVKIT_ARM] =
            devkitArm.toString()

        val inheritedPath = inheritedEnvironment["PATH"].orEmpty()

        val path = buildList {
            addAll(pathEntries.map(Path::toString))

            if (inheritedPath.isNotBlank()) {
                add(inheritedPath)
            }
        }.joinToString(File.pathSeparator)

        variables["PATH"] = path

        return variables
    }
}
