package toolchain.bootstrap

import java.net.URI

data class GitHubReleaseAsset(
    val name: String,
    val downloadUri: URI,
    val sha256: String? = null
)
