package village.preview

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerButtons
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.unit.Density
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import village.api.Console
import village.api.Guard
import village.api.LevelEntry
import java.lang.ref.WeakReference
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

/** What the game shows for the player's running attempt. Published from the player thread. */
data class PreviewState(
    /** The latest rendered frame, or null before the first frame / after a crash. */
    val frame: ImageBitmap? = null,
    val frameCount: Int = 0,
    /** Wall time of the latest render in milliseconds (CPU raster + pixel copy). */
    val renderMillis: Double = 0.0,
    val error: RuntimeError? = null,
    /** The attempt's console output so far. */
    val console: String = "",
    /** Composable name -> number of times it ran on the player thread, since the attempt loaded. */
    val recompositions: Map<String, Int> = emptyMap(),
    /** The first trace info string seen for the player's file (format record for design D9). */
    val traceInfoSample: String? = null,
)

/**
 * Runs one player attempt at a time in an isolated offscreen [ImageComposeScene] (design D8).
 *
 * Everything that can run player code (creating the scene, which composes in its constructor;
 * rendering; input; effects) happens on a dedicated player thread inside [Guard] slices. Failures
 * close the scene and become a [RuntimeError] instead of reaching the game. A watchdog restarts
 * the player thread when a slice is wedged in code that never ticks.
 *
 * All methods are safe to call from any thread; they post work to the player thread.
 */
@OptIn(ExperimentalComposeUiApi::class)
class PreviewEngine(
    /** Scene size in pixels. */
    val width: Int = 360,
    val height: Int = 640,
    val density: Float = 1f,
    /** A slice running this long has not reached a tick for a while: it is wedged in uninstrumented code. */
    private val hardLimitMillis: Long = 3_000,
    private val playerFile: String = "Player.kt",
    private val frameIntervalMillis: Long = 16,
) : AutoCloseable {
    private val _state = MutableStateFlow(PreviewState())
    val state: StateFlow<PreviewState> = _state

    /** Weak references to every loaded attempt's class loader, for leak checks (task 4.8). */
    val loaderRefs: MutableList<WeakReference<ClassLoader>> = CopyOnWriteArrayList()

    /** How many times the watchdog replaced a wedged player thread. */
    val restarts: Int get() = restartCount.get()
    private val restartCount = AtomicInteger()
    private val sessionIds = AtomicInteger()

    @Volatile private var closed = false
    @Volatile private var session = Session()
    private val watchdog = Thread(::watch, "preview-watchdog").apply { isDaemon = true; start() }

    init {
        RecompositionTracer.install()
    }

    /**
     * Replaces the running attempt with [entry]: closes the previous scene, creates a new one
     * (composing the entry) and renders the first frame. Completes with the resulting state; a
     * crash during the first composition completes normally with [PreviewState.error] set.
     *
     * @param playerLines line count of the player's file, to ignore stack frames of inlined code.
     * @param loader the attempt's class loader, tracked weakly for leak checks.
     */
    fun load(entry: LevelEntry, playerLines: Int, loader: ClassLoader? = null): CompletableFuture<PreviewState> {
        loader?.let { loaderRefs += WeakReference(it) }
        return session.call { load(entry, playerLines) }
    }

    /** Forwards a pointer event, in scene pixels, to the player's scene. */
    fun sendPointer(type: PointerEventType, position: Offset, scrollDelta: Offset = Offset.Zero) {
        val s = session
        s.post { s.pointer(type, position, scrollDelta) }
    }

    /** Maps a position on a preview pane of [paneWidth] x [paneHeight] pixels to scene pixels. */
    fun toScene(position: Offset, paneWidth: Int, paneHeight: Int) =
        Offset(position.x * width / paneWidth, position.y * height / paneHeight)

    /**
     * Finds the first semantics node whose text contains [text] and clicks its center with a
     * synthetic press and release, exactly like a mouse click. Completes with false if not found.
     */
    fun click(text: String): CompletableFuture<Boolean> = session.call { click(text) }

    /** Renders now if the scene has pending changes (e.g. after input), then completes with the state. */
    fun renderNow(): CompletableFuture<PreviewState> = session.call { renderPending(); _state.value }

    /** Closes the running attempt's scene without loading another; the preview becomes empty. */
    fun unload(): CompletableFuture<Unit> = session.call { release(); publish { PreviewState() } }

    override fun close() {
        closed = true
        val s = session
        s.post { s.release() }
        s.post { s.player.stop() }
    }

    /**
     * One player thread and everything confined to it. When the watchdog gives up on a wedged
     * thread it marks the session dead and starts a new one, so the old thread (if it ever wakes
     * up) can only touch its own dead session, never the new scene or the published state.
     */
    private inner class Session {
        val player = PlayerThread("player-${sessionIds.incrementAndGet()}", frameIntervalMillis, ::onFrame, ::crash)
        private val dispatcher = PlayerDispatcher(player)
        @Volatile var dead = false
        private val pending = CopyOnWriteArrayList<CompletableFuture<*>>()

        private var scene: ImageComposeScene? = null
        private var playerLines = 0

        init {
            player.start()
        }

        fun post(task: () -> Unit) = player.post(Runnable { if (!dead) task() })

        fun <T> call(task: Session.() -> T): CompletableFuture<T> {
            val future = CompletableFuture<T>()
            pending += future
            future.whenComplete { _, _ -> pending -= future }
            player.post(Runnable {
                if (dead) return@Runnable
                try {
                    future.complete(task())
                } catch (t: Throwable) {
                    future.completeExceptionally(t)
                    throw t // the player thread reports it through crash()
                }
            })
            return future
        }

        fun publish(update: (PreviewState) -> PreviewState) {
            if (!dead) _state.update(update)
        }

        // ---- everything below runs on the player thread ----

        fun load(entry: LevelEntry, lines: Int): PreviewState {
            release()
            Console.clear()
            playerLines = lines
            val trace = RecompositionTracer.Target(player.thread, playerFile)
            RecompositionTracer.target = trace
            publish { PreviewState() }
            val handler = CoroutineExceptionHandler { _, t -> post { crash(t) } }
            try {
                // Create the scene empty first: if the first composition throws, we still hold the
                // scene and crash() can close it. A scene that is never closed keeps its Recomposer
                // registered in Compose's global snapshot observers, which leaks the attempt's loader.
                val s = ImageComposeScene(width, height, Density(density), dispatcher + handler)
                scene = s
                s.setContent { entry.content() } // composes right here, inside the current slice
                render()
            } catch (t: Throwable) {
                crash(t)
            }
            return _state.value
        }

        fun render() {
            val s = scene ?: return
            val start = System.nanoTime()
            val image = s.render(start)
            val bitmap = try { image.toComposeImageBitmap() } finally { image.close() }
            val millis = (System.nanoTime() - start) / 1e6
            val trace = RecompositionTracer.target
            publish {
                it.copy(
                    frame = bitmap, frameCount = it.frameCount + 1, renderMillis = millis, console = Console.text(),
                    recompositions = trace?.counts?.toMap().orEmpty(), traceInfoSample = trace?.sampleInfo,
                )
            }
        }

        fun renderPending() {
            repeat(3) { if (scene?.hasInvalidations() == true) render() }
        }

        /** Called by the player thread between tasks; renders only when something changed or animates. */
        fun onFrame() {
            if (dead || scene == null) return
            try {
                Guard.slice { if (scene?.hasInvalidations() == true) render() }
            } catch (t: Throwable) {
                crash(t)
            }
            val console = Console.text()
            if (console != _state.value.console) publish { it.copy(console = console) }
        }

        fun pointer(type: PointerEventType, position: Offset, scrollDelta: Offset) {
            val s = scene ?: return
            val button = if (type == PointerEventType.Press || type == PointerEventType.Release) PointerButton.Primary else null
            try {
                s.sendPointerEvent(
                    eventType = type, position = position, scrollDelta = scrollDelta,
                    buttons = PointerButtons(isPrimaryPressed = type == PointerEventType.Press), button = button,
                )
            } catch (t: Throwable) {
                crash(t)
            }
        }

        fun click(text: String): Boolean {
            val s = scene ?: return false
            val node = s.semanticsOwners.asSequence()
                .flatMap { owner -> descendants(owner.rootSemanticsNode) }
                .firstOrNull { node -> node.config.getOrNull(SemanticsProperties.Text)?.any { text in it.text } == true }
                ?: return false
            val center = node.boundsInRoot.center
            pointer(PointerEventType.Move, center, Offset.Zero)
            pointer(PointerEventType.Press, center, Offset.Zero)
            pointer(PointerEventType.Release, center, Offset.Zero)
            return true
        }

        private fun descendants(node: SemanticsNode): Sequence<SemanticsNode> =
            sequenceOf(node) + node.children.asSequence().flatMap { descendants(it) }

        /** Reports [t] as the attempt's runtime error and closes the broken scene. */
        fun crash(t: Throwable) {
            if (dead) return
            val error = RuntimeError.from(t, playerFile, playerLines)
            // A fresh slice: the abort that may have brought us here is sticky, and closing the
            // scene runs the player's dispose callbacks.
            runCatching { Guard.slice { scene?.close() } }
            scene = null
            publish { it.copy(error = it.error ?: error, console = Console.text()) }
        }

        /** Drops the scene; the entry and its class loader become unreachable with it. */
        fun release() {
            val s = scene ?: return
            scene = null
            runCatching { Guard.slice { s.close() } }
        }

        /** Called by the watchdog thread: give up on this session's wedged player thread. */
        fun kill() {
            dead = true
            Guard.abandon(player.thread)
            player.stop()
            pending.forEach { it.cancel(false) }
        }
    }

    private fun watch() {
        while (!closed) {
            try {
                Thread.sleep(100)
            } catch (_: InterruptedException) {
                return
            }
            val s = session
            val elapsed = Guard.sliceElapsedMillis() ?: continue
            if (elapsed > hardLimitMillis && Guard.sliceOwner() === s.player.thread) restart(s)
        }
    }

    /** Abandons a wedged player thread (it may keep burning CPU) and starts a fresh session. */
    private fun restart(old: Session) {
        old.kill()
        restartCount.incrementAndGet()
        session = Session()
        _state.value = PreviewState(
            error = RuntimeError(
                RuntimeError.Kind.RESTARTED,
                "Your code got stuck for more than ${hardLimitMillis / 1000} s, so the preview was restarted. Run it again after fixing it.",
                line = null, type = "PreviewRestarted",
            ),
            console = Console.text(),
        )
    }
}
