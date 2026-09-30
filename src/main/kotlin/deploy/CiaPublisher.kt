package deploy

import com.sun.net.httpserver.HttpServer
import build.ActiveBuild
import build.CiaTools
import com.intellij.platform.eel.fs.EelFiles
import toolchain.process.ToolchainProcessRunner
import toolchain.process.ToolchainProcessRequest
import java.io.DataOutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

object CiaPublisher {
    fun buildAndExport(root: Path, build: Path): Path {
        val name = ActiveBuild.projectName(root)
        ActiveBuild.build(root, build)
        val tools = CiaTools.ensureInstalled()

        val assets = root.resolve("assets")
        val icon = build.resolve("icon.icn")
        val banner = build.resolve("banner.bnr")
        val title = assets.resolve("title.txt").takeIf(Files::isRegularFile)?.let {
            EelFiles.readString(it).trim()
        }?.takeIf(String::isNotEmpty) ?: name

        val description = assets.resolve("description.txt").takeIf(Files::isRegularFile)?.let {
            EelFiles.readString(it).trim()
        }?.takeIf(String::isNotEmpty) ?: "$title for Nintendo 3DS"

        val publisher = assets.resolve("publisher.txt").takeIf(Files::isRegularFile)?.let {
            EelFiles.readString(it).trim()
        }?.takeIf(String::isNotEmpty) ?: "Homebrew"

        execute(ToolchainProcessRequest(tools.bannertool, listOf("makesmdh", "-s", title,
            "-l", description, "-p", publisher, "-i", assets.resolve("icon.png").toString(),
            "-o", icon.toString()), root, timeout = Duration.ofMinutes(2)), "CIA icon"
        )

        execute(ToolchainProcessRequest(tools.bannertool, listOf("makebanner", "-i",
            assets.resolve("banner.png").toString(), "-a", assets.resolve("silence.wav").toString(),
            "-o", banner.toString()), root, timeout = Duration.ofMinutes(2)), "CIA banner"
        )

        val packaged = build.resolve("$name.cia")

        execute(ToolchainProcessRequest(tools.makerom, listOf("-f", "cia", "-o", packaged.toString(),
            "-target", "t", "-elf", build.resolve("$name.elf").toString(), "-rsf",
            assets.resolve("app.rsf").toString(), "-desc", "app:4", "-exefslogo",
            "-icon", icon.toString(), "-banner", banner.toString()), root,
            timeout = Duration.ofMinutes(5)), "CIA packaging"
        )

        check(Files.isRegularFile(packaged) && Files.size(packaged) > 0) { "makerom did not create $packaged" }

        val dist = root.resolve("dist")
        Files.createDirectories(dist)
        return Files.copy(packaged, dist.resolve("$name.cia"), java.nio.file.StandardCopyOption.REPLACE_EXISTING)
    }

    private fun execute(request: ToolchainProcessRequest, stage: String) {
        val result = ToolchainProcessRunner().run(request)
        if (!result.success) {
            error("$stage failed (${result.exitCode}):\n${result.standardError.ifBlank { result.standardOutput }.takeLast(4000)}")
        }
    }

    fun sendToFbi(cia: Path, consoleIp: String, consolePort: Int = 5000) {
        require(Files.isRegularFile(cia) && cia.fileName.toString().endsWith(".cia")) { "CIA file not found: $cia" }
        val server = HttpServer.create(InetSocketAddress("0.0.0.0", 0), 0)
        val delivery = CountDownLatch(1)
        val executor = Executors.newSingleThreadExecutor()
        server.executor = executor
        server.createContext("/${cia.fileName}") { exchange ->
            exchange.use { exchange ->
                if (exchange.requestMethod != "GET") {
                    exchange.sendResponseHeaders(405, -1)
                } else {
                    exchange.responseHeaders.add("Content-Type", "application/octet-stream")
                    exchange.sendResponseHeaders(200, Files.size(cia))
                    exchange.responseBody.use { output -> Files.newInputStream(cia).use { it.copyTo(output) } }
                    delivery.countDown()
                }
            }
        }
        server.start()
        try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(consoleIp, consolePort), 10000)
                val localIp = socket.localAddress.hostAddress
                val url = "$localIp:${server.address.port}/${cia.fileName}"
                val bytes = url.toByteArray(StandardCharsets.US_ASCII)
                DataOutputStream(socket.getOutputStream()).apply {
                    writeInt(bytes.size)
                    write(bytes)
                    flush()
                }
                check(delivery.await(5, TimeUnit.MINUTES)) {
                    "FBI did not download the CIA. Check that the console and computer are on the same network."
                }
            }
        } finally {
            server.stop(0)
            executor.shutdownNow()
        }
    }
}
