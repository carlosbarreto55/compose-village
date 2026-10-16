package village.app.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import village.app.ShopState
import village.app.ui.components.VillagerBadge
import village.app.ui.theme.Village
import village.levels.Profession

/** The size of a building's drawing; the mockup's 150 × 150 viewBox maps 1:1 to dp. */
val BUILDING_SIZE = 150.dp

/** Dark-facade shades from the home mockups, for shops that aren't open. */
private val DarkWall = Color(0xFFEFE3CF)
private val DarkRoof = Color(0xFFB8A58C)
private val DarkLine = Color(0xFF8A6A4E)
private val LitRoof = Color(0xFF9A6440)
private val LockedStripe = Color(0xFFE2D6C2)

/**
 * A level's shop front (home-screen D4): gable roof, sign band, awning stripe in the villager's tint, a
 * window and a door, lit in `glow` when [state] is OPEN, with the villager's badge on the roof.
 */
@Composable
fun ShopFront(state: ShopState, profession: Profession) {
    val c = Village.colors
    val tint = Village.badges.tint(profession)
    val lit = state == ShopState.OPEN
    val locked = state == ShopState.NOT_YET || state == ShopState.COMING_SOON
    Box(Modifier.size(BUILDING_SIZE)) {
        Canvas(Modifier.size(BUILDING_SIZE)) {
            val s = size.width / 150f
            val line = if (lit) c.accent else DarkLine
            if (lit) drawOval(c.glow.copy(alpha = 0.35f), Offset(0f, 42 * s), Size(150 * s, 108 * s))
            box(18, 50, 114, 98, 4, if (lit) c.surface else DarkWall, line, 3f, s)
            val roof = Path().apply { moveTo(8 * s, 54 * s); lineTo(75 * s, 14 * s); lineTo(142 * s, 54 * s); close() }
            drawPath(roof, if (lit) LitRoof else DarkRoof)
            drawPath(roof, if (lit) c.accentStrong else DarkLine, style = Stroke(3 * s, join = StrokeJoin.Round))
            box(40, 58, 70, 18, 4, if (lit) c.glow else c.muted, if (lit) c.accent else null, 2f, s)
            box(26, 82, 98, 12, 3, if (locked) LockedStripe else tint, line, 2f, s)
            box(30, 102, 50, 34, 3, if (lit) c.glow else c.muted, line, 3f, s)
            drawLine(line, Offset(55 * s, 102 * s), Offset(55 * s, 136 * s), 2 * s)
            drawLine(line, Offset(30 * s, 119 * s), Offset(80 * s, 119 * s), 2 * s)
            box(92, 102, 28, 46, 3, line, null, 0f, s)
            if (lit) drawCircle(c.glow, 2 * s, Offset(114 * s, 126 * s))
        }
        Box(Modifier.align(Alignment.TopCenter).offset(y = (-10).dp)) {
            VillagerBadge(profession, size = 40.dp, locked = locked)
        }
    }
}

/** The Sandbox at the end of the street: a flat roof, and code braces glowing in a dark doorway. */
@Composable
fun SandboxFront() {
    val c = Village.colors
    Box(Modifier.size(BUILDING_SIZE)) {
        Canvas(Modifier.size(BUILDING_SIZE)) {
            val s = size.width / 150f
            box(12, 44, 126, 16, 4, LitRoof, c.accentStrong, 3f, s)
            box(22, 58, 106, 90, 4, c.surface, c.accent, 3f, s)
            box(42, 76, 66, 72, 4, c.editorBg, null, 0f, s)
            val brace = Stroke(3 * s, cap = StrokeCap.Round)
            drawPath(Path().apply {
                moveTo(66 * s, 94 * s); quadraticTo(60 * s, 94 * s, 60 * s, 100 * s); lineTo(60 * s, 106 * s)
                quadraticTo(60 * s, 110 * s, 55 * s, 110 * s); quadraticTo(60 * s, 110 * s, 60 * s, 114 * s); lineTo(60 * s, 120 * s)
                quadraticTo(60 * s, 126 * s, 66 * s, 126 * s)
            }, c.syntaxKeyword, style = brace)
            drawPath(Path().apply {
                moveTo(84 * s, 94 * s); quadraticTo(90 * s, 94 * s, 90 * s, 100 * s); lineTo(90 * s, 106 * s)
                quadraticTo(90 * s, 110 * s, 95 * s, 110 * s); quadraticTo(90 * s, 110 * s, 90 * s, 114 * s); lineTo(90 * s, 120 * s)
                quadraticTo(90 * s, 126 * s, 84 * s, 126 * s)
            }, c.syntaxKeyword, style = brace)
        }
        Box(Modifier.align(Alignment.TopCenter).offset(y = 8.dp)) { SandboxBadge() }
    }
}

/** The Sandbox's round badge: `</>` on `chip`, with the badges' 3 dp `accent` ring. */
@Composable
fun SandboxBadge(size: Dp = 40.dp) {
    val c = Village.colors
    Box(Modifier.size(size).clip(CircleShape).background(c.chip).border(3.dp, c.accent, CircleShape), contentAlignment = Alignment.Center) {
        CodeGlyph(c.accentStrong, size * 0.6f)
    }
}

/** A rounded rect in the 150-unit space, filled and optionally stroked, like the mockup's `<rect>`. */
private fun DrawScope.box(x: Int, y: Int, w: Int, h: Int, r: Int, fill: Color, stroke: Color?, width: Float, s: Float) {
    val topLeft = Offset(x * s, y * s)
    val size = Size(w * s, h * s)
    val radius = CornerRadius(r * s)
    drawRoundRect(fill, topLeft, size, radius)
    if (stroke != null) drawRoundRect(stroke, topLeft, size, radius, style = Stroke(width * s))
}
