package toolchain.process

import java.io.IOException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class ToolchainProcessRunner {
    fun run(request: ToolchainProcessRequest): ToolchainProcessResult {
        val command = buildList {
            add(request.executable.toString())
            addAll(request.arguments)
        }

        val builder = ProcessBuilder(command)

        request.workingDirectory?.let {
            builder.directory(it.toFile())
        }

        if (request.environment.isNotEmpty()) {
            builder.environment().putAll(request.environment)
        }

        val process = try {
            builder.start()
        } catch (exception: IOException) {
            throw ToolchainProcessException(
                "Failed to start ${request.executable}",
                exception
            )
        }

        val executor = Executors.newFixedThreadPool(2)

        try {
            val stdoutFuture = executor.submit<String> {
                process.inputStream.bufferedReader().use {
                    it.readText()
                }
            }

            val stderrFuture = executor.submit<String> {
                process.errorStream.bufferedReader().use {
                    it.readText()
                }
            }

            val finished = process.waitFor(
                request.timeout.toMillis(),
                TimeUnit.MILLISECONDS
            )

            if (!finished) {
                process.destroyForcibly()
                process.waitFor()

                throw ToolchainProcessException("Process timed out after ${request.timeout}: ${request.executable}")
            }

            return ToolchainProcessResult(
                exitCode = process.exitValue(),
                standardOutput = stdoutFuture.get(),
                standardError = stderrFuture.get()
            )
        } finally {
            executor.shutdownNow()
        }
    }
}
