package village.app.ui.theme.cozy

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/** Cozy's brand mark: a little wooden house (32 x 32), in its own colors, so it is drawn untinted. */
internal val CozyBrandMark: ImageVector by lazy {
    val wood = SolidColor(Color(0xFF7A4E2D))
    ImageVector.Builder("brand", 32.dp, 32.dp, 32f, 32f).apply {
        addPath(addPathNodes("M4 14 L16 5 L28 14"), stroke = wood, strokeLineWidth = 2.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        addPath(
            addPathNodes("M9 13 H23 A2 2 0 0 1 25 15 V25 A2 2 0 0 1 23 27 H9 A2 2 0 0 1 7 25 V15 A2 2 0 0 1 9 13 Z"),
            fill = SolidColor(Color(0xFFF7D9A8)), stroke = wood, strokeLineWidth = 2.5f,
        )
        addPath(addPathNodes("M13 19 H19 V27 H13 Z"), fill = wood)
    }.build()
}
