package village.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.text.input.TextFieldValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import org.jetbrains.skia.Image
import kotlinx.coroutines.withContext
import village.compiler.Diagnostic
import village.compiler.PlayerPipeline
import village.compiler.Timings
import village.levels.Evaluator
import village.levels.Level
import village.levels.LevelRepository
import village.levels.Panel
import village.levels.Verdict
import village.levels.panelUnlocks
import village.preview.PreviewEngine
import java.util.concurrent.Executors
import kotlin.math.roundToInt

/**
 * The spike window's logic: compiles on one background thread (the compiler is not thread-safe)
 * and hands successful attempts to the [PreviewEngine]. With a [level] open, each Run is also
 * judged against the level's criteria. State is Compose state, written on [scope].
 *
 * @param density the window's density; the scene renders at the same pixel density, so frames are sharp.
 */
class SpikeController(private val scope: CoroutineScope, density: Float) : AutoCloseable {
    private val compileExecutor = Executors.newSingleThreadExecutor { r -> Thread(r, "player-compiler").apply { isDaemon = true } }
    private val compileDispatcher = compileExecutor.asCoroutineDispatcher()
    private var pipeline: PlayerPipeline? = null // created and used on the compile thread only

    val engine = PreviewEngine(width = (PREVIEW_WIDTH_DP * density).roundToInt(), height = (PREVIEW_HEIGHT_DP * density).roundToInt(), density = density)

    /** The editor's content. */
    var code by mutableStateOf(TextFieldValue(Samples.BAKERY))

    var warmingUp by mutableStateOf(true)
        private set
    var warmUpMillis by mutableStateOf<Long?>(null)
        private set
    var running by mutableStateOf(false)
        private set
    var diagnostics by mutableStateOf(emptyList<Diagnostic>())
        private set
    var timings by mutableStateOf<Timings?>(null)
        private set

    /** The bundled levels. They are checked by the build's level tests, so a failure here is a packaging bug. */
    val levels: List<Level> = runCatching { LevelRepository().all() }.getOrElse { e -> System.err.println(e.message); emptyList() }

    /** The open level, or null in the free sandbox. */
    var level by mutableStateOf<Level?>(null)
        private set
    /** The latest Run's verdict for the open level. */
    var verdict by mutableStateOf<Verdict?>(null)
        private set

    /** The open level's target, decoded once per level from its build-rendered PNG. */
    var target by mutableStateOf<ImageBitmap?>(null)
        private set
    private val decodedTargets = mutableMapOf<String, ImageBitmap?>()

    /** Whether the preview shows the level's target ("Target") instead of the player's screen ("Your screen"). */
    var showTarget by mutableStateOf(false)

    /** Each panel and the level that unlocks it: the first in play order that lists it. */
    val unlocks: Map<Panel, String> = panelUnlocks(levels)

    /** Panels being highlighted because the open level unlocks them. */
    var highlighted by mutableStateOf(emptySet<Panel>())
        private set
    private var highlightJob: Job? = null

    /** The level after the open one in play order, or null on the last level (and in the sandbox). */
    val nextLevel: Level? get() = level?.let { levels.getOrNull(levels.indexOf(it) + 1) }

    /** Whether [panel] is on screen: the sandbox shows every panel, a level only the ones it lists. */
    fun shows(panel: Panel) = level?.let { panel in it.panels } ?: true

    /** Opens [level] (null for the sandbox): its starter code replaces the editor's content. Ignored while a run is in progress. */
    fun open(level: Level?) {
        if (running || level == this.level) return
        this.level = level
        code = TextFieldValue(level?.starterCode ?: Samples.BAKERY)
        diagnostics = emptyList()
        timings = null
        verdict = null
        showTarget = false
        target = level?.let { l -> decodedTargets.getOrPut(l.id) { l.targetPng?.let { Image.makeFromEncoded(it).toComposeImageBitmap() } } }
        engine.unload()
        highlightJob?.cancel()
        highlighted = level?.let { l -> unlocks.filterValues { it == l.id }.keys } ?: emptySet()
        if (highlighted.isNotEmpty()) highlightJob = scope.launch { delay(HIGHLIGHT_MILLIS); highlighted = emptySet() }
    }

    /** Opens the next level; used by the "Level complete" state. */
    fun continueToNext() {
        nextLevel?.let(::open)
    }

    /** The player dismissed [panel]'s unlock explanation. */
    fun dismissHighlight(panel: Panel) {
        highlighted = highlighted - panel
    }

    /** Compiles the sample once in the background so the player's first Run is already warm. */
    fun warmUp() {
        scope.launch {
            val start = System.nanoTime()
            withContext(compileDispatcher) { pipeline().run(Samples.BAKERY) }
            warmUpMillis = (System.nanoTime() - start) / 1_000_000
            warmingUp = false
        }
    }

    /** Compiles the editor's code and, if it compiles, shows it in the preview. Ignored while a run is in progress. */
    fun run() {
        val source = code.text
        val level = level
        if (running) return
        running = true
        showTarget = false // a Run always shows its result
        scope.launch {
            try {
                val result = withContext(compileDispatcher) { pipeline().run(source, level?.entryFunction ?: "PlayerScreen") }
                diagnostics = result.diagnostics
                timings = result.timings
                result.entry?.let { engine.load(it, source.lines().size, result.loader, level?.viewport).await() }
                if (level != null) {
                    val snapshot = if (result.entry != null) engine.snapshot().await() else null
                    verdict = Evaluator.verdict(level, compiled = result.entry != null, snapshot)
                }
            } finally {
                running = false
            }
        }
    }

    private fun pipeline() = pipeline ?: PlayerPipeline().also { pipeline = it }

    override fun close() {
        engine.close()
        compileExecutor.shutdownNow()
    }

    companion object {
        /** How long an unlocked panel stays highlighted unless dismissed. */
        const val HIGHLIGHT_MILLIS = 8_000L

        /** A phone-sized preview. */
        const val PREVIEW_WIDTH_DP = 360
        const val PREVIEW_HEIGHT_DP = 640
    }
}
