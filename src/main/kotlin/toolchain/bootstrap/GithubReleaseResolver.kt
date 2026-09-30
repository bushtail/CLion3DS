package toolchain.bootstrap

import com.google.gson.JsonParser
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

class GithubReleaseResolver(private val client: HttpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).followRedirects(HttpClient.Redirect.NORMAL).build()) {
    fun latestAsset(repository: String, assetPredicate: (String) -> Boolean): GitHubReleaseAsset {
        val uri = URI.create("https://api.github.com/repos/$repository/releases/latest")

        val request = HttpRequest.newBuilder(uri)
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .header("User-Agent", USER_AGENT)
            .timeout(Duration.ofSeconds(60))
            .GET()
            .build()

        val response = client.send(
            request,
            HttpResponse.BodyHandlers.ofString()
        )

        if (response.statusCode() !in 200..299) {
            error("GitHub release lookup failed with HTTP ${response.statusCode()} for $repository")
        }

        val assets = parseAssets(response.body())

        return assets.firstOrNull {
            assetPredicate(it.name)
        } ?: error("No matching asset found in the latest $repository release.")
    }

    internal fun parseAssets(json: String): List<GitHubReleaseAsset> {
        val assets = JsonParser.parseString(json).asJsonObject.getAsJsonArray("assets")
            ?: error("GitHub release response did not contain assets")
        return assets.map { asset ->
            val fields = asset.asJsonObject
            GitHubReleaseAsset(
                name = fields.get("name").asString,
                downloadUri = URI.create(fields.get("browser_download_url").asString),
                sha256 = fields.get("digest")?.takeUnless { it.isJsonNull }?.asString
                    ?.takeIf { it.startsWith("sha256:") }?.removePrefix("sha256:")
            )
        }
    }

    private companion object {
        const val USER_AGENT = "CLion3DS/1.0"
    }
}
