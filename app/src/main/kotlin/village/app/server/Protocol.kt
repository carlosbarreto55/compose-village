package village.app.server

import village.compiler.Diagnostic
import village.compiler.Severity
import village.compiler.Timings
import village.preview.DpBounds
import village.preview.RuntimeError
import village.preview.SemanticsSnapshot
import village.preview.SnapshotNode
import village.preview.Viewport
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException

/**
 * The [CompileServer] wire protocol (design D10): length-prefixed frames over the helper's
 * stdin (requests) and stdout (replies). A frame is a magic number, the payload length and the
 * payload, whose first byte is the message type. The magic number turns stray bytes on stdout
 * (e.g. a native library printing) into a clear protocol error instead of a garbled length.
 *
 * Requests are answered in order, one reply each, except [Request.Fault] and [Request.Shutdown].
 */
internal object Protocol {
    private const val MAGIC = 0x56494C47 // "VILG"
    private const val MAX_FRAME = 64 * 1024 * 1024

    fun write(out: DataOutputStream, payload: ByteArray) {
        out.writeInt(MAGIC)
        out.writeInt(payload.size)
        out.write(payload)
        out.flush()
    }

    /** The next frame's payload, or null at end of stream. */
    fun read(input: DataInputStream): ByteArray? {
        val magic = try {
            input.readInt()
        } catch (_: EOFException) {
            return null
        }
        if (magic != MAGIC) {
            // Show what was printed instead, to find out who wrote to stdout.
            val stray = ByteArray(minOf(input.available(), 300)).also { input.readFully(it) }
            val text = String(java.nio.ByteBuffer.allocate(4).putInt(magic).array() + stray, Charsets.UTF_8)
            error("protocol corrupted: expected a frame, got stray output: $text")
        }
        val size = input.readInt()
        check(size in 0..MAX_FRAME) { "protocol corrupted: frame size $size" }
        return ByteArray(size).also { input.readFully(it) }
    }

    fun encode(block: DataOutputStream.() -> Unit): ByteArray =
        ByteArrayOutputStream().also { DataOutputStream(it).use(block) }.toByteArray()

    fun <T> decode(payload: ByteArray, block: DataInputStream.() -> T): T = DataInputStream(ByteArrayInputStream(payload)).use(block)

    // writeUTF is limited to 64 KB; player files and messages may be longer.
    fun DataOutputStream.writeText(s: String) {
        val bytes = s.toByteArray(Charsets.UTF_8)
        writeInt(bytes.size)
        write(bytes)
    }

    fun DataInputStream.readText(): String = String(ByteArray(readInt()).also { readFully(it) }, Charsets.UTF_8)

    // A snapshot is its viewport, then its roots; each node is written before its children (pre-order).
    fun DataOutputStream.writeSnapshot(s: SemanticsSnapshot) {
        writeFloat(s.viewport.widthDp); writeFloat(s.viewport.heightDp)
        writeInt(s.roots.size)
        s.roots.forEach { writeNode(it) }
    }

    private fun DataOutputStream.writeNode(n: SnapshotNode) {
        writeInt(n.id)
        writeInt(n.texts.size); n.texts.forEach { writeText(it) }
        writeInt(n.contentDescriptions.size); n.contentDescriptions.forEach { writeText(it) }
        writeBoolean(n.role != null); n.role?.let { writeText(it) }
        writeFloat(n.bounds.left); writeFloat(n.bounds.top); writeFloat(n.bounds.right); writeFloat(n.bounds.bottom)
        writeInt(n.children.size)
        n.children.forEach { writeNode(it) }
    }

    fun DataInputStream.readSnapshot() = SemanticsSnapshot(Viewport(readFloat(), readFloat()), List(readInt()) { readNode() })

    private fun DataInputStream.readNode(): SnapshotNode = SnapshotNode(
        id = readInt(),
        texts = List(readInt()) { readText() },
        contentDescriptions = List(readInt()) { readText() },
        role = if (readBoolean()) readText() else null,
        bounds = DpBounds(readFloat(), readFloat(), readFloat(), readFloat()),
        children = List(readInt()) { readNode() },
    )
}

/** Host -> helper. */
internal sealed interface Request {
    /** Compile, verify and load [source], show it in the helper's preview and reply with its first frame. */
    data class Compile(val source: String, val entryFunction: String = "PlayerScreen") : Request

    /** Click the first node whose text contains [text] and reply with the resulting frame. */
    data class Click(val text: String) : Request

    /**
     * Spike-only fault injection: make the helper fail the way a sandbox escape (`exit`), heap
     * exhaustion (`oom`) or a hang (`wedge`) would. The sandbox and guard block the real causes,
     * so the harness simulates them to measure recovery. No reply.
     */
    data class Fault(val kind: String) : Request

    data object Shutdown : Request

    fun encode(): ByteArray = Protocol.encode {
        with(Protocol) {
            when (val r = this@Request) {
                is Compile -> { writeByte(1); writeText(r.source); writeText(r.entryFunction) }
                is Click -> { writeByte(2); writeText(r.text) }
                is Fault -> { writeByte(3); writeText(r.kind) }
                Shutdown -> writeByte(4)
            }
        }
    }

    companion object {
        fun decode(payload: ByteArray): Request = Protocol.decode(payload) {
            with(Protocol) {
                when (val type = readByte().toInt()) {
                    1 -> Compile(readText(), readText())
                    2 -> Click(readText())
                    3 -> Fault(readText())
                    4 -> Shutdown
                    else -> error("unknown request type $type")
                }
            }
        }
    }
}

/** Helper -> host. */
internal sealed interface Reply {
    /** Sent once, as soon as the helper's `main` runs. */
    data class Ready(val pid: Long, val javaHome: String) : Reply

    /** The outcome of a [Request.Compile] or [Request.Click]. */
    class Result(
        /** Compile: the attempt compiled and loaded. Click: the node was found. */
        val ok: Boolean,
        val diagnostics: List<Diagnostic>,
        /** Compile only. */
        val timings: Timings?,
        val error: RuntimeError?,
        /** The latest frame as PNG, or null when there is no frame (failed compile, crash). */
        val png: ByteArray?,
        /** The helper's render time of [png] (CPU raster + pixel copy) and its PNG encoding time. */
        val renderMillis: Double,
        val encodeMillis: Double,
        /** Wall time the helper spent on the request, from decoding it to encoding the reply. */
        val serverMillis: Double,
        /** The scene's semantics snapshot, for acceptance criteria; null when there is no scene. */
        val snapshot: SemanticsSnapshot? = null,
    ) : Reply

    /** The helper could not handle a request; it keeps serving. */
    data class Failure(val message: String) : Reply

    fun encode(): ByteArray = Protocol.encode {
        with(Protocol) {
            when (val r = this@Reply) {
                is Ready -> { writeByte(1); writeLong(r.pid); writeText(r.javaHome) }
                is Result -> {
                    writeByte(2)
                    writeBoolean(r.ok)
                    writeInt(r.diagnostics.size)
                    r.diagnostics.forEach { d ->
                        writeText(d.severity.name); writeInt(d.line); writeInt(d.column); writeInt(d.endLine); writeInt(d.endColumn)
                        writeText(d.source.name); writeText(d.message)
                    }
                    writeBoolean(r.timings != null)
                    r.timings?.let { writeLong(it.compileMs); writeLong(it.guardMs); writeLong(it.loadMs) }
                    writeBoolean(r.error != null)
                    r.error?.let { writeText(it.kind.name); writeText(it.message); writeInt(it.line ?: -1); writeText(it.type) }
                    writeInt(r.png?.size ?: -1)
                    r.png?.let { write(it) }
                    writeDouble(r.renderMillis); writeDouble(r.encodeMillis); writeDouble(r.serverMillis)
                    writeBoolean(r.snapshot != null)
                    r.snapshot?.let { writeSnapshot(it) }
                }
                is Failure -> { writeByte(3); writeText(r.message) }
            }
        }
    }

    companion object {
        fun decode(payload: ByteArray): Reply = Protocol.decode(payload) {
            with(Protocol) {
                when (val type = readByte().toInt()) {
                    1 -> Ready(readLong(), readText())
                    2 -> Result(
                        ok = readBoolean(),
                        diagnostics = List(readInt()) {
                            val severity = Severity.valueOf(readText())
                            val line = readInt(); val column = readInt(); val endLine = readInt(); val endColumn = readInt()
                            val source = Diagnostic.Source.valueOf(readText())
                            Diagnostic(severity, line, column, readText(), endLine, endColumn, source)
                        },
                        timings = if (readBoolean()) Timings(readLong(), readLong(), readLong()) else null,
                        error = if (readBoolean()) {
                            val kind = RuntimeError.Kind.valueOf(readText())
                            val message = readText()
                            RuntimeError(kind, message, readInt().takeIf { it >= 0 }, readText())
                        } else null,
                        png = readInt().takeIf { it >= 0 }?.let { n -> ByteArray(n).also { readFully(it) } },
                        renderMillis = readDouble(), encodeMillis = readDouble(), serverMillis = readDouble(),
                        snapshot = if (readBoolean()) readSnapshot() else null,
                    )
                    3 -> Failure(readText())
                    else -> error("unknown reply type $type")
                }
            }
        }
    }
}
