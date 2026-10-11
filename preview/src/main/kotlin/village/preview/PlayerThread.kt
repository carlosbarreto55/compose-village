package village.preview

import kotlinx.coroutines.CoroutineDispatcher
import village.api.Guard
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext

/**
 * The one thread player code runs on (design D7/D8). Every posted task runs inside its own
 * [Guard] slice. Between tasks, and at least every [frameIntervalMillis] when idle, the thread
 * calls [onFrame], which is where the engine renders.
 */
internal class PlayerThread(
    name: String,
    private val frameIntervalMillis: Long,
    private val onFrame: () -> Unit,
    private val onUncaught: (Throwable) -> Unit,
) {
    private val queue = LinkedBlockingQueue<Runnable>()
    @Volatile private var stopped = false
    val thread: Thread = Thread(::loop, name).apply { isDaemon = true }

    fun start() = thread.start()

    fun post(task: Runnable) {
        if (!stopped) queue.add(task)
    }

    /** Stops after the current task; tasks still queued are dropped. */
    fun stop() {
        stopped = true
        queue.add(Runnable {}) // wake the loop
    }

    private fun loop() {
        var lastFrame = 0L
        while (!stopped) {
            val task = queue.poll(frameIntervalMillis, TimeUnit.MILLISECONDS)
            if (stopped) break
            if (task != null) {
                try {
                    Guard.slice { task.run() }
                } catch (t: Throwable) {
                    onUncaught(t)
                }
            }
            val now = System.nanoTime()
            if (now - lastFrame >= frameIntervalMillis * 1_000_000) {
                lastFrame = now
                onFrame()
            }
        }
    }
}

/** Runs coroutines on the player thread; every dispatched continuation is its own guarded slice. */
internal class PlayerDispatcher(private val player: PlayerThread) : CoroutineDispatcher() {
    override fun dispatch(context: CoroutineContext, block: Runnable) = player.post(block)
}
