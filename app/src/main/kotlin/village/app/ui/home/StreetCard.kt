package village.app.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import village.app.Building
import village.app.GameText
import village.app.HomeModel
import village.app.ShopState
import village.app.ui.theme.Village

private val Sky = Color(0xFFF6E7CE)
private val Hill = Color(0xFFEDDCBF)
private val Road = Color(0xFFE3CDA9)
private val RoadEdge = Color(0xFFD2B98F)
private val Lead = Color(0xFF4A3B2D)

/**
 * The street card (spec: Street card): sky with two clouds, the title block, one building per level and
 * placeholder, then the Sandbox, on a hill above the road. Buildings wrap rather than scroll.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StreetCard(home: HomeModel, enabled: Boolean, onOpen: (Building) -> Unit, onSandbox: () -> Unit) {
    val c = Village.colors
    Column(Modifier.fillMaxWidth().clip(Village.shapes.card).background(Sky).border(1.dp, c.outline, Village.shapes.card)) {
        Box(Modifier.fillMaxWidth()) {
            Canvas(Modifier.matchParentSize()) { drawClouds() }
            Column(
                Modifier.fillMaxWidth().padding(start = 28.dp, end = 28.dp, top = 40.dp, bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                val caption = if (home.firstLaunch) GameText.HOME_TAGLINE else GameText.STREET
                Text(
                    caption.uppercase(), color = c.accent,
                    style = Village.type.small.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.06.em),
                )
                Text(
                    if (home.firstLaunch) GameText.WELCOME else GameText.WELCOME_BACK, color = c.accentStrong, textAlign = TextAlign.Center,
                    style = Village.type.celebration.copy(fontSize = 48.sp, lineHeight = 53.sp),
                )
                Text(
                    if (home.firstLaunch) GameText.WELCOME_INTRO else home.welcomeLine, color = Lead, textAlign = TextAlign.Center,
                    style = Village.type.request, modifier = Modifier.widthIn(max = 620.dp),
                )
            }
        }
        Box(Modifier.fillMaxWidth()) {
            Canvas(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(120.dp)) { drawHill() }
            FlowRow(
                Modifier.fillMaxWidth().padding(start = 28.dp, end = 28.dp, top = 28.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.Bottom),
            ) {
                for (building in home.buildings) {
                    StreetBuilding(
                        building.shop, building.numbered, building.state,
                        enabled = enabled && building.state != ShopState.COMING_SOON, onClick = { onOpen(building) },
                        modifier = Modifier.align(Alignment.Bottom),
                    ) { ShopFront(building.state, building.villager.profession) }
                }
                StreetBuilding(
                    GameText.SANDBOX, GameText.SANDBOX_PLACE, state = null, enabled = enabled, onClick = onSandbox,
                    modifier = Modifier.align(Alignment.Bottom),
                ) { SandboxFront() }
            }
        }
        RoadStrip()
    }
}

/** A building as a button: drawing, shop name, number and title, status; dashed `accent` around the current one. */
@Composable
private fun StreetBuilding(
    name: String, place: String, state: ShopState?, enabled: Boolean, onClick: () -> Unit,
    modifier: Modifier = Modifier, drawing: @Composable () -> Unit,
) {
    val c = Village.colors
    val current = state == ShopState.START_HERE || state == ShopState.UP_NEXT
    val shape = Village.shapes.sign
    Column(
        modifier.heightIn(min = 44.dp).clip(shape)
            .background(if (current) c.groundRaised else Color.Transparent)
            .drawBehind { if (current) dashedOutline(c.accent) }
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(start = 8.dp, end = 8.dp, top = 10.dp, bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        drawing()
        Text(name, style = Village.type.title.copy(fontSize = 17.sp), color = c.ink)
        Text(place, style = Village.type.caption, color = c.inkMuted)
        Box(Modifier.height(26.dp), contentAlignment = Alignment.Center) { Status(state) }
    }
}

@Composable
private fun Status(state: ShopState?) {
    val c = Village.colors
    val bold = Village.type.caption.copy(fontWeight = FontWeight.Bold)
    when (state) {
        null -> Text(GameText.SANDBOX_STATUS, style = bold, color = c.accentStrong)
        ShopState.OPEN -> Row(verticalAlignment = Alignment.CenterVertically) {
            Canvas(Modifier.size(14.dp)) {
                val s = size.width / 14f
                drawPath(
                    Path().apply { moveTo(3 * s, 7.2f * s); lineTo(6 * s, 10 * s); lineTo(11 * s, 4.5f * s) },
                    c.primary, style = Stroke(2 * s, cap = StrokeCap.Round),
                )
            }
            Text(" " + GameText.STATUS_OPEN, style = bold, color = c.primary)
        }
        ShopState.START_HERE, ShopState.UP_NEXT -> Text(
            if (state == ShopState.START_HERE) GameText.STATUS_START_HERE else GameText.STATUS_UP_NEXT,
            style = bold, color = c.onAccent,
            modifier = Modifier.clip(Village.shapes.chip).background(c.accent).padding(horizontal = 12.dp, vertical = 4.dp),
        )
        ShopState.NOT_YET -> Text(GameText.STATUS_NOT_YET, style = Village.type.caption, color = c.inkMuted)
        ShopState.COMING_SOON -> Text(GameText.STATUS_COMING_SOON, style = Village.type.caption, color = c.inkMuted)
    }
}

/** The 44 dp road: a raised top edge and a dashed centre line. */
@Composable
private fun RoadStrip() {
    Canvas(Modifier.fillMaxWidth().height(44.dp)) {
        drawRect(Road)
        drawLine(RoadEdge, Offset(0f, 1.5.dp.toPx()), Offset(size.width, 1.5.dp.toPx()), 3.dp.toPx())
        val y = 3.dp.toPx() + (size.height - 3.dp.toPx()) / 2
        drawLine(
            Color(0xFFF3EADB).copy(alpha = 0.8f), Offset(28.dp.toPx(), y), Offset(size.width - 28.dp.toPx(), y), 3.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(9.dp.toPx(), 9.dp.toPx())),
        )
    }
}

private fun DrawScope.dashedOutline(color: Color) {
    val w = 2.dp.toPx()
    drawRoundRect(
        color, topLeft = Offset(w / 2, w / 2), size = size.copy(width = size.width - w, height = size.height - w),
        cornerRadius = CornerRadius(17.dp.toPx()),
        style = Stroke(w, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))),
    )
}

/** Two clouds: 120 × 44 at 7% from the left, 38 dp down; 96 × 36 at 9% from the right, 92 dp down. */
private fun DrawScope.drawClouds() {
    val cloud = Color(0xFFFBF4E8)
    val d = 1.dp.toPx()
    val x1 = size.width * 0.07f
    val y1 = 38 * d
    drawPath(Path().apply {
        moveTo(x1 + 14 * d, y1 + 38 * d)
        cubicTo(x1 + 2 * d, y1 + 38 * d, x1 + 2 * d, y1 + 22 * d, x1 + 16 * d, y1 + 22 * d)
        cubicTo(x1 + 18 * d, y1 + 8 * d, x1 + 40 * d, y1 + 6 * d, x1 + 46 * d, y1 + 18 * d)
        cubicTo(x1 + 54 * d, y1 + 6 * d, x1 + 78 * d, y1 + 8 * d, x1 + 80 * d, y1 + 22 * d)
        cubicTo(x1 + 96 * d, y1 + 18 * d, x1 + 104 * d, y1 + 38 * d, x1 + 90 * d, y1 + 38 * d)
        close()
    }, cloud)
    val x2 = size.width * 0.91f - 96 * d
    val y2 = 92 * d
    drawPath(Path().apply {
        moveTo(x2 + 12 * d, y2 + 31 * d)
        cubicTo(x2 + 2 * d, y2 + 31 * d, x2 + 2 * d, y2 + 18 * d, x2 + 14 * d, y2 + 18 * d)
        cubicTo(x2 + 16 * d, y2 + 6 * d, x2 + 34 * d, y2 + 5 * d, x2 + 38 * d, y2 + 15 * d)
        cubicTo(x2 + 44 * d, y2 + 5 * d, x2 + 62 * d, y2 + 7 * d, x2 + 64 * d, y2 + 18 * d)
        cubicTo(x2 + 78 * d, y2 + 15 * d, x2 + 84 * d, y2 + 31 * d, x2 + 72 * d, y2 + 31 * d)
        close()
    }, cloud)
}

/** The hill behind the buildings, the mockup's 1400 × 120 path stretched to the card's width. */
private fun DrawScope.drawHill() {
    val sx = size.width / 1400f
    val sy = size.height / 120f
    drawPath(Path().apply {
        moveTo(0f, 120 * sy); lineTo(0f, 70 * sy)
        cubicTo(180 * sx, 20 * sy, 360 * sx, 30 * sy, 520 * sx, 62 * sy)
        cubicTo(700 * sx, 96 * sy, 860 * sx, 24 * sy, 1040 * sx, 40 * sy)
        cubicTo(1200 * sx, 54 * sy, 1300 * sx, 30 * sy, 1400 * sx, 46 * sy)
        lineTo(1400 * sx, 120 * sy); close()
    }, Hill)
}
