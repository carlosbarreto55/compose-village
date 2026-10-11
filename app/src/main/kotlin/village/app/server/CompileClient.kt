package village.app.server

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.IOException
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

/** The helper died or stopped answering. [exitCode] is null while it is still running (hung). */
class HelperFailure(message: String, val exitCode: Int?) : IOException(message)

/**
 * The game's side of the [CompileServer]: spawns the helper JVM on this JVM's classpath and sends
 * it requests. A helper that exits or does not answer within a request's timeout raises
 * [HelperFailure]; the caller then [close]s this client and spawns a new one.
 *
 * Not thread-safe: one request at a time, like the in-process pipeline.
 */
internal class CompileClient private constructor(private val process: Process) : AutoCloseable {
    private val requests = DataOutputStream(BufferedOutputStream(process.outputStream))
    private val replies = LinkedBlockingQueue<Result<Reply>>()

    /** Wall time from starting the process to receiving [Reply.Ready]. */
    var spawnMillis = 0.0
        private set
    lateinit var ready: Reply.Ready
        private set

    val pid get() = process.pid()
    val alive get() = process.isAlive

    init {
        Thread({
            val input = DataInputStream(BufferedInputStream(process.inputStream))
            while (true) {
                val reply = try {
                    Protocol.read(input)?.let(Reply::decode) ?: throw HelperFailure("helper closed its output", exitCodeAfterEof())
                } catch (e: HelperFailure) {
                    replies += Result.failure(e)
                    return@Thread
                } catch (e: Exception) { // unreadable output: the channel is unusable either way
                    replies += Result.failure(HelperFailure("helper output broke: ${e.message}", exitCodeAfterEof()))
                    return@Thread
                }
                replies += Result.success(reply)
            }
        }, "compile-client-${process.pid()}").apply { isDaemon = true; start() }
    }

    fun compile(source: String, entryFunction: String = "PlayerScreen", timeoutMillis: Long = 60_000) =
        request(Request.Compile(source, entryFunction), timeoutMillis)

    fun click(text: String, timeoutMillis: Long = 10_000) = request(Request.Click(text), timeoutMillis)

    /** Sends a spike-only fault; see [Request.Fault]. */
    fun fault(kind: String) = send(Request.Fault(kind))

    /** Waits for the helper to exit; returns its exit code, or null if it is still running. */
    fun awaitExit(timeoutMillis: Long): Int? =
        if (process.waitFor(timeoutMillis, TimeUnit.MILLISECONDS)) process.exitValue() else null

    private fun request(request: Request, timeoutMillis: Long): Reply.Result {
        send(request)
        return when (val reply = next(timeoutMillis)) {
            is Reply.Result -> reply
            is Reply.Failure -> error("helper failed: ${reply.message}")
            is Reply.Ready -> error("unexpected $reply")
        }
    }

    private fun send(request: Request) {
        try {
            Protocol.write(requests, request.encode())
        } catch (e: IOException) {
            throw HelperFailure("helper is gone: $e", exitCodeAfterEof())
        }
    }

    private fun next(timeoutMillis: Long): Reply =
        replies.poll(timeoutMillis, TimeUnit.MILLISECONDS)?.getOrThrow()
            ?: throw HelperFailure("helper did not answer within $timeoutMillis ms", process.takeUnless { it.isAlive }?.exitValue())

    private fun exitCodeAfterEof(): Int? = awaitExit(2_000)

    /** Kills the helper without asking, e.g. when it stopped answering. */
    fun kill() {
        process.destroyForcibly().waitFor(5, TimeUnit.SECONDS)
    }

    /** Asks the helper to shut down, and kills it if it does not exit promptly. */
    override fun close() {
        if (process.isAlive) {
            runCatching { send(Request.Shutdown) }
            if (!process.waitFor(5, TimeUnit.SECONDS)) process.destroyForcibly().waitFor(5, TimeUnit.SECONDS)
        }
    }

    companion object {
        /**
         * JVM options of the helper. It exits on OOM: a helper is disposable, the game respawns it.
         * The JVM's own messages (e.g. "Terminating due to java.lang.OutOfMemoryError") go to
         * stderr, because stdout carries the protocol.
         */
        val DEFAULT_JVM_ARGS = listOf(
            "-Xmx1g", "-XX:+UseG1GC", "-XX:+ExitOnOutOfMemoryError", "-XX:+DisplayVMOutputToStderr", "-Djava.awt.headless=true",
        )

        /** Starts a helper and waits for it to report ready. Its stderr is appended to [log]. */
        fun spawn(log: File, jvmArgs: List<String> = DEFAULT_JVM_ARGS, timeoutMillis: Long = 30_000): CompileClient {
            log.parentFile?.mkdirs()
            val java = ProcessHandle.current().info().command().orElse(File(System.getProperty("java.home"), "bin/java").path)
            val start = System.nanoTime()
            val process = ProcessBuilder(listOf(java) + jvmArgs + listOf("-cp", System.getProperty("java.class.path"), "village.app.server.CompileServerKt"))
                .redirectError(ProcessBuilder.Redirect.appendTo(log))
                .start()
            val client = CompileClient(process)
            try {
                client.ready = client.next(timeoutMillis) as? Reply.Ready ?: error("helper did not start with a Ready message")
            } catch (t: Throwable) {
                process.destroyForcibly()
                throw t
            }
            client.spawnMillis = (System.nanoTime() - start) / 1e6
            return client
        }
    }
}
