package village.api

import java.io.OutputStream
import java.io.PrintStream

/**
 * The player's console. Player bytecode is instrumented so that every read of `System.out` /
 * `System.err` (including the ones inlined from `kotlin.io.println`) reads [out] / [err] instead,
 * so player output lands in this per-attempt buffer and never on the host's stdout.
 *
 * Both streams write into one buffer, in order, like a terminal. The buffer is capped at
 * [MAX_BYTES]; output beyond that is dropped and [truncated] is set, so a `println` loop cannot
 * grow the host's heap. The host calls [clear] when a new attempt starts.
 */
object Console {
    const val MAX_BYTES = 64 * 1024

    private val lock = Any()
    private var bytes = ByteArray(4 * 1024)
    private var size = 0

    /** True if output was dropped because the buffer was full. */
    @Volatile var truncated = false
        private set

    // Static fields (not properties) so instrumentation only has to swap the GETSTATIC owner.
    @JvmField val out: PrintStream = PrintStream(Sink, true, Charsets.UTF_8)
    @JvmField val err: PrintStream = PrintStream(Sink, true, Charsets.UTF_8)

    /** Starts a fresh buffer for a new attempt. */
    fun clear() = synchronized(lock) {
        bytes = ByteArray(4 * 1024)
        size = 0
        truncated = false
    }

    /** Everything printed since the last [clear], decoded as UTF-8. */
    fun text(): String = synchronized(lock) { String(bytes, 0, size, Charsets.UTF_8) }

    private fun append(b: ByteArray, off: Int, len: Int) {
        synchronized(lock) {
            val n = minOf(len, MAX_BYTES - size)
            if (n < len) truncated = true
            if (n <= 0) return
            if (size + n > bytes.size) bytes = bytes.copyOf(minOf(MAX_BYTES, maxOf(bytes.size * 2, size + n)))
            System.arraycopy(b, off, bytes, size, n)
            size += n
        }
    }

    private object Sink : OutputStream() {
        override fun write(b: Int) = append(byteArrayOf(b.toByte()), 0, 1)
        override fun write(b: ByteArray, off: Int, len: Int) = append(b, off, len)
    }
}
