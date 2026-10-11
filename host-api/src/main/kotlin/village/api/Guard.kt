package village.api

/**
 * Runtime half of the sandbox. Player bytecode is instrumented so that every method entry and
 * every backward jump (loop) calls [tick]. All player code runs inside a "slice" opened by the
 * host on the player thread; a slice has a wall-clock budget and an allocation budget. When a
 * budget is exceeded, or the host calls [abort], [tick] throws [PlayerAbort].
 *
 * The abort is sticky until the next [beginSlice] so a player `catch (t: Throwable)` inside a
 * loop cannot swallow it: the very next tick throws again.
 *
 * [slice] nests: an inner slice gets a fresh budget and a cleared abort (the host uses this to
 * close a crashed scene), and when it ends the outer slice's budget resumes from its own start.
 * [beginSlice]/[endSlice] are the non-nesting primitives.
 */
object Guard {
    private val threadMx = java.lang.management.ManagementFactory.getThreadMXBean()
        as com.sun.management.ThreadMXBean
    private val measuresAllocation = threadMx.isThreadAllocatedMemorySupported && threadMx.isThreadAllocatedMemoryEnabled

    @Volatile private var abort: PlayerAbort? = null
    private val abandoned: MutableSet<Thread> = java.util.concurrent.ConcurrentHashMap.newKeySet()
    @Volatile private var sliceThread: Thread? = null
    @Volatile private var sliceStartNanos = 0L
    private var sliceStartAlloc = 0L
    private var counter = 0

    @Volatile var timeBudgetMillis: Long = 1_000
    @Volatile var allocBudgetBytes: Long = 512L * 1024 * 1024

    fun beginSlice() {
        abort = null
        counter = 0
        sliceStartAlloc = if (measuresAllocation) threadMx.currentThreadAllocatedBytes else 0
        sliceStartNanos = System.nanoTime()
        sliceThread = Thread.currentThread()
    }

    fun endSlice() {
        sliceThread = null
    }

    /** Runs [block] as one slice on the calling thread; see the class comment for nesting. */
    inline fun <T> slice(block: () -> T): T {
        val outer = currentSlice()
        beginSlice()
        try {
            return block()
        } finally {
            if (outer != null) restoreSlice(outer) else endSlice()
        }
    }

    @PublishedApi internal class SavedSlice(val thread: Thread, val startNanos: Long, val startAlloc: Long)

    /** The slice running on the calling thread, if any. */
    @PublishedApi internal fun currentSlice(): SavedSlice? =
        sliceThread?.takeIf { it === Thread.currentThread() }?.let { SavedSlice(it, sliceStartNanos, sliceStartAlloc) }

    @PublishedApi internal fun restoreSlice(saved: SavedSlice) {
        sliceStartAlloc = saved.startAlloc
        sliceStartNanos = saved.startNanos
        sliceThread = saved.thread
    }

    /**
     * How long the current slice has been running, or null outside a slice. Read by the host's
     * hard watchdog, which must catch code wedged where no tick runs (uninstrumented library code).
     */
    fun sliceElapsedMillis(): Long? = if (sliceThread == null) null else (System.nanoTime() - sliceStartNanos) / 1_000_000

    /** The thread running the current slice, or null outside a slice. */
    fun sliceOwner(): Thread? = sliceThread

    /**
     * Gives up on [thread], wedged in code that never ticks (the host's hard watchdog then starts
     * a new player thread). Ends its slice and aborts it; if it ever reaches player code again,
     * its next budget check throws, even after a new slice has cleared the sticky abort.
     */
    fun abandon(thread: Thread) {
        abandoned += thread
        abort = PlayerAbort("The preview was restarted.", PlayerAbort.Kind.HOST)
        if (sliceThread === thread) sliceThread = null
    }

    /** Called from another thread (watchdog/UI) to stop the player's code at its next tick. */
    fun abort(reason: String) {
        abort = PlayerAbort(reason, PlayerAbort.Kind.HOST)
    }

    @JvmStatic
    fun tick() {
        abort?.let { throw it }
        // Cheap path: only check the budgets every 1024 ticks, and only on the slice thread.
        if (++counter and 1023 != 0) return
        val current = Thread.currentThread()
        if (current in abandoned) throw PlayerAbort("The preview was restarted.", PlayerAbort.Kind.HOST)
        val t = sliceThread ?: return
        if (t !== current) return
        val elapsedMs = (System.nanoTime() - sliceStartNanos) / 1_000_000
        if (elapsedMs > timeBudgetMillis) {
            fail("Your code ran for more than $timeBudgetMillis ms without finishing. Is there an infinite loop?", PlayerAbort.Kind.TIME)
        }
        if (measuresAllocation) {
            val allocated = threadMx.currentThreadAllocatedBytes - sliceStartAlloc
            if (allocated > allocBudgetBytes) {
                fail("Your code allocated more than ${allocBudgetBytes / (1024 * 1024)} MB in one go. Is a list growing forever?", PlayerAbort.Kind.ALLOCATION)
            }
        }
    }

    private fun fail(message: String, kind: PlayerAbort.Kind): Nothing {
        val e = PlayerAbort(message, kind)
        abort = e
        throw e
    }
}

/** An Error (not Exception) so ordinary `catch (e: Exception)` in player code does not swallow it. */
class PlayerAbort(message: String, val kind: Kind) : Error(message) {
    enum class Kind { TIME, ALLOCATION, HOST }
}
