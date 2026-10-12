package village.levels

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import village.preview.Viewport

/** A playable level: what a villager asks for, the code the player starts from, and how the attempt is judged. */
class Level(
    /** Stable key for player progress; equals the level's folder name. Never changes once released. */
    val id: String,
    /** The villager who asks, e.g. "Rosa, the baker". */
    val resident: String,
    /** The villager's request: the task shown to the player. */
    val request: String,
    /** The composable the player must write. */
    val entryFunction: String,
    /** The code the editor starts with. */
    val starterCode: String,
    val viewport: Viewport,
    val criteria: List<Criterion>,
) {
    override fun toString() = "Level($id)"
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

/** One acceptance check. [message] is shown to the player, in the resident's voice, when it fails. */
@Serializable
sealed interface Criterion {
    val id: String
    val message: String

    /** At least one visible node matches [node]; exactly [count] if set. */
    @Serializable @SerialName("exists")
    data class Exists(override val id: String, override val message: String, val node: Selector, val count: Int? = null) : Criterion

    /** No visible node matches [node]. */
    @Serializable @SerialName("absent")
    data class Absent(override val id: String, override val message: String, val node: Selector) : Criterion

    /** Some node matching [a] has its bottom at or above the top of some other node matching [b]. */
    @Serializable @SerialName("above")
    data class Above(
        override val id: String, override val message: String, val a: Selector, val b: Selector, val toleranceDp: Float = 1f,
    ) : Criterion

    /** Some node matching [a] has its right edge at or left of the left edge of some other node matching [b]. */
    @Serializable @SerialName("leftOf")
    data class LeftOf(
        override val id: String, override val message: String, val a: Selector, val b: Selector, val toleranceDp: Float = 1f,
    ) : Criterion
}

/** The level data cannot be played; [problems] name the field at fault, each on its own. */
class LevelLoadException(val folder: String, val problems: List<String>) :
    Exception("Level '$folder' is invalid:\n" + problems.joinToString("\n") { "  - $it" })
