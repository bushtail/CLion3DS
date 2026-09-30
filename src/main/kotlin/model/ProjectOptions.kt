package model

data class ProjectOptions(
    val projectType: EProjectType = EProjectType.Application,
    val graphicsLibrary: EGraphicsLibrary = EGraphicsLibrary.None,
    val buildType: EBuildType = EBuildType.Debug,
    val appTitle: String = "",
    val description: String = "",
    val publisher: String = "Homebrew",
    val productCode: String = "",
    val uniqueId: String = ""
)
