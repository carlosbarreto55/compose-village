package village.api

/**
 * Runtime half of the sandbox. Player bytecode is instrumented so that every method entry and
 * every backward jump (loop) calls [tick]. All player code runs inside a "slice" opened by the
 * host on the player thread; a slice has a wall-clock budget and an allocation budget. When a
 * budget is exceeded, or the host calls [abort], [tick] throws [PlayerAbort].
 *
 * The abort is sticky until the next [beginSlice] so a player `catch (t: Throwable)` inside a
 * loop cannot swallow it: the very next tick throws again.
 */
object Guard {
    private val threadMx = java.lang.management.ManagementFactory.getThreadMXBean()
        as com.sun.management.ThreadMXBean

    @Volatile private var abortReason: String? = null
    @Volatile private var sliceThread: Thread? = null
    private var sliceStartNanos = 0L
    private var sliceStartAlloc = 0L
    private var counter = 0

    @Volatile var timeBudgetMillis: Long = 1_000
    @Volatile var allocBudgetBytes: Long = 512L * 1024 * 1024

    fun beginSlice() {
        abortReason = null
        sliceThread = Thread.currentThread()
        sliceStartNanos = System.nanoTime()
        sliceStartAlloc = threadMx.getThreadAllocatedBytes(Thread.currentThread().id)
    }

    fun endSlice() {
        sliceThread = null
    }

    /** Called from another thread (watchdog/UI) to stop the player's code at its next tick. */
    fun abort(reason: String) {
        abortReason = reason
    }

    @JvmStatic
    fun tick() {
        abortReason?.let { throw PlayerAbort(it) }
        // Cheap path: only check the clock every 1024 ticks, and only on the slice thread.
        if (++counter and 1023 != 0) return
        val t = sliceThread ?: return
        if (t !== Thread.currentThread()) return
        val elapsedMs = (System.nanoTime() - sliceStartNanos) / 1_000_000
        if (elapsedMs > timeBudgetMillis) {
            abortReason = "Your code ran for more than ${timeBudgetMillis} ms without finishing. Is there an infinite loop?"
            throw PlayerAbort(abortReason!!)
        }
        val allocated = threadMx.getThreadAllocatedBytes(t.id) - sliceStartAlloc
        if (allocated > allocBudgetBytes) {
            abortReason = "Your code allocated more than ${allocBudgetBytes / (1024 * 1024)} MB in one go. Is a list growing forever?"
            throw PlayerAbort(abortReason!!)
        }
    }
}

/** An Error (not Exception) so ordinary `catch (e: Exception)` in player code does not swallow it. */
class PlayerAbort(message: String) : Error(message)
