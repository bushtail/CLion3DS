package actions.dev3ds

import java.nio.file.Files
import java.nio.file.Path

internal object Dev3DSArtifactFinder {
    fun findArtifact(root: Path): Path? {
        return findBuildDirectories(root).asSequence().flatMap { directory ->
            Files.walk(directory, 3).use { stream ->
                stream.filter { Files.isRegularFile(it) && it.toString().endsWith(".3dsx") }.toList()
            }.asSequence()
        }.firstOrNull()
    }

    private fun findBuildDirectories(root: Path): List<Path> {
        if (!Files.isDirectory(root)) {
            return emptyList()
        }

        val topLevel = Files.list(root).use { stream ->
            stream.filter { Files.isDirectory(it) && Files.isRegularFile(it.resolve("CMakeCache.txt")) }.toList()
        }
        return (listOf(root.resolve("build/3ds")) + topLevel)
            .filter { Files.isRegularFile(it.resolve("CMakeCache.txt")) }
    }
}
