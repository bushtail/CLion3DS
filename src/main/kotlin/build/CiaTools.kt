package build

import com.intellij.openapi.application.PathManager
import toolchain.bootstrap.DevkitProDownloadService
import toolchain.bootstrap.GithubReleaseResolver
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.zip.ZipInputStream

/** CIA packaging tools are cached outside project folders and fetched on first use. */
object CiaTools {
    @Synchronized
    fun ensureInstalled(): CiaToolPaths {
        require(System.getProperty("os.name").lowercase().contains("win")) {
            "Automatic CIA tool installation currently supports Windows."
        }
        val directory = Path.of(PathManager.getSystemPath(), "clion3ds", "cia-tools")
        Files.createDirectories(directory)
        val makerom = directory.resolve("makerom.exe")
        val bannertool = directory.resolve("bannertool.exe")
        if (!Files.isRegularFile(makerom)) {
            install("3DSGuy/Project_CTR", { it.contains("win_x86_64") && it.endsWith(".zip") }, "makerom.exe", makerom)
        }
        if (!Files.isRegularFile(bannertool)) {
            install("carstene1ns/3ds-bannertool", { it.contains("windows") && it.endsWith(".zip") }, "bannertool.exe", bannertool)
        }
        return CiaToolPaths(makerom, bannertool)
    }

    private fun install(repository: String, selector: (String) -> Boolean, executable: String, destination: Path) {
        val release = GithubReleaseResolver().latestAsset(repository, selector)
        val archive = Files.createTempFile(destination.parent, "cia-tool-", ".zip")
        val staged = Files.createTempFile(destination.parent, "cia-executable-", ".exe")
        try {
            DevkitProDownloadService().download(release.downloadUri, archive, release.sha256)
            var found = false
            ZipInputStream(Files.newInputStream(archive)).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    if (!entry.isDirectory && entry.name.replace('\\', '/').substringAfterLast('/') == executable) {
                        Files.newOutputStream(staged).use { zip.copyTo(it) }
                        found = true
                        break
                    }
                }
            }
            check(found && Files.size(staged) > 0) { "$executable was not found in the $repository release" }
            Files.move(staged, destination, StandardCopyOption.REPLACE_EXISTING)
        } finally {
            Files.deleteIfExists(archive)
            Files.deleteIfExists(staged)
        }
    }
}
