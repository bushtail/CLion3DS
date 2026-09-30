package toolchain.packages

import toolchain.environment.DevkitProEnvironment
import toolchain.environment.EHostPlatform
import java.nio.file.Path

class DevkitProLayout(val root: Path, val host: EHostPlatform) {
    val devkitArm: Path
        get() {
            return root.resolve(DevkitProConstants.DIR_DEVKIT_ARM)
        }

    val devkitArmBin: Path
        get() {
            return devkitArm.resolve(DevkitProConstants.DIR_BIN)
        }

    val libctru: Path
        get() {
            return root.resolve(DevkitProConstants.DIR_LIBCTRU)
        }

    val libctruInclude: Path
        get() {
            return libctru.resolve(DevkitProConstants.DIR_INCLUDE)
        }

    val libctruLib: Path
        get() {
            return libctru.resolve(DevkitProConstants.DIR_LIB)
        }

    val tools: Path
        get() {
            return root.resolve(DevkitProConstants.DIR_TOOLS)
        }

    val dev3DSCMakeToolchain: Path
        get() {
            return root.resolve("cmake").resolve("3DS.cmake")
        }

    val toolsBin: Path
        get() {
            return tools.resolve(DevkitProConstants.DIR_BIN)
        }

    val msys2: Path
        get() {
            return root.resolve(DevkitProConstants.DIR_MSYS2)
        }

    val msys2UsrBin: Path
        get() {
            return msys2.resolve(DevkitProConstants.DIR_MSYS2_USR).resolve(DevkitProConstants.DIR_BIN)
        }

    val gcc: Path
        get() {
            return devkitArmBin.resolve(executable(DevkitProExecutableNames.ARM_GCC))
        }

    val gxx: Path
        get() {
            return devkitArmBin.resolve(executable(DevkitProExecutableNames.ARM_GXX))
        }

    val gdb: Path
        get() {
            return devkitArmBin.resolve(executable(DevkitProExecutableNames.ARM_GDB))
        }

    val dev3DSLink: Path
        get() {
            return toolsBin.resolve(executable(DevkitProExecutableNames.DEV_3DSLINK))
        }

    val dev3DSXTool: Path
        get() {
            return toolsBin.resolve(executable(DevkitProExecutableNames.DEV_3DSXTOOL))
        }

    val smdhTool: Path
        get() {
            return toolsBin.resolve(executable(DevkitProExecutableNames.SMDH_TOOL))
        }

    val windowsPacman: Path
        get() {
            return msys2UsrBin.resolve("pacman.exe")
        }

    val windowsMake: Path
        get() {
            return msys2UsrBin.resolve("make.exe")
        }

    fun executable(name: String): String {
        return when (host) {
            EHostPlatform.Windows -> "$name.exe"
            EHostPlatform.Linux,
            EHostPlatform.MacOS -> name
        }
    }

    fun environment(): DevkitProEnvironment {
        val pathEntries = buildList {
            add(devkitArmBin)
            add(toolsBin)

            if (host == EHostPlatform.Windows) {
                add(msys2UsrBin)
            }
        }

        return DevkitProEnvironment(
            devkitPro = root,
            devkitArm = devkitArm,
            pathEntries = pathEntries
        )
    }
}
