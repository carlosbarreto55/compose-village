package village.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import village.app.ui.theme.Village

/** A parchment card as wide as its column: `surface` fill, 1 dp `outline` border, 22 dp corners. */
@Composable
fun VillageCard(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
    gap: Dp = 12.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = Village.colors
    Column(
        modifier.fillMaxWidth().clip(Village.shapes.card).background(c.surface).border(1.dp, c.outline, Village.shapes.card).padding(padding),
        verticalArrangement = Arrangement.spacedBy(gap),
        content = content,
    )
}

/**
 * The primary button: `primary` fill with a solid `primaryShadow` at the theme's offset (cozy: a 3 dp
 * bottom edge), a display-font label, and an 80 ms press to 0.96 scale (storyboard moment 1).
 */
@Composable
fun PrimaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    height: Dp = 48.dp,
    content: @Composable RowScope.() -> Unit,
) {
    val c = Village.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f, tween(80))
    // The parent's minimum width reaches the face, so a weighted button fills its slot and others wrap their label.
    Box(modifier.scale(scale).alpha(if (enabled) 1f else 0.55f), propagateMinConstraints = true) {
        // The shadow is the face's shape moved by the theme's offset; the face leaves room for it.
        val shadow = Village.shapes.buttonShadow
        Box(Modifier.matchParentSize().padding(start = shadow.x, top = shadow.y).clip(Village.shapes.button).background(c.primaryShadow))
        Row(
            Modifier.padding(end = shadow.x, bottom = shadow.y).height(height).clip(Village.shapes.button).background(c.primary)
                .clickable(interaction, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
                .padding(horizontal = 22.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
            content = content,
        )
    }
}

/** A quiet outlined pill ("Keep tinkering"). */
@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, height: Dp = 48.dp) {
    val c = Village.colors
    Box(
        modifier.height(height).clip(Village.shapes.button).border(1.dp, c.muted, Village.shapes.button)
            .clickable(role = Role.Button, onClick = onClick).padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = Village.type.body, color = c.accentStrong)
    }
}

/** A text-only action with a 44 dp hit area ("Got it", "Sandbox"). */
@Composable
fun TextAction(text: String, onClick: () -> Unit, color: Color = Village.colors.accentStrong, bold: Boolean = false, enabled: Boolean = true) {
    Box(
        Modifier.heightIn(min = 44.dp).widthIn(min = 44.dp).clip(Village.shapes.button)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick).padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        val style = if (bold) Village.type.small.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) else Village.type.small
        Text(text, style = style, color = color.copy(alpha = if (enabled) 1f else 0.5f))
    }
}

/** "New: Row": a concept chip. */
@Composable
fun ConceptChip(text: String) {
    val c = Village.colors
    Text(
        text, style = Village.type.caption, color = c.accentStrong,
        modifier = Modifier.clip(Village.shapes.chip).background(c.chip).padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

/** A requirement's state as the checklist draws it. */
enum class Mark { PENDING, PASSED, FAILED }

/**
 * The checklist's 20 dp status circle: empty when pending, filled `primary` with a check, or filled
 * `error` with a cross. The check's stroke draws in over 180 ms when it appears (storyboard moment 2).
 */
@Composable
fun StatusMark(mark: Mark, fill: Color, size: Dp = 20.dp) {
    val c = Village.colors
    val stroke = remember { Animatable(if (mark == Mark.PENDING) 0f else 1f) }
    LaunchedEffect(mark) {
        if (mark == Mark.PENDING) stroke.snapTo(0f) else { stroke.snapTo(0f); stroke.animateTo(1f, tween(180)) }
    }
    Canvas(Modifier.size(size)) {
        val r = this.size.minDimension / 2
        if (mark == Mark.PENDING) {
            drawCircle(c.muted, r - 1.dp.toPx(), style = Stroke(2.dp.toPx()))
            return@Canvas
        }
        drawCircle(fill, r)
        val s = this.size.width / 20f
        val path = Path().apply {
            if (mark == Mark.PASSED) { moveTo(6f * s, 10.2f * s); lineTo(8.8f * s, 13f * s); lineTo(14f * s, 7.5f * s) }
            else { moveTo(7f * s, 7f * s); lineTo(13f * s, 13f * s); moveTo(13f * s, 7f * s); lineTo(7f * s, 13f * s) }
        }
        val partial = Path()
        PathMeasure().apply { setPath(path, false); getSegment(0f, length * stroke.value, partial, true) }
        drawPath(if (mark == Mark.PASSED) partial else path, if (mark == Mark.PASSED) c.onPrimary else Color.White, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
    }
}

/** A small filled triangle: the Run button's play glyph. */
@Composable
fun PlayGlyph(color: Color = Village.colors.onPrimary) {
    Canvas(Modifier.size(16.dp)) {
        val s = size.width / 16f
        drawPath(Path().apply { moveTo(4f * s, 2.5f * s); lineTo(13f * s, 8f * s); lineTo(4f * s, 13.5f * s); close() }, color)
    }
}

/** A right-pointing chevron, as in the breadcrumb. */
@Composable
fun Chevron(color: Color, size: Dp = 16.dp) {
    Canvas(Modifier.size(size)) {
        val s = this.size.width / 16f
        drawPath(
            Path().apply { moveTo(6f * s, 3f * s); lineTo(11f * s, 8f * s); lineTo(6f * s, 13f * s) },
            color, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round),
        )
    }
}

/** A small round dot, e.g. beside the build status. */
@Composable
fun Dot(color: Color, size: Dp = 8.dp, modifier: Modifier = Modifier) {
    Box(modifier.size(size).clip(Village.shapes.chip).background(color))
}

/** Offsets content horizontally by [dx] dp; used by the failure shake. */
fun Modifier.nudge(dx: Float) = offset(x = dx.dp)
