package toolchain.environment

object HostPlatformDetector {
    fun detect(): EHostPlatform {
        val osName = System.getProperty("os.name")
            ?.lowercase()
            ?: error("Unable to determine the host operating system.")

        return when {
            osName.contains("win") -> EHostPlatform.Windows
            osName.contains("mac") || osName.contains("darwin") -> EHostPlatform.MacOS
            osName.contains("linux") -> EHostPlatform.Linux
            else -> error("Unsupported host operating system: $osName")
        }
    }
}