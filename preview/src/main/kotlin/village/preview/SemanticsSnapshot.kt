package village.preview

/** A size in dp: a level's viewport, and the size of the scene a snapshot was taken from. */
data class Viewport(val widthDp: Float, val heightDp: Float) {
    init {
        require(widthDp > 0 && heightDp > 0) { "viewport must be positive, was $widthDp x $heightDp dp" }
    }
}

/** A rectangle in dp, in scene coordinates (origin at the top left of the viewport). */
data class DpBounds(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val width get() = right - left
    val height get() = bottom - top
}

/**
 * A copy of the attempt's **merged** semantics tree (design D2): what acceptance criteria are
 * checked against. Plain immutable data with no Compose types and no reference to the player's
 * classes, so it outlives the attempt and can cross the helper-process boundary.
 */
data class SemanticsSnapshot(
    val viewport: Viewport,
    /** One root per semantics owner of the scene: the content, plus any popups or dialogs. */
    val roots: List<SnapshotNode>,
) {
    /** Every node, depth first, parents before children. */
    fun nodes(): Sequence<SnapshotNode> = roots.asSequence().flatMap { it.selfAndDescendants() }
}

data class SnapshotNode(
    /** `SemanticsProperties.Text`: several values when the node merged several texts. */
    val texts: List<String> = emptyList(),
    val contentDescriptions: List<String> = emptyList(),
    /** The semantics role's name (`Button`, `Checkbox`, ...), or null if the node has none. */
    val role: String? = null,
    val bounds: DpBounds,
    val children: List<SnapshotNode> = emptyList(),
) {
    fun selfAndDescendants(): Sequence<SnapshotNode> = sequenceOf(this) + children.asSequence().flatMap { it.selfAndDescendants() }

    companion object {
        /** The names [role] can take, after Compose's `Role` constants. */
        val ROLE_NAMES = listOf("Button", "Checkbox", "Switch", "RadioButton", "Tab", "Image", "DropdownList", "ValuePicker", "Carousel")
    }
}
