package village.levels

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import village.preview.Viewport

/** A playable level: what a villager asks for, the code the player starts from, and how the attempt is judged. */
class Level(
    /** Stable key for player progress; equals the level's folder name. Never changes once released. */
    val id: String,
    /** The level's short name, e.g. "Open Sign": breadcrumb, street and Next button. */
    val title: String,
    /** The villager who asks. */
    val villager: Villager,
    /** The villager's shop, e.g. "Tom's Flowers": "<shop> is open!" when the level is complete. */
    val shop: String,
    /** The villager's request: the task shown to the player. Phrases in double quotes are its key texts. */
    val request: String,
    /** The Compose concepts this level introduces, e.g. `Row`: the "New: Row" chips. */
    val concepts: List<String> = emptyList(),
    /** The villager's line when the level is complete, or null for the game's own thank-you. */
    val thanks: String? = null,
    /** What the villager says when the level first opens (1–4 lines), or none for no intro. */
    val intro: List<String> = emptyList(),
    /** What the villager says on the level's first completion, before [thanks] (up to 3 lines). */
    val celebration: List<String> = emptyList(),
    /** The composable the player must write. */
    val entryFunction: String,
    /** The code the editor starts with. */
    val starterCode: String,
    val viewport: Viewport,
    val criteria: List<Criterion>,
    /** What the customer does after the first frame, in order (customer-actions); none for a still screen. */
    val actions: List<Action> = emptyList(),
    /** Extra panels this level shows; the preview, target, checklist and problems are always shown. */
    val panels: List<Panel> = emptyList(),
    /** The reference solution rendered at build time (PNG at 2x density), or null if none was rendered. */
    val targetPng: ByteArray? = null,
) {
    override fun toString() = "Level($id)"
}

/** Who asks for a level: [id] (e.g. "tom") picks their bust, [name] is shown, and [profession] picks their badge. */
data class Villager(val id: String, val name: String, val profession: Profession)

/** Villager professions; each has a glyph and badge tint in the game. [label] is player-facing. */
enum class Profession(val jsonName: String, val label: String) {
    BAKER("baker", "Baker"),
    FLORIST("florist", "Florist"),
    CHEF("chef", "Chef"),
    TEACHER("teacher", "Teacher"),
    BOAT_CAPTAIN("boat-captain", "Boat captain"),
    FISHMONGER("fishmonger", "Fishmonger"),
    MAIL_CARRIER("mail-carrier", "Mail carrier"),
    MAYOR("mayor", "Mayor");

    companion object {
        fun fromJson(name: String) = entries.firstOrNull { it.jsonName == name }
    }
}

/** Panels a level can opt in to. The first level in play order that lists one unlocks it. */
enum class Panel(val jsonName: String) {
    CONSOLE("console"),
    RECOMPOSITIONS("recompositions");

    companion object {
        fun fromJson(name: String) = entries.firstOrNull { it.jsonName == name }
    }
}

/** Each panel mapped to the id of the first level in [levels] (in play order) that lists it. */
fun panelUnlocks(levels: List<Level>): Map<Panel, String> = buildMap {
    for (level in levels) for (panel in level.panels) putIfAbsent(panel, level.id)
}

/**
 * Matches merged semantics nodes. Every field that is set must match; at least one of [text],
 * [role] and [contentDescription] must be set. Text comparisons trim and collapse whitespace.
 */
@Serializable
data class Selector(
    val text: String? = null,
    /** One of [village.preview.SnapshotNode.ROLE_NAMES]. */
    val role: String? = null,
    val contentDescription: String? = null,
    val ignoreCase: Boolean = false,
    /** Accept a node whose text contains the expected text, instead of equal to it. */
    val contains: Boolean = false,
)

/** One step the customer performs on the player's screen after the first frame. [node] picks its target. */
@Serializable
sealed interface Action {
    /** Named by a criterion's `after` to check the screen right after this step. */
    val id: String
    val node: Selector

    /** Presses the target like a mouse click. */
    @Serializable @SerialName("press")
    data class Press(override val id: String, override val node: Selector) : Action

    /** Replaces the target's text with [text], as typing does. */
    @Serializable @SerialName("enterText")
    data class EnterText(override val id: String, override val node: Selector, val text: String) : Action
}

/**
 * One acceptance check. [description] is the checklist line the player always sees; [message], if
 * set, is an extra line in the villager's voice shown when it fails. [after], if set, is the id of the
 * customer step after which it is checked; otherwise it is checked against the first frame.
 */
@Serializable
sealed interface Criterion {
    val id: String
    val description: String
    val message: String?
    val after: String?

    /** At least one visible node matches [node]; exactly [count] if set. */
    @Serializable @SerialName("exists")
    data class Exists(
        override val id: String, override val description: String, val node: Selector, val count: Int? = null,
        override val message: String? = null, override val after: String? = null,
    ) : Criterion

    /** No visible node matches [node]. */
    @Serializable @SerialName("absent")
    data class Absent(
        override val id: String, override val description: String, val node: Selector, override val message: String? = null,
        override val after: String? = null,
    ) : Criterion

    /** The relative position of a node matching [a] and some other node matching [b]. */
    sealed interface Order : Criterion {
        val a: Selector
        val b: Selector
        val toleranceDp: Float
    }

    /** A's bottom is at or above B's top. */
    @Serializable @SerialName("above")
    data class Above(
        override val id: String, override val description: String, override val a: Selector, override val b: Selector,
        override val toleranceDp: Float = 1f, override val message: String? = null, override val after: String? = null,
    ) : Order

    /** A's top is at or below B's bottom: [Above] with [a] and [b] swapped. */
    @Serializable @SerialName("below")
    data class Below(
        override val id: String, override val description: String, override val a: Selector, override val b: Selector,
        override val toleranceDp: Float = 1f, override val message: String? = null, override val after: String? = null,
    ) : Order

    /** A's right edge is at or left of B's left edge. */
    @Serializable @SerialName("leftOf")
    data class LeftOf(
        override val id: String, override val description: String, override val a: Selector, override val b: Selector,
        override val toleranceDp: Float = 1f, override val message: String? = null, override val after: String? = null,
    ) : Order

    /** A's left edge is at or right of B's right edge: [LeftOf] with [a] and [b] swapped. */
    @Serializable @SerialName("rightOf")
    data class RightOf(
        override val id: String, override val description: String, override val a: Selector, override val b: Selector,
        override val toleranceDp: Float = 1f, override val message: String? = null, override val after: String? = null,
    ) : Order

    /**
     * A line printed during the moment (the [after] step, or while the screen first appeared) matches
     * [text], with the forgiving text rules of [Selector].
     */
    @Serializable @SerialName("prints")
    data class Prints(
        override val id: String, override val description: String, val text: String,
        val ignoreCase: Boolean = false, val contains: Boolean = false,
        override val message: String? = null, override val after: String? = null,
    ) : Criterion

    /**
     * Every one of [checks] passes against this criterion's moment: one checklist row for several
     * checks (main-street-levels D5). Each check is an exists, absent or order criterion with the id
     * `<id>.<n>` and this criterion's description; the loader decodes them from `checks`.
     */
    @Serializable @SerialName("all")
    data class All(
        override val id: String, override val description: String,
        override val message: String? = null, override val after: String? = null,
        @Transient val checks: List<Criterion> = emptyList(),
    ) : Criterion
}

/** The level data cannot be played; [problems] name the field at fault, each on its own. */
class LevelLoadException(val folder: String, val problems: List<String>) :
    Exception("Level '$folder' is invalid:\n" + problems.joinToString("\n") { "  - $it" })
