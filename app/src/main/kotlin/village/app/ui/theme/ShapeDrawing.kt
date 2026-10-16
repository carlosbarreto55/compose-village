package village.app.ui.theme

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.translate

/**
 * Draws the theme [shape] of this draw area, grown by [grow] px on every side (negative shrinks) and moved
 * by [offset], so halos, drop shadows and inner rings follow the theme's corners instead of fixed radii.
 * Rounded corners grow by [radiusGrow] (default [grow]); a square theme stays square.
 */
fun DrawScope.drawThemeShape(
    shape: Shape, color: Color, grow: Float = 0f, radiusGrow: Float = grow, offset: Offset = Offset.Zero, style: DrawStyle = Fill,
) {
    val topLeft = Offset(offset.x - grow, offset.y - grow)
    val grown = Size(size.width + 2 * grow, size.height + 2 * grow)
    when (val outline = shape.createOutline(size, layoutDirection, this)) {
        is Outline.Rectangle -> drawRect(color, topLeft, grown, style = style)
        is Outline.Rounded -> {
            val r = outline.roundRect
            fun CornerRadius.grown() = CornerRadius((x + radiusGrow).coerceAtLeast(0f), (y + radiusGrow).coerceAtLeast(0f))
            val uniform = r.topLeftCornerRadius == r.topRightCornerRadius && r.topLeftCornerRadius == r.bottomLeftCornerRadius &&
                r.topLeftCornerRadius == r.bottomRightCornerRadius
            if (uniform) {
                drawRoundRect(color, topLeft, grown, r.topLeftCornerRadius.grown(), style = style)
            } else {
                val rect = RoundRect(
                    topLeft.x, topLeft.y, topLeft.x + grown.width, topLeft.y + grown.height,
                    r.topLeftCornerRadius.grown(), r.topRightCornerRadius.grown(), r.bottomRightCornerRadius.grown(), r.bottomLeftCornerRadius.grown(),
                )
                drawPath(Path().apply { addRoundRect(rect) }, color, style = style)
            }
        }
        // Free-form shapes don't grow; they are drawn as they are, moved by the offset.
        is Outline.Generic -> translate(offset.x, offset.y) { drawPath(outline.path, color, style = style) }
    }
}
