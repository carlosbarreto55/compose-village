package village.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.text.input.TextFieldValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
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
 * judged against the level's criteria, and the player's progress is saved to [store]. State is
 * Compose state, written on [scope].
 *
 * @param density the window's density; the scene renders at the same pixel density, so frames are sharp.
 */
class SpikeController(private val scope: CoroutineScope, density: Float, private val store: ProgressStore) : AutoCloseable {
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
    /** Whether the latest Run since opening compiled, or null before the first Run. */
    var lastRunCompiled by mutableStateOf<Boolean?>(null)
        private set

    /** What the header's build status says (design D9). */
    val buildStatus: BuildStatus
        get() = when {
            warmingUp -> BuildStatus.WarmingUp
            running -> BuildStatus.Building
            lastRunCompiled == false -> BuildStatus.DidNotBuild(timings)
            lastRunCompiled == true -> timings?.let(BuildStatus::Built) ?: BuildStatus.Ready
            else -> BuildStatus.Ready
        }

    /** Counts every finished Run; keys the checklist's resolve animation so each Run replays it. */
    var runCount by mutableStateOf(0)
        private set

    /** The bundled levels. They are checked by the build's level tests, so a failure here is a packaging bug. */
    val levels: List<Level> = runCatching { LevelRepository().all() }.getOrElse { e -> System.err.println(e.message); emptyList() }

    /** Which screen is shown: the home street, or the open level or sandbox (home-screen D1). */
    var screen by mutableStateOf(Screen.HOME)
        private set

    /** The open level, or null in the free sandbox. */
    var level by mutableStateOf<Level?>(null)
        private set
    /** The latest Run's verdict for the open level. */
    var verdict by mutableStateOf<Verdict?>(null)
        private set

    /** Whether the latest verdict passed every criterion. */
    val complete: Boolean get() = (verdict as? Verdict.Evaluated)?.passed == true

    /** The player's saved progress (village-progress D3): read once, saved after every change. */
    var progress by mutableStateOf(store.load())
        private set

    /** Ids of the completed levels, in this session or an earlier one. */
    val completed: Set<String> get() = progress.completed

    /** Completed levels whose shop is lit: all of them, except the one whose celebration is still on (talking-villagers D3). */
    val litShops: Set<String>
        get() = level?.id?.takeIf { bigDialogue?.kind == BigDialogue.Kind.CELEBRATION }?.let { completed - it } ?: completed

    /**
     * The open level was already completed when it was opened: its saved code is replayed once and the
     * completion card takes its welcome back form, until the player's own Run (village-progress D6).
     */
    var revisiting by mutableStateOf(false)
        private set

    /**
     * The big dialogue over the level screen (talking-villagers D2): the intro on a level's first opening,
     * or the celebration on its first completion. Run and ⌘↵ do nothing while it is open.
     */
    var bigDialogue by mutableStateOf<BigDialogue?>(null)
        private set

    val dialogueOpen: Boolean get() = bigDialogue != null

    /** "Talk to <name>": plays the open level's intro again. Ignored while a run is in progress. */
    fun talk() {
        val level = level ?: return
        if (running || level.intro.isEmpty()) return
        bigDialogue = BigDialogue(BigDialogue.Kind.INTRO, level.intro)
    }

    /** Skip intro, Let's build it! or See <shop>: closes the big dialogue; a closed intro is seen for good. */
    fun closeDialogue() {
        val dialogue = bigDialogue ?: return
        bigDialogue = null
        val level = level ?: return
        if (dialogue.kind == BigDialogue.Kind.INTRO) record(progress.withIntroSeen(level.id))
    }

    /** Whether "Start over" is offered: a level is open and the editor differs from its starter code. */
    val canStartOver: Boolean get() = level?.let { code.text != it.starterCode } ?: false

    /** Runs since the open level was opened: the completion card's Tries. */
    var runsThisVisit by mutableStateOf(0)
        private set

    /** The player chose "Keep tinkering": the completion card stays hidden until the next Run or opening. */
    var dismissedCompletion by mutableStateOf(false)
        private set

    /**
     * Whether the level's sketch is drawn over the preview (design D6): on when a level opens, so it is
     * the ghost before the first Run, and turned off once by the opening's first rendered frame.
     */
    var overlaySketch by mutableStateOf(false)
        private set
    private var overlayTurnsOffOnFirstFrame = false

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
    private var highlightedSince = 0L

    /** The level after the open one in play order, or null on the last level (and in the sandbox). */
    val nextLevel: Level? get() = level?.let { levels.getOrNull(levels.indexOf(it) + 1) }

    /** Whether [panel] is on screen: the sandbox shows every panel, a level only the ones it lists. */
    fun shows(panel: Panel) = level?.let { panel in it.panels } ?: true

    /**
     * Opens [level] (null for the sandbox) with the player's saved code for it, or its starter code. The
     * outgoing level's code is saved first. A completed level replays its code once. Opening what was open
     * before going home just shows it again. Ignored while a run is in progress.
     */
    fun open(level: Level?) {
        if (running) return
        screen = Screen.PLAY
        if (level == this.level) return // back from home to what was open: the visit goes on
        saveDraft()
        this.level = level
        val saved = level?.let { progress[it.id] }
        code = TextFieldValue(saved?.draft ?: level?.starterCode ?: Samples.BAKERY)
        target = level?.let { l -> decodedTargets.getOrPut(l.id) { l.targetPng?.let { Image.makeFromEncoded(it).toComposeImageBitmap() } } }
        resetVisit()
        highlighted = level?.let { l -> unlocks.filterValues { it == l.id }.keys } ?: emptySet()
        highlightedSince = System.nanoTime()
        if (saved?.completed == true && saved.draft != null) {
            revisiting = true
            start(replay = true)
        }
        // A level's intro plays once, unless it was finished in an earlier session (talking-villagers D2).
        bigDialogue = level?.takeIf { it.intro.isNotEmpty() && saved?.completed != true && saved?.introSeen != true }
            ?.let { BigDialogue(BigDialogue.Kind.INTRO, it.intro) }
    }

    /** "Main Street" in the header: shows the home street, saving the open level's code. Ignored while a run is in progress. */
    fun goHome() {
        if (running) return
        saveDraft()
        screen = Screen.HOME
    }

    /** "Start over" and "Play again": the starter code, as if the level had just been opened; completion stays (village-progress D7). */
    fun startOver() {
        val level = level ?: return
        if (running) return
        code = TextFieldValue(level.starterCode)
        resetVisit()
        record(progress.withDraft(level.id, code.text, level.starterCode).withStartOver(level.id))
    }

    /** Forgets this visit's Runs: verdict, build status, preview, and the sketch overlay back on. */
    private fun resetVisit() {
        diagnostics = emptyList()
        timings = null
        lastRunCompiled = null
        verdict = null
        runsThisVisit = 0
        revisiting = false
        dismissedCompletion = false
        showTarget = false
        overlaySketch = target != null
        overlayTurnsOffOnFirstFrame = overlaySketch
        engine.unload()
    }

    /** Saves the editor's code as the open level's draft, if it changed. */
    private fun saveDraft() {
        val level = level ?: return
        record(progress.withDraft(level.id, code.text, level.starterCode))
    }

    private fun record(updated: Progress) {
        if (updated == progress) return
        progress = updated
        store.save(updated)
    }

    /** The player toggled "Overlay sketch"; from now on only they change it in this opening. */
    fun toggleOverlay(on: Boolean) {
        overlaySketch = on
        overlayTurnsOffOnFirstFrame = false
    }

    /** "Keep tinkering": hides the completion card and keeps the level open. */
    fun keepTinkering() {
        dismissedCompletion = true
    }

    /**
     * A key or mouse press anywhere in the window (design D8b). It dismisses unlock tips, but only once
     * their pulse has finished, so the click that opened the level doesn't dismiss its own tip.
     */
    fun noteInteraction() {
        if (highlighted.isNotEmpty() && System.nanoTime() - highlightedSince >= UNLOCK_PULSE_MILLIS * 1_000_000) highlighted = emptySet()
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
    fun run() = start(replay = false)

    /** A Run; a [replay] of a completed level's code is not the player's, so it isn't counted or saved. */
    private fun start(replay: Boolean) {
        val source = code.text
        val level = level
        if (running || (!replay && dialogueOpen)) return
        running = true
        showTarget = false // a Run always shows its result
        if (!replay) {
            highlighted = emptySet()
            revisiting = false
        }
        dismissedCompletion = false
        scope.launch {
            try {
                val result = withContext(compileDispatcher) { pipeline().run(source, level?.entryFunction ?: "PlayerScreen") }
                diagnostics = result.diagnostics
                timings = result.timings
                lastRunCompiled = result.entry != null
                val state = result.entry?.let { engine.load(it, source.lines().size, result.loader, level?.viewport).await() }
                if (state?.frame != null && overlayTurnsOffOnFirstFrame) {
                    overlaySketch = false
                    overlayTurnsOffOnFirstFrame = false
                }
                if (level != null) {
                    val snapshot = if (result.entry != null) engine.snapshot().await() else null
                    verdict = Evaluator.verdict(level, compiled = result.entry != null, snapshot)
                    if (!replay) {
                        runsThisVisit++
                        val met = (verdict as? Verdict.Evaluated)?.results?.count { it.passed } ?: 0
                        if (complete && !progress[level.id].completed) {
                            bigDialogue = BigDialogue(BigDialogue.Kind.CELEBRATION, level.celebration + (level.thanks ?: GameText.THANKS))
                        }
                        record(progress.withDraft(level.id, source, level.starterCode).afterRun(level.id, complete, countCodeLines(source), met))
                    }
                }
            } finally {
                runCount++
                running = false
            }
        }
    }

    private fun pipeline() = pipeline ?: PlayerPipeline().also { pipeline = it }

    override fun close() {
        saveDraft()
        engine.close()
        compileExecutor.shutdownNow()
    }

    companion object {
        /** How long a panel unlock pulses (two 600 ms rings); presses before then don't dismiss its tip. */
        const val UNLOCK_PULSE_MILLIS = 1_200L

        /** A phone-sized preview. */
        const val PREVIEW_WIDTH_DP = 360
        const val PREVIEW_HEIGHT_DP = 640
    }
}

/** The game's two screens (home-screen D1). */
enum class Screen { HOME, PLAY }

/** The header's build status (design D9). */
sealed interface BuildStatus {
    data object WarmingUp : BuildStatus
    data object Ready : BuildStatus
    data object Building : BuildStatus
    data class Built(val timings: Timings) : BuildStatus
    data class DidNotBuild(val timings: Timings?) : BuildStatus
}
