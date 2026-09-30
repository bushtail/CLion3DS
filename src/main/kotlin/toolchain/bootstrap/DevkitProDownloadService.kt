package toolchain.bootstrap

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.HexFormat
import java.time.Duration

class DevkitProDownloadService(
    private val client: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(20))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()
) {
    fun download(
        uri: URI,
        destination: Path,
        sha256: String? = null
    ): Path {
        require(uri.scheme == "https") { "Only HTTPS downloads are supported: $uri" }
        destination.parent?.let(Files::createDirectories)

        val request = HttpRequest.newBuilder(uri)
            .header("User-Agent", USER_AGENT)
            .timeout(Duration.ofMinutes(20))
            .GET()
            .build()

        val response = client.send(
            request,
            HttpResponse.BodyHandlers.ofFile(destination)
        )

        if (response.statusCode() !in 200..299) {
            Files.deleteIfExists(destination)

            error("Download failed with HTTP ${response.statusCode()}: $uri")
        }

        if (sha256 != null) {
            val actual = MessageDigest.getInstance("SHA-256").let { digest ->
                Files.newInputStream(destination).use { stream ->
                    val buffer = ByteArray(8192)
                    while (true) {
                        val count = stream.read(buffer)
                        if (count < 0) {
                            break
                        }
                        digest.update(buffer, 0, count)
                    }
                }
                HexFormat.of().formatHex(digest.digest())
            }
            if (!actual.equals(sha256, ignoreCase = true)) {
                Files.deleteIfExists(destination)
                error("SHA-256 mismatch for $uri")
            }
        }

        return destination
    }

    private companion object {
        const val USER_AGENT = "CLion3DS/1.0"
    }
}
