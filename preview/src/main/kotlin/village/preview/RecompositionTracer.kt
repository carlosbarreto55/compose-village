package village.preview

import androidx.compose.runtime.Composer
import androidx.compose.runtime.CompositionTracer
import androidx.compose.runtime.InternalComposeTracingApi
import java.util.concurrent.ConcurrentHashMap

/**
 * Counts how often each player composable runs (design D9), from the trace markers the Compose
 * compiler plugin emits (`traceEventStart(key, dirty1, dirty2, info)`). The tracer is process-wide,
 * so it also sees the game's own UI: only events on the current player thread whose info string
 * names the player's file are counted.
 */
@OptIn(InternalComposeTracingApi::class)
internal object RecompositionTracer : CompositionTracer {
    class Target(val thread: Thread, val file: String) {
        val counts = ConcurrentHashMap<String, Int>()
        /** The first info string seen, recorded for the design's open question on its format. */
        @Volatile var sampleInfo: String? = null
    }

    @Volatile var target: Target? = null
    private var installed = false

    @Synchronized
    fun install() {
        if (installed) return
        Composer.setTracer(this)
        installed = true
    }

    override fun isTraceInProgress(): Boolean = true

    override fun traceEventStart(key: Int, dirty1: Int, dirty2: Int, info: String) {
        val t = target ?: return
        if (Thread.currentThread() !== t.thread || !info.contains(t.file)) return
        if (t.sampleInfo == null) t.sampleInfo = info
        // e.g. "PlayerScreen (Player.kt:12)"; lambdas look like "ComposableSingletons$PlayerKt.lambda-1.<anonymous> (Player.kt:20)"
        t.counts.merge(info.substringBefore(" ("), 1, Int::plus)
    }

    override fun traceEventEnd() {}
}
