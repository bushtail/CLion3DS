package run

import com.intellij.platform.eel.fs.EelFiles
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class AzaharSystemFilesTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun importPreservesPreviousSystemFilesAndCanRestoreThem() {
        val root = temporary.root.toPath()
        val user = root.resolve("Azahar")
        val backups = root.resolve("backups")
        val source = root.resolve("import")

        Files.createDirectories(user.resolve("nand"))
        Files.createDirectories(source.resolve("nand"))
        Files.createDirectories(source.resolve("sysdata"))
        Files.writeString(user.resolve("nand/old.dat"), "old")
        Files.writeString(source.resolve("nand/new.dat"), "new")
        Files.writeString(source.resolve("sysdata/font.bin"), "font")

        val old = AzaharSystemFiles.import(source, user, backups)
        assertNotNull(old)

        assertEquals("old", EelFiles.readString(old!!.resolve("nand/old.dat")))
        assertEquals("new", EelFiles.readString(user.resolve("nand/new.dat")))
        assertTrue(Files.exists(user.resolve("sysdata/font.bin")))
        assertFalse(Files.exists(user.resolve("nand/old.dat")))

        AzaharSystemFiles.import(old, user, backups)
        assertEquals("old", EelFiles.readString(user.resolve("nand/old.dat")))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rawNandImageIsRejected() {
        val root = temporary.root.toPath()
        val image = root.resolve("NAND.bin")
        Files.writeString(image, "raw image")
        AzaharSystemFiles.import(image, root.resolve("Azahar"), root.resolve("backups"))
    }

    @Test fun backupCopiesCurrentFilesWithoutRemovingThem() {
        val root = temporary.root.toPath()
        val user = root.resolve("Azahar")
        Files.createDirectories(user.resolve("sysdata"))
        Files.writeString(user.resolve("sysdata/font.bin"), "font")
        val backup = AzaharSystemFiles.backup(user, root.resolve("backups"))
        assertEquals("font", EelFiles.readString(backup.resolve("sysdata/font.bin")))
        assertEquals("font", EelFiles.readString(user.resolve("sysdata/font.bin")))
    }

    @Test fun importsZipWithOneContainingFolder() {
        val root = temporary.root.toPath()
        val archive = root.resolve("firmware.zip")
        ZipOutputStream(Files.newOutputStream(archive)).use { zip ->
            zip.putNextEntry(ZipEntry("backup/sysdata/font.bin"))
            zip.write("font".toByteArray())
            zip.closeEntry()
        }
        AzaharSystemFiles.importPackage(archive, root.resolve("Azahar"), root.resolve("backups"))
        assertEquals("font", EelFiles.readString(root.resolve("Azahar/sysdata/font.bin")))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsZipTraversal() {
        val root = temporary.root.toPath()
        val archive = root.resolve("firmware.zip")
        ZipOutputStream(Files.newOutputStream(archive)).use { zip ->
            zip.putNextEntry(ZipEntry("../outside.txt"))
            zip.write("bad".toByteArray())
            zip.closeEntry()
        }
        AzaharSystemFiles.importPackage(archive, root.resolve("Azahar"), root.resolve("backups"))
    }

    @Test fun explainsWhyConsoleUpdateZipCannotBeImported() {
        val root = temporary.root.toPath()
        val archive = root.resolve("11.17.0-50E-NEW.zip")
        ZipOutputStream(Files.newOutputStream(archive)).use { zip ->
            zip.putNextEntry(ZipEntry("updates/0004013000002D02.cia"))
            zip.write("console update".toByteArray())
            zip.closeEntry()
        }
        try {
            AzaharSystemFiles.importPackage(archive, root.resolve("Azahar"), root.resolve("backups"))
            throw AssertionError("Expected console update ZIP to be rejected")
        } catch (exception: IllegalArgumentException) {
            assertTrue(exception.message.orEmpty().contains("physical 3DS"))
        }
    }
}
