package toolchain.bootstrap

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ProvisioningTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun parsesOnlyAssetsAndTheirDigests() {
        val json = """{"name":"release","assets":[{"name":"tool.zip","browser_download_url":"https://github.com/example/tool.zip","digest":"sha256:abc123"}]}"""
        val assets = GithubReleaseResolver().parseAssets(json)
        assertEquals(1, assets.size)
        assertEquals("tool.zip", assets.single().name)
        assertEquals("abc123", assets.single().sha256)
    }

    @Test fun refusesArchivePathTraversal() {
        val archive = temporary.root.toPath().resolve("unsafe.zip")
        ZipOutputStream(Files.newOutputStream(archive)).use { zip ->
            zip.putNextEntry(ZipEntry("../outside.exe"))
            zip.write(byteArrayOf(1, 2, 3))
            zip.closeEntry()
        }
        val destination = temporary.root.toPath().resolve("emulator")
        Files.createDirectories(destination)
        try {
            AzaharInstaller().extract(archive, destination)
            fail("Expected unsafe path to be rejected")
        } catch (_: IllegalArgumentException) {
            assertFalse(Files.exists(temporary.root.toPath().resolve("outside.exe")))
        }
    }

    @Test fun extractsPortableEmulatorArchive() {
        val archive = temporary.root.toPath().resolve("emulator.zip")
        ZipOutputStream(Files.newOutputStream(archive)).use { zip ->
            zip.putNextEntry(ZipEntry("azaharplus-windows/azahar.exe"))
            zip.write(byteArrayOf(1, 2, 3))
            zip.closeEntry()
        }
        val destination = temporary.root.toPath().resolve("installed")
        Files.createDirectories(destination)
        AzaharInstaller().extract(archive, destination)
        assertTrue(Files.isRegularFile(destination.resolve("azaharplus-windows/azahar.exe")))
    }
}
