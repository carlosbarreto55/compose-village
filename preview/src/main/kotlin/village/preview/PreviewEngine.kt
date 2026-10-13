package village.preview

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerButtons
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.text.AnnotatedString
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
import kotlin.math.roundToInt

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

/** What a customer action on a node did (customer-bot design D4). */
data class ActionResult(
    /** False when the node isn't in the current tree, or can't take text for enter text: nothing was done. */
    val found: Boolean,
    /** The screen after the action, or null if the attempt crashed (or nothing is loaded). */
    val snapshot: SemanticsSnapshot?,
    /** Everything the attempt printed while the action ran. */
    val printed: String,
    /** Where [printed] starts in the attempt's console output. */
    val consoleStart: Int = 0,
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
    /** Default scene size in pixels, for attempts loaded without a viewport. */
    width: Int = 360,
    height: Int = 640,
    val density: Float = 1f,
    /** A slice running this long has not reached a tick for a while: it is wedged in uninstrumented code. */
    private val hardLimitMillis: Long = 3_000,
    private val playerFile: String = "Player.kt",
    private val frameIntervalMillis: Long = 16,
) : AutoCloseable {
    private val defaultWidth = width
    private val defaultHeight = height

    /** The current scene's size in pixels: the default, or the latest loaded attempt's viewport × [density]. */
    @Volatile var width = width
        private set
    @Volatile var height = height
        private set

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
     * @param viewport the size to render the attempt at, e.g. the level's; null keeps the default size.
     */
    fun load(entry: LevelEntry, playerLines: Int, loader: ClassLoader? = null, viewport: Viewport? = null): CompletableFuture<PreviewState> {
        loader?.let { loaderRefs += WeakReference(it) }
        return session.call { load(entry, playerLines, viewport) }
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

    /**
     * Presses the merged semantics node [nodeId], from the latest [snapshot], like a mouse click at
     * its centre. Completes once the scene has settled, with the screen after it and what it printed.
     */
    fun press(nodeId: Int): CompletableFuture<ActionResult> = act { pressNode(nodeId) }

    /**
     * Replaces the text of node [nodeId] through its set-text semantics action, as typing does, so the
     * player's `onValueChange` runs. Not found when the node has no such action.
     */
    fun enterText(nodeId: Int, text: String): CompletableFuture<ActionResult> = act { setText(nodeId, text) }

    /**
     * Runs [perform] on the player thread, then lets the tasks it posted run (click handlers resume as
     * coroutines on the player thread) before rendering and reading the result.
     */
    private fun act(perform: Session.() -> Boolean): CompletableFuture<ActionResult> {
        val s = session
        return s.call { Console.text().length to perform() }
            .thenCompose { (consoleStart, found) -> settle(s, SETTLE_ROUNDS).thenCompose { s.call { actionResult(consoleStart, found) } } }
    }

    private fun settle(s: Session, rounds: Int): CompletableFuture<Unit> =
        if (rounds == 0) CompletableFuture.completedFuture(Unit) else s.call { }.thenCompose { settle(s, rounds - 1) }

    /** Renders now if the scene has pending changes (e.g. after input), then completes with the state. */
    fun renderNow(): CompletableFuture<PreviewState> = session.call { renderPending(); _state.value }

    /**
     * Renders pending changes, then copies the scene's merged semantics tree (design D2). Completes
     * with null when there is no scene: nothing loaded, or the attempt crashed.
     */
    fun snapshot(): CompletableFuture<SemanticsSnapshot?> = session.call { renderPending(); snapshot() }

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

        fun load(entry: LevelEntry, lines: Int, viewport: Viewport?): PreviewState {
            release()
            Console.clear()
            playerLines = lines
            width = viewport?.let { (it.widthDp * density).roundToInt() } ?: defaultWidth
            height = viewport?.let { (it.heightDp * density).roundToInt() } ?: defaultHeight
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
            val node = mergedNodes().firstOrNull { node -> node.config.getOrNull(SemanticsProperties.Text)?.any { text in it.text } == true }
                ?: return false
            pressAt(node.boundsInRoot.center)
            return true
        }

        private fun pressAt(center: Offset) {
            pointer(PointerEventType.Move, center, Offset.Zero)
            pointer(PointerEventType.Press, center, Offset.Zero)
            pointer(PointerEventType.Release, center, Offset.Zero)
        }

        private fun mergedNodes(): Sequence<SemanticsNode> =
            scene?.semanticsOwners?.asSequence()?.flatMap { owner -> descendants(owner.rootSemanticsNode) }.orEmpty()

        private fun node(id: Int) = mergedNodes().firstOrNull { it.id == id }

        fun pressNode(id: Int): Boolean {
            val node = node(id) ?: return false
            pressAt(node.boundsInRoot.center)
            return true
        }

        fun setText(id: Int, text: String): Boolean {
            val setText = node(id)?.config?.getOrNull(SemanticsActions.SetText)?.action ?: return false
            try {
                setText(AnnotatedString(text))
            } catch (t: Throwable) {
                crash(t)
            }
            return true
        }

        /** After an action: renders what it changed, then the screen and what was printed since [consoleStart]. */
        fun actionResult(consoleStart: Int, found: Boolean): ActionResult {
            try {
                renderPending()
            } catch (t: Throwable) {
                crash(t)
            }
            val console = Console.text()
            val start = consoleStart.coerceAtMost(console.length)
            return ActionResult(found, snapshot(), console.substring(start), start)
        }

        fun snapshot(): SemanticsSnapshot? {
            val s = scene ?: return null
            return try {
                SemanticsSnapshot(Viewport(width / density, height / density), s.semanticsOwners.map { copy(it.rootSemanticsNode) })
            } catch (t: Throwable) { // reading semantics can run player code, e.g. a lazy semantics block
                crash(t)
                null
            }
        }

        private fun copy(node: SemanticsNode): SnapshotNode {
            val config = node.config
            val b = node.boundsInRoot
            return SnapshotNode(
                id = node.id,
                // A text field's content is its editable text; the player sees it like any other text.
                texts = config.getOrNull(SemanticsProperties.Text)?.map { it.text }.orEmpty() +
                    listOfNotNull(config.getOrNull(SemanticsProperties.EditableText)?.text?.takeIf { it.isNotEmpty() }),
                contentDescriptions = config.getOrNull(SemanticsProperties.ContentDescription)?.toList().orEmpty(),
                role = config.getOrNull(SemanticsProperties.Role)?.let(::roleName),
                bounds = DpBounds(b.left / density, b.top / density, b.right / density, b.bottom / density),
                children = node.children.map(::copy),
            )
        }

        // Role.toString() names ValuePicker "Picker"; levels use the constants' names.
        private fun roleName(role: Role): String? = when (role) {
            Role.Button -> "Button"
            Role.Checkbox -> "Checkbox"
            Role.Switch -> "Switch"
            Role.RadioButton -> "RadioButton"
            Role.Tab -> "Tab"
            Role.Image -> "Image"
            Role.DropdownList -> "DropdownList"
            Role.ValuePicker -> "ValuePicker"
            Role.Carousel -> "Carousel"
            else -> null
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

    private companion object {
        /** Round trips through the player thread after an action, so the continuations it posted run first. */
        const val SETTLE_ROUNDS = 4
    }
}
