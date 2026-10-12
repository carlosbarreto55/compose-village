package village.app.server

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import village.compiler.PipelineResult
import village.compiler.PlayerPipeline
import village.preview.PreviewEngine
import village.preview.PreviewState
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.FileDescriptor
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess

/**
 * The out-of-process variant (design D10, task 7.1): a helper JVM on the game's classpath that runs
 * the same [PlayerPipeline] and [PreviewEngine] as the in-process game, driven over stdin/stdout
 * with the [Protocol]. Launch: `java -cp <game classpath> village.app.server.CompileServerKt`.
 *
 * Requests are handled one at a time on the main thread, which is also the compile thread.
 */
class CompileServer internal constructor(private val channel: DataOutputStream) : AutoCloseable {
    private val pipeline = PlayerPipeline()
    private val engine = PreviewEngine()

    internal fun serve(input: DataInputStream) {
        reply(Reply.Ready(ProcessHandle.current().pid(), System.getProperty("java.home")))
        while (true) {
            val payload = Protocol.read(input) ?: return // the game closed our stdin
            val start = System.nanoTime()
            val request = Request.decode(payload)
            val reply = try {
                when (request) {
                    is Request.Compile -> compile(request, start)
                    is Request.Click -> click(request, start)
                    is Request.Fault -> fault(request.kind)
                    Request.Shutdown -> return
                }
            } catch (e: Exception) {
                Reply.Failure(e.stackTraceToString().lines().take(8).joinToString("\n"))
            }
            reply(reply)
        }
    }

    private fun compile(request: Request.Compile, start: Long): Reply {
        val result = pipeline.run(request.source, request.entryFunction)
        val entry = result.entry ?: run {
            engine.unload().get(5, TimeUnit.SECONDS)
            return Reply.Result(false, result.diagnostics, result.timings, null, null, 0.0, 0.0, millisSince(start))
        }
        val state = engine.load(entry, request.source.lines().size, result.loader).get(30, TimeUnit.SECONDS)
        return state.toReply(true, start, result)
    }

    private fun click(request: Request.Click, start: Long): Reply {
        val clicked = engine.click(request.text).get(10, TimeUnit.SECONDS)
        val state = engine.renderNow().get(10, TimeUnit.SECONDS)
        return state.toReply(clicked, start, null)
    }

    private fun PreviewState.toReply(ok: Boolean, start: Long, result: PipelineResult?): Reply.Result {
        val encodeStart = System.nanoTime()
        val png = frame?.let(::encodePng)
        val encodeMillis = (System.nanoTime() - encodeStart) / 1e6
        val snapshot = engine.snapshot().get(10, TimeUnit.SECONDS)
        return Reply.Result(ok, result?.diagnostics.orEmpty(), result?.timings, error, png, renderMillis, encodeMillis, millisSince(start), snapshot)
    }

    private fun fault(kind: String): Nothing {
        when (kind) {
            "exit" -> exitProcess(0) // what player code calling System.exit(0) would do
            "oom" -> { // the helper runs with -XX:+ExitOnOutOfMemoryError
                val hog = ArrayList<LongArray>()
                while (true) hog += LongArray(1 shl 20)
            }
            "wedge" -> { // stuck in code that never returns, e.g. a hung compile
                @Suppress("ControlFlowWithEmptyBody")
                while (true) {}
            }
            else -> error("unknown fault '$kind'")
        }
    }

    private fun reply(reply: Reply) = synchronized(channel) { Protocol.write(channel, reply.encode()) }

    private fun millisSince(start: Long) = (System.nanoTime() - start) / 1e6

    override fun close() = engine.close()

    companion object {
        internal fun encodePng(frame: ImageBitmap): ByteArray =
            Image.makeFromBitmap(frame.asSkiaBitmap()).use { it.encodeToData(EncodedImageFormat.PNG)!!.bytes }
    }
}

fun main() {
    // stdout belongs to the protocol. Anything else that prints (Compose's error logging, the
    // compiler) goes to stderr, which the game logs.
    val channel = DataOutputStream(BufferedOutputStream(FileOutputStream(FileDescriptor.out)))
    System.setOut(System.err)
    CompileServer(channel).use { it.serve(DataInputStream(BufferedInputStream(System.`in`))) }
    exitProcess(0)
}
