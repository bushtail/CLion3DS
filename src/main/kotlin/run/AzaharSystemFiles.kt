package run

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import java.util.zip.ZipInputStream

/** Manages the emulator's already extracted system files, never raw NAND images. */
object AzaharSystemFiles {
    private val names = listOf("nand", "sysdata")

    fun userDirectory(): Path {
        val appData = System.getenv("APPDATA") ?: error("Automatic emulator firmware import currently supports Windows only.")

        return Path.of(appData, "AzaharPlus").toAbsolutePath().normalize()
    }

    fun backupDirectory(): Path {
        val appData = System.getenv("APPDATA") ?: error("Automatic emulator firmware import currently supports Windows only.")

        return Path.of(appData, "CLion3DS", "emulator-firmware-backups").toAbsolutePath().normalize()
    }

    fun backupCurrent(): Path {
        return backup(userDirectory(), backupDirectory())
    }

    fun import(source: Path): Path? {
        return importPackage(source, userDirectory(), backupDirectory())
    }

    internal fun importPackage(source: Path, user: Path, backups: Path): Path? {
        if (Files.isDirectory(source)) {
            return import(source, user, backups)
        }
        require(Files.isRegularFile(source) && source.fileName.toString().endsWith(".zip", ignoreCase = true)) {
            "Choose a folder or ZIP containing emulator nand and/or sysdata directories. Raw NAND.bin images are not supported."
        }
        Files.createDirectories(backups)
        val extracted = Files.createTempDirectory(backups, ".firmware-import-")
        try {
            extractZip(source, extracted)
            val root = firmwareRoot(extracted)
            return import(root, user, backups)
        } finally {
            deleteTree(extracted)
        }
    }

    internal fun backup(user: Path, backups: Path): Path {
        checkEmulatorClosed()
        val folders = names.filter { Files.isDirectory(user.resolve(it)) }
        require(folders.isNotEmpty()) { "The emulator has no firmware to back up at $user" }
        val destination = newBackupPath(backups)
        Files.createDirectories(destination)
        for (name in folders) {
            copyTree(user.resolve(name), destination.resolve(name))
        }
        return destination
    }

    /** Returns the backup of the old files, if there were any. */
    internal fun import(source: Path, user: Path, backups: Path): Path? {
        checkEmulatorClosed()
        val input = firmwareRoot(source.toAbsolutePath().normalize())
        require(Files.isDirectory(input)) {
            "Choose a firmware folder or ZIP containing the emulator's nand and/or sysdata directories. Raw NAND.bin images cannot be imported directly."
        }
        require(!Files.isDirectory(input.resolve("updates"))) {
            "This ZIP contains an updates folder for a physical 3DS. The emulator cannot use it as a firmware backup. Import an emulator data folder containing nand and/or sysdata instead."
        }
        val folders = names.filter { Files.isDirectory(input.resolve(it)) }
        require(folders.isNotEmpty()) {
            "No nand or sysdata directory found in $input. The IDE needs an extracted emulator firmware backup."
        }
        require(folders.any { name -> Files.walk(input.resolve(name)).use { entries -> entries.anyMatch(Files::isRegularFile) } }) {
            "The selected firmware folder contains no files."
        }
        val target = user.toAbsolutePath().normalize()
        require(!input.startsWith(target) && !target.startsWith(input)) {
            "The firmware backup must be outside the emulator's active data directory."
        }
        Files.createDirectories(target)
        val staging = target.resolve(".clion3ds-import-${UUID.randomUUID()}")
        val backup = if (folders.any { Files.exists(target.resolve(it)) }) {
            newBackupPath(backups)
        } else {
            null
        }
        val installed = mutableSetOf<String>()
        try {
            Files.createDirectories(staging)
            for (name in folders) {
                copyTree(input.resolve(name), staging.resolve(name))
            }
            if (backup != null) {
                Files.createDirectories(backup)
                for (name in folders) {
                    val existing = target.resolve(name)
                    if (Files.exists(existing)) {
                        Files.move(existing, backup.resolve(name))
                    }
                }
            }
            for (name in folders) {
                Files.move(staging.resolve(name), target.resolve(name))
                installed.add(name)
            }
            return backup
        } catch (failure: Exception) {
            for (name in folders) {
                val old = backup?.resolve(name)
                val current = target.resolve(name)
                if (name in installed && Files.exists(current)) {
                    deleteTree(current)
                }
                if (old != null && Files.exists(old)) {
                    Files.move(old, current)
                }
            }
            throw failure
        } finally {
            if (Files.exists(staging)) {
                deleteTree(staging)
            }
        }
    }

    private fun newBackupPath(parent: Path): Path {
        val stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
        return parent.resolve("firmware-$stamp-${UUID.randomUUID().toString().take(8)}")
    }

    private fun firmwareRoot(source: Path): Path {
        if (!Files.isDirectory(source) || names.any { Files.isDirectory(source.resolve(it)) }) {
            return source
        }
        val children = Files.list(source).use { entries -> entries.filter(Files::isDirectory).toList() }
        return if (children.size == 1 &&
            (names.any { Files.isDirectory(children[0].resolve(it)) } || Files.isDirectory(children[0].resolve("updates")))) {
            children[0]
        } else {
            source
        }
    }

    private fun extractZip(archive: Path, destination: Path) {
        var count = 0
        var bytes = 0L
        ZipInputStream(Files.newInputStream(archive)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                require(++count <= 100_000) { "Firmware ZIP contains too many entries." }
                val target = destination.resolve(entry.name.replace('\\', '/')).normalize()
                require(target.startsWith(destination)) { "Unsafe path in firmware ZIP: ${entry.name}" }
                if (entry.isDirectory) {
                    Files.createDirectories(target)
                } else {
                    Files.createDirectories(target.parent)
                    Files.newOutputStream(target).use { output ->
                        val buffer = ByteArray(8192)
                        while (true) {
                            val read = zip.read(buffer)
                            if (read < 0) {
                                break
                            }
                            bytes += read
                            require(bytes <= 8_000_000_000L) { "Firmware ZIP expands beyond 8 GB." }
                            output.write(buffer, 0, read)
                        }
                    }
                }
                zip.closeEntry()
            }
        }
    }

    private fun copyTree(source: Path, destination: Path) {
        Files.walk(source).use { entries ->
            entries.forEach { entry ->
                require(!Files.isSymbolicLink(entry)) { "Symbolic links are not supported in emulator firmware backups: $entry" }
                val target = destination.resolve(source.relativize(entry))
                if (Files.isDirectory(entry)) {
                    Files.createDirectories(target)
                } else {
                    Files.copy(entry, target, StandardCopyOption.COPY_ATTRIBUTES)
                }
            }
        }
    }

    private fun deleteTree(root: Path) {
        Files.walk(root).use { entries ->
            entries.sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists)
        }
    }

    private fun checkEmulatorClosed() {
        val running = ProcessHandle.allProcesses().anyMatch { process ->
            process.info().command().orElse("").substringAfterLast('\\').substringAfterLast('/')
                .equals("azahar.exe", ignoreCase = true)
        }
        check(!running) { "Close the emulator before importing or backing up firmware." }
    }
}
