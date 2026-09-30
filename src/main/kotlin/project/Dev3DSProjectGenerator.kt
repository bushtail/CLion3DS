package project

import com.intellij.ui.JBColor
import com.intellij.util.ui.ImageUtil
import model.EGraphicsLibrary
import model.ProjectOptions
import java.nio.file.Files
import java.nio.file.Path
import java.awt.Font
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.imageio.ImageIO

/** Generates a CLion CMake project without overwriting existing user files. */
object Dev3DSProjectGenerator {
    private val validName = Regex("[A-Za-z][A-Za-z0-9_]*")

    fun create(directory: Path, name: String, options: ProjectOptions = ProjectOptions(), devkitProRoot: Path? = null) {
        require(validName.matches(name)) { "Use letters, digits, and underscores; start with a letter." }
        require(!Files.exists(directory) || Files.list(directory).use { it.findAny().isEmpty }) {
            "The project directory must be empty: $directory"
        }
        writeFiles(directory, name, options, devkitProRoot)
    }

    /** The New Project wizard may have already created .idea or .git in the directory. */
    fun createFromWizard(directory: Path, name: String, options: ProjectOptions = ProjectOptions(), devkitProRoot: Path? = null) {
        require(validName.matches(name)) { "Use letters, digits, and underscores; start with a letter." }
        val generated = listOf("CMakeLists.txt", "cmake/3ds-windows.cmake", "source/main.c", "assets/app.rsf",
            "assets/icon.png", "assets/banner.png", "assets/silence.wav", "assets/title.txt",
            "assets/description.txt", "assets/publisher.txt", ".gitignore", "README.md")
        require(generated.none { Files.exists(directory.resolve(it)) }) {
            "The selected directory already contains a 3DS project file: $directory"
        }
        writeFiles(directory, name, options, devkitProRoot)
    }

    fun cmakeTargetName(projectName: String): String {
        val sanitized = projectName.replace(Regex("[^A-Za-z0-9_]"), "_")
        return if (sanitized.firstOrNull()?.isLetter() == true) {
            sanitized
        } else {
            "Project_$sanitized"
        }
    }

    private fun writeFiles(directory: Path, name: String, options: ProjectOptions, devkitProRoot: Path?) {
        require(options.appTitle.length <= 64 && '\n' !in options.appTitle && '\r' !in options.appTitle) {
            "Home Menu title must be one line and at most 64 characters."
        }
        require(options.description.length <= 128 && '\n' !in options.description && '\r' !in options.description) {
            "Description must be one line and at most 128 characters."
        }
        require(options.publisher.length <= 64 && '\n' !in options.publisher && '\r' !in options.publisher) {
            "Publisher must be one line and at most 64 characters."
        }
        require(options.productCode.isBlank() || Regex("[A-Za-z0-9]{4}").matches(options.productCode)) {
            "Product code must be four letters or digits."
        }
        require(options.uniqueId.isBlank() || Regex("(?:0[xX])?[0-9A-Fa-f]{5}").matches(options.uniqueId)) {
            "Unique ID must be five hexadecimal digits."
        }
        Files.createDirectories(directory.resolve("source"))
        Files.createDirectories(directory.resolve("cmake"))
        Files.createDirectories(directory.resolve("assets"))
        Files.writeString(directory.resolve("CMakeLists.txt"), cmake(name, options, devkitProRoot))
        Files.writeString(directory.resolve("cmake/3ds-windows.cmake"), windowsToolchain())
        Files.writeString(directory.resolve("source/main.c"), mainSource(name))
        Files.writeString(directory.resolve("assets/app.rsf"), ciaMetadata(name, options))
        val title = options.appTitle.ifBlank { name }
        Files.writeString(directory.resolve("assets/title.txt"), "$title\n")
        Files.writeString(directory.resolve("assets/description.txt"),
            "${options.description.ifBlank { "$title for Nintendo 3DS" }}\n")
        Files.writeString(directory.resolve("assets/publisher.txt"), "${options.publisher.ifBlank { "Homebrew" }}\n")
        writeImage(directory.resolve("assets/icon.png"), 48, 48, title)
        writeImage(directory.resolve("assets/banner.png"), 256, 128, title)
        writeSilence(directory.resolve("assets/silence.wav"))
        Files.writeString(directory.resolve(".gitignore"), "cmake-build-*/\nbuild/\ndist/\n.idea/\n")
        Files.writeString(directory.resolve("README.md"), readme(name))
    }

    internal fun cmake(name: String, options: ProjectOptions = ProjectOptions(), devkitProRoot: Path? = null): String {
        val graphicsLibraries = when (options.graphicsLibrary) {
            EGraphicsLibrary.None -> ""
            EGraphicsLibrary.Citro2D -> "citro2d citro3d"
            EGraphicsLibrary.Citro3D -> "citro3d"
        }
        val preferredRoot = devkitProRoot?.toAbsolutePath()?.normalize()?.toString()
            ?.replace('\\', '/')?.replace("\"", "\\\"")?.replace("$", "\\$")
        val cmakeRoot = preferredRoot ?: $$"${DEVKITPRO_ROOT}"
        return $$"""
        cmake_minimum_required(VERSION 3.20)
        set(DEVKITPRO_ROOT "$ENV{DEVKITPRO}")
        set(DEVKITPRO_ROOT "$$cmakeRoot")
        if(CMAKE_HOST_WIN32 AND NOT EXISTS "${DEVKITPRO_ROOT}/cmake/3DS.cmake")
            set(DEVKITPRO_ROOT "C:/devkitPro")
        elseif(DEVKITPRO_ROOT STREQUAL "")
            set(DEVKITPRO_ROOT "/opt/devkitpro")
        endif()
        if(NOT EXISTS "${DEVKITPRO_ROOT}/cmake/3DS.cmake")
            message(FATAL_ERROR "3DS tools missing at ${DEVKITPRO_ROOT}. Use Tools > Nintendo 3DS > Install or Repair Tools in CLion, then reload CMake.")
        endif()
        set(ENV{DEVKITPRO} "${DEVKITPRO_ROOT}")
        set(ENV{DEVKITARM} "${DEVKITPRO_ROOT}/devkitARM")
        if(CMAKE_HOST_WIN32)
            set(CMAKE_TOOLCHAIN_FILE "${CMAKE_CURRENT_LIST_DIR}/cmake/3ds-windows.cmake" CACHE FILEPATH "3DS toolchain")
        else()
            set(CMAKE_TOOLCHAIN_FILE "${DEVKITPRO_ROOT}/cmake/3DS.cmake" CACHE FILEPATH "3DS toolchain")
        endif()
        project($$name C)

        add_executable($$name source/main.c)
        set_target_properties($$name PROPERTIES SUFFIX ".elf" C_STANDARD 11 C_STANDARD_REQUIRED ON C_EXTENSIONS ON)
        target_include_directories($$name PRIVATE "${DEVKITPRO_ROOT}/libctru/include")
        target_include_directories($$name PRIVATE "${DEVKITPRO_ROOT}/portlibs/3ds/include")
        target_link_directories($$name PRIVATE "${DEVKITPRO_ROOT}/libctru/lib")
        target_link_directories($$name PRIVATE "${DEVKITPRO_ROOT}/portlibs/3ds/lib")
        target_link_libraries($$name PRIVATE $$graphicsLibraries ctru m)

        find_program(DEV3DSXTOOL 3dsxtool HINTS "${DEVKITPRO_ROOT}/tools/bin" REQUIRED)
        add_custom_command(
            OUTPUT "${CMAKE_CURRENT_BINARY_DIR}/$$name.3dsx"
            COMMAND "${DEV3DSXTOOL}" "$<TARGET_FILE:$$name>" "${CMAKE_CURRENT_BINARY_DIR}/$$name.3dsx"
            DEPENDS $$name
            VERBATIM
        )
        add_custom_target($${name}_3dsx ALL DEPENDS "${CMAKE_CURRENT_BINARY_DIR}/$$name.3dsx")
        set_property(DIRECTORY APPEND PROPERTY ADDITIONAL_CLEAN_FILES
            "${CMAKE_CURRENT_BINARY_DIR}/$$name.cia"
            "${CMAKE_CURRENT_BINARY_DIR}/icon.icn"
            "${CMAKE_CURRENT_BINARY_DIR}/banner.bnr"
            "${CMAKE_CURRENT_SOURCE_DIR}/dist/$$name.cia")
    """.trimIndent().let { "$it\n" }
    }

    internal fun windowsToolchain(): String {
        return $$"""
        set(CMAKE_SYSTEM_NAME Generic)
        set(CMAKE_SYSTEM_PROCESSOR armv6k)
        set(DEVKITPRO_ROOT "$ENV{DEVKITPRO}")
        if(NOT EXISTS "${DEVKITPRO_ROOT}/devkitARM/bin/arm-none-eabi-gcc.exe")
            set(DEVKITPRO_ROOT "C:/devkitPro")
        endif()
        file(TO_CMAKE_PATH "${DEVKITPRO_ROOT}" DEVKITPRO_ROOT)
        set(CMAKE_C_COMPILER "${DEVKITPRO_ROOT}/devkitARM/bin/arm-none-eabi-gcc.exe")
        set(CMAKE_CXX_COMPILER "${DEVKITPRO_ROOT}/devkitARM/bin/arm-none-eabi-g++.exe")
        set(CMAKE_C_FLAGS_INIT "-march=armv6k -mtune=mpcore -mfloat-abi=hard -mtp=soft -mword-relocations -ffunction-sections -D__3DS__")
        set(CMAKE_CXX_FLAGS_INIT "${CMAKE_C_FLAGS_INIT}")
        set(CMAKE_EXE_LINKER_FLAGS_INIT "-specs=3dsx.specs")
        set(CMAKE_TRY_COMPILE_TARGET_TYPE STATIC_LIBRARY)
    """.trimIndent().let { "$it\n" }
    }

    internal fun ciaMetadata(name: String, options: ProjectOptions = ProjectOptions()): String {
        val code = options.productCode.ifBlank {
            name.uppercase().filter(Char::isLetterOrDigit).take(4).padEnd(4, 'X')
        }.uppercase()
        val unique = options.uniqueId.ifBlank {
            (0xF0000 or (name.hashCode() and 0xFFFF)).toString(16)
        }.removePrefix("0x").removePrefix("0X").uppercase()
        return """
            BasicInfo:
              Title: $name
              ProductCode: CTR-P-$code
              Logo: Homebrew
            TitleInfo:
              Category: Application
              UniqueId: 0x$unique
            Option:
              UseOnSD: true
              FreeProductCode: true
              EnableCrypt: false
            SystemControlInfo:
              StackSize: 0x40000
        """.trimIndent().let { "$it\n" }
    }

    private fun writeImage(path: Path, width: Int, height: Int, name: String) {
        val image = ImageUtil.createImage(width, height, BufferedImage.TYPE_INT_RGB)
        val graphics = image.createGraphics()
        try {
            graphics.color = JBColor(0x18305A, 0x18305A)
            graphics.fillRect(0, 0, width, height)
            graphics.color = JBColor(0x46C1CC, 0x46C1CC)
            graphics.fillRect(0, height - maxOf(4, height / 10), width, maxOf(4, height / 10))
            graphics.color = JBColor.WHITE
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
            graphics.font = Font(
                "SansSerif",
                Font.BOLD,
                if (width == 48) {
                    16
                } else {
                    25
                }
            )
            graphics.drawString(
                if (width == 48) {
                    "3DS"
                } else {
                    name.take(14)
                },
                4,
                height / 2
            )
        } finally {
            graphics.dispose()
        }
        ImageIO.write(image, "png", path.toFile())
    }

    private fun writeSilence(path: Path) {
        val sampleCount = 22050
        val samples = sampleCount * 2
        val wave = ByteBuffer.allocate(44 + samples).order(ByteOrder.LITTLE_ENDIAN)
        wave.put("RIFF".toByteArray()).putInt(36 + samples).put("WAVEfmt ".toByteArray())
        wave.putInt(16).putShort(1.toShort()).putShort(1.toShort()).putInt(22050).putInt(44100)
        wave.putShort(2.toShort()).putShort(16.toShort()).put("data".toByteArray()).putInt(samples)
        Files.write(path, wave.array())
    }

    private fun mainSource(name: String): String {
        return """
        #include <3ds.h>
        #include <stdio.h>

        int main(void) {
            gfxInitDefault();
            consoleInit(GFX_TOP, NULL);
            printf("Hello from $name!\nPress START to exit.\n");
            while (aptMainLoop()) {
                hidScanInput();
                if (hidKeysDown() & KEY_START) {
                    break;
                }
                gspWaitForVBlank();
                gfxSwapBuffers();
            }
            gfxExit();
            return 0;
        }
    """.trimIndent().let { "$it\n" }
    }

    private fun readme(name: String): String {
        return """
        # $name

        Nintendo 3DS homebrew project for CLion.

        The plugin installs the devkitPro `3ds-dev` group and configures the project
        when it opens. Build the `${name}_3dsx` target. If setup fails, use
        **Tools > Nintendo 3DS > Set Up Current 3DS Project** to retry.

        The output is `<cmake build directory>/$name.3dsx`. Use **Build > Build and Export CIA**
        to create `dist/$name.cia`. Edit `assets/app.rsf`, `icon.png`,
        `banner.png`, `title.txt`, `description.txt`, and `publisher.txt` to customize the Home Menu
        entry. The project uses C11, including GNU extensions for the devkitPro toolchain. **Send CIA to 3DS** uses
        FBI's Remote Install > Receive URLs over the network on your console.
    """.trimIndent().let { "$it\n" }
    }
}
