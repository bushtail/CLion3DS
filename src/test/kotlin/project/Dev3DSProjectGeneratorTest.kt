package project

import com.intellij.platform.eel.fs.EelFiles
import model.EGraphicsLibrary
import model.ProjectOptions
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Files
import javax.imageio.ImageIO

class Dev3DSProjectGeneratorTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun generatesClionProjectAnd3dsxTarget() {
        val directory = temporary.root.toPath().resolve("Hello3DS")
        Dev3DSProjectGenerator.create(directory, "Hello3DS")

        val cmake = EelFiles.readString(directory.resolve("CMakeLists.txt"))
        val source = EelFiles.readString(directory.resolve("source/main.c"))

        assertTrue(cmake.contains("3DS.cmake"))
        assertTrue(cmake.contains("C:/devkitPro"))
        assertTrue(cmake.contains("if(CMAKE_HOST_WIN32 AND NOT EXISTS"))
        assertTrue(!cmake.contains("if(WIN32)"))
        assertTrue(cmake.contains("add_custom_target(Hello3DS_3dsx ALL"))
        assertTrue(cmake.contains("C_STANDARD 11 C_STANDARD_REQUIRED ON C_EXTENSIONS ON"))
        assertTrue(source.contains("while (aptMainLoop())"))

        assertTrue(Files.isRegularFile(directory.resolve("README.md")))
        assertTrue(Files.isRegularFile(directory.resolve("cmake/3ds-windows.cmake")))
        assertTrue(Files.isRegularFile(directory.resolve("assets/app.rsf")))

        assertTrue(EelFiles.readString(directory.resolve("assets/title.txt")).trim() == "Hello3DS")
        assertTrue(EelFiles.readString(directory.resolve("assets/description.txt")).trim() == "Hello3DS for Nintendo 3DS")

        assertTrue(ImageIO.read(directory.resolve("assets/icon.png").toFile()).width == 48)
        assertTrue(ImageIO.read(directory.resolve("assets/banner.png").toFile()).width == 256)

        assertTrue(EelFiles.readString(directory.resolve("assets/app.rsf")).contains("UniqueId: 0x"))
        assertTrue(EelFiles.readAllBytes(directory.resolve("assets/silence.wav")).copyOfRange(0, 4).contentEquals("RIFF".toByteArray()))

        assertTrue(!cmake.contains("target_link_options(Hello3DS PRIVATE \"-specs=3dsx.specs\")"))
    }

    @Test fun refusesToOverwriteExistingFiles() {
        val directory = temporary.newFolder("existing").toPath()
        val original = directory.resolve("CMakeLists.txt")
        Files.writeString(original, "user content")

        try {
            Dev3DSProjectGenerator.create(directory, "Example")
            fail("Expected existing project to be rejected")
        } catch (_: IllegalArgumentException) {
            assertTrue(EelFiles.readString(original) == "user content")
        }
    }

    @Test fun includesSelectedGraphicsLibraries() {
        val cmake = Dev3DSProjectGenerator.cmake("Sample", ProjectOptions(graphicsLibrary = EGraphicsLibrary.Citro2D))

        assertTrue(cmake.contains("target_link_libraries(Sample PRIVATE citro2d citro3d ctru m)"))
    }

    @Test fun wizardAllowsIdeMetadataButProtectsProjectFiles() {
        val directory = temporary.newFolder("wizard").toPath()

        Files.createDirectory(directory.resolve(".idea"))
        Dev3DSProjectGenerator.createFromWizard(directory, "My_3DS")

        assertTrue(Files.isRegularFile(directory.resolve("CMakeLists.txt")))
        assertTrue(Dev3DSProjectGenerator.cmakeTargetName("3DS demo") == "Project_3DS_demo")
    }

    @Test fun usesConfiguredDevkitProRootInTemplate() {
        val cmake = Dev3DSProjectGenerator.cmake("Sample", devkitProRoot = temporary.root.toPath().resolve("devkitPro"))

        assertTrue(cmake.contains("set(DEVKITPRO_ROOT"))
        assertTrue(cmake.contains("/devkitPro"))
    }

    @Test fun writesSelectedHomeMenuMetadata() {
        val directory = temporary.root.toPath().resolve("custom")

        Dev3DSProjectGenerator.create(directory, "Game", ProjectOptions(
            appTitle = "My Game", description = "A small adventure", publisher = "Example Studio",
            productCode = "AB12", uniqueId = "0xF1234"
        ))

        assertTrue(EelFiles.readString(directory.resolve("assets/title.txt")).trim() == "My Game")
        assertTrue(EelFiles.readString(directory.resolve("assets/description.txt")).trim() == "A small adventure")
        assertTrue(EelFiles.readString(directory.resolve("assets/publisher.txt")).trim() == "Example Studio")

        val rsf = EelFiles.readString(directory.resolve("assets/app.rsf"))
        assertTrue(rsf.contains("ProductCode: CTR-P-AB12"))
        assertTrue(rsf.contains("UniqueId: 0xF1234"))
    }

}
