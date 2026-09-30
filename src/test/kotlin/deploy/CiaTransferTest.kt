@file:Suppress("HttpUrlsUsage")

package deploy

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.DataInputStream
import java.net.HttpURLConnection
import java.net.ServerSocket
import java.net.URI
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class CiaTransferTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun sendsFbiUrlAndServesCompleteCia() {
        val payload = ByteArray(8192) { it.toByte() }
        val cia = temporary.root.toPath().resolve("Example.cia")
        Files.write(cia, payload)
        ServerSocket(0).use { receiver ->
            val worker = Executors.newSingleThreadExecutor()
            try {
                val received = worker.submit<ByteArray> {
                    receiver.accept().use { socket ->
                        val input = DataInputStream(socket.getInputStream())
                        val length = input.readInt()
                        assertTrue(length in 1..512)
                        val address = String(input.readNBytes(length), StandardCharsets.US_ASCII)
                        val connection = URI.create("http://$address").toURL().openConnection() as HttpURLConnection
                        connection.connectTimeout = 3000
                        connection.readTimeout = 3000
                        connection.inputStream.use { it.readAllBytes() }
                    }
                }
                CiaPublisher.sendToFbi(cia, "127.0.0.1", receiver.localPort)
                assertArrayEquals(payload, received.get(5, TimeUnit.SECONDS))
            } finally {
                worker.shutdownNow()
            }
        }
    }
}
