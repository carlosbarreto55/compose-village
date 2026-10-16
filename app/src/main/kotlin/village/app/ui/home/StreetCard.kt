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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
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
import village.app.ui.theme.drawThemeShape


/**
 * The street card (spec: Street card): the theme's scenery (sky, ground, road, buildings) around the title
 * block, one building per level and placeholder, then the Sandbox. Buildings wrap rather than scroll.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StreetCard(home: HomeModel, enabled: Boolean, onOpen: (Building) -> Unit, onSandbox: () -> Unit) {
    val c = Village.colors
    val scenery = Village.scenery
    Column(Modifier.fillMaxWidth().clip(Village.shapes.card).background(scenery.sky).border(1.dp, c.outline, Village.shapes.card)) {
        Box(Modifier.fillMaxWidth()) {
            Canvas(Modifier.matchParentSize()) { with(scenery) { drawSky() } }
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
                    if (home.firstLaunch) GameText.WELCOME_INTRO else home.welcomeLine, color = scenery.onSky, textAlign = TextAlign.Center,
                    style = Village.type.request, modifier = Modifier.widthIn(max = 620.dp),
                )
            }
        }
        Box(Modifier.fillMaxWidth()) {
            Canvas(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(120.dp)) { with(scenery) { drawGround() } }
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
                    ) { scenery.ShopFront(building.state, building.villager.profession) }
                }
                StreetBuilding(
                    GameText.SANDBOX, GameText.SANDBOX_PLACE, state = null, enabled = enabled, onClick = onSandbox,
                    modifier = Modifier.align(Alignment.Bottom),
                ) { scenery.SandboxFront() }
            }
        }
        Canvas(Modifier.fillMaxWidth().height(44.dp)) { with(scenery) { drawRoad() } }
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
            .drawBehind { if (current) dashedOutline(shape, c.accent) }
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


/** A dashed line just inside [shape]'s edge. */
private fun DrawScope.dashedOutline(shape: Shape, color: Color) {
    val w = 2.dp.toPx()
    drawThemeShape(shape, color, grow = -w / 2, style = Stroke(w, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))))
}



