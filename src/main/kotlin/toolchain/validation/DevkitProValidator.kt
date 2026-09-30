package toolchain.validation

import com.intellij.util.containers.addIfNotNull
import toolchain.environment.EHostPlatform
import toolchain.installation.DevkitProInstallation
import java.nio.file.Path
import java.nio.file.Files

class DevkitProValidator {
    fun validate(installation: DevkitProInstallation): ToolchainValidationResult {
        val layout = installation.layout
        val host = installation.host

        val issues = buildList {
            addIfNotNull(
                ToolchainPathValidator.validateDirectory(
                    path = layout.root,
                    component = EToolchainComponent.DevkitPro,
                    displayName = "devkitPro root"
                )
            )

            addIfNotNull(
                ToolchainPathValidator.validateDirectory(
                    path = layout.devkitArm,
                    component = EToolchainComponent.DevkitArm,
                    displayName = "devkitARM"
                )
            )

            addIfNotNull(
                ToolchainPathValidator.validateDirectory(
                    path = layout.libctru,
                    component = EToolchainComponent.Libctru,
                    displayName = "libctru"
                )
            )

            addIfNotNull(
                ToolchainPathValidator.validateDirectory(
                    path = layout.libctruInclude,
                    component = EToolchainComponent.LibctruHeaders,
                    displayName = "libctru include directory"
                )
            )

            addIfNotNull(
                ToolchainPathValidator.validateDirectory(
                    path = layout.libctruLib,
                    component = EToolchainComponent.LibctruLibraries,
                    displayName = "libctru library directory"
                )
            )

            if (!Files.isRegularFile(layout.dev3DSCMakeToolchain)) {
                add(
                    ToolchainValidationIssue(
                        EToolchainComponent.CMakeToolchain,
                        EToolchainValidationError.Missing,
                        layout.dev3DSCMakeToolchain,
                        "3DS CMake toolchain was not found at ${layout.dev3DSCMakeToolchain}"
                    )
                )
            }

            addIfNotNull(
                executable(
                    layout.gcc,
                    host,
                    EToolchainComponent.Compiler,
                    "ARM C compiler"
                )
            )

            addIfNotNull(
                executable(
                    layout.gxx,
                    host,
                    EToolchainComponent.CppCompiler,
                    "ARM C++ compiler"
                )
            )

            addIfNotNull(
                executable(
                    layout.gdb,
                    host,
                    EToolchainComponent.Debugger,
                    "ARM debugger"
                )
            )

            addIfNotNull(
                executable(
                    layout.dev3DSLink,
                    host,
                    EToolchainComponent.Dev3DSLink,
                    "3dslink"
                )
            )

            addIfNotNull(
                executable(
                    layout.dev3DSXTool,
                    host,
                    EToolchainComponent.Dev3DSXTool,
                    "3dsxtool"
                )
            )

            addIfNotNull(
                executable(
                    layout.smdhTool,
                    host,
                    EToolchainComponent.SmdhTool,
                    "smdhtool"
                )
            )

            if (host == EHostPlatform.Windows) {
                addIfNotNull(
                    executable(
                        layout.msys2UsrBin.resolve("cmake.exe"),
                        host,
                        EToolchainComponent.CMakeToolchain,
                        "MSYS2 CMake"
                    )
                )

                addIfNotNull(
                    executable(
                        layout.msys2UsrBin.resolve("ninja.exe"),
                        host,
                        EToolchainComponent.Make,
                        "MSYS2 Ninja"
                    )
                )

                addIfNotNull(
                    executable(
                        layout.windowsPacman,
                        host,
                        EToolchainComponent.Pacman,
                        "pacman"
                    )
                )

                addIfNotNull(
                    executable(
                        layout.windowsMake,
                        host,
                        EToolchainComponent.Make,
                        "make"
                    )
                )
            }
        }

        return ToolchainValidationResult(
            installation = installation,
            issues = issues
        )
    }

    private fun executable(
        path: Path,
        host: EHostPlatform,
        component: EToolchainComponent,
        displayName: String
    ): ToolchainValidationIssue? {
        return ToolchainPathValidator.validateExecutable(
            path = path,
            host = host,
            component = component,
            displayName = displayName
        )
    }
}
