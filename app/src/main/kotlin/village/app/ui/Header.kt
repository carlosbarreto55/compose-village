package village.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import village.app.BuildStatus
import village.app.GameText
import village.app.SpikeController
import village.app.ui.components.Chevron
import village.app.ui.components.Dot
import village.app.ui.components.PlayGlyph
import village.app.ui.components.PrimaryButton
import village.app.ui.components.TextAction
import village.app.ui.icons.BrandMark
import village.app.ui.theme.Village
import village.app.ui.theme.VillageShapes
import village.levels.Level

/** `2 · Open Sign`: a level's number in play order and its title. */
fun numbered(levels: List<Level>, level: Level) = "${levels.indexOf(level) + 1} · ${level.title}"

/** Brand, breadcrumb with progress dots, build status and Run (spec: Header). */
@Composable
fun Header(controller: SpikeController) {
    val c = Village.colors
    val level = controller.level
    Box(Modifier.fillMaxWidth().background(c.groundRaised).drawBehind {
        drawLine(c.outline, Offset(0f, size.height - 0.5f), Offset(size.width, size.height - 0.5f), 1.dp.toPx())
    }) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = VillageShapes.pagePaddingH, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Image(BrandMark, contentDescription = null, Modifier.size(32.dp))
                Text("Compose Village", style = Village.type.wordmark, color = c.accentStrong, maxLines = 1)
            }
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StreetLink(onClick = controller::goHome, enabled = !controller.running)
                Chevron(c.inkMuted)
                if (level == null) {
                    Text("Sandbox", style = Village.type.body.copy(fontWeight = FontWeight.Bold), color = c.ink)
                } else {
                    Text(
                        numbered(controller.levels, level), style = Village.type.body.copy(fontWeight = FontWeight.Bold), color = c.ink,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false),
                    )
                    ProgressDots(controller)
                }
            }
            BuildStatusLabel(controller.buildStatus)
            val again = level != null && controller.runsThisVisit > 0 && !controller.complete
            PrimaryButton(onClick = controller::run, enabled = !controller.running) {
                PlayGlyph()
                Text(if (again) "Run again" else "Run", style = Village.type.button, color = Color.White, maxLines = 1)
                Text("⌘↵", style = Village.type.codeSmall, color = Color.White.copy(alpha = 0.85f))
            }
        }
        if (controller.running) {
            LinearProgressIndicator(
                Modifier.align(Alignment.BottomStart).fillMaxWidth().height(3.dp),
                color = c.primary, trackColor = c.outline,
            )
        }
    }
}

/** "Main Street" in the breadcrumb: a link back to the home street, underlined on hover (home-screen D7). */
@Composable
private fun StreetLink(onClick: () -> Unit, enabled: Boolean) {
    val c = Village.colors
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Box(
        Modifier.heightIn(min = 44.dp).clip(RoundedCornerShape(8.dp))
            .clickable(interaction, indication = null, enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            GameText.STREET, color = c.inkMuted, maxLines = 1,
            style = Village.type.body.copy(textDecoration = if (hovered && enabled) TextDecoration.Underline else null),
        )
    }
}

/** One dot per level: filled when completed this session, ringed for the open level, muted otherwise. */
@Composable
private fun ProgressDots(controller: SpikeController) {
    val c = Village.colors
    Row(Modifier.padding(start = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        for (l in controller.levels) {
            val current = l == controller.level
            val color = when {
                l.id in controller.litShops -> c.primary
                current -> c.accent
                else -> c.muted
            }
            Box(Modifier.size(16.dp), contentAlignment = Alignment.Center) {
                if (current) Dot(c.currentRing, 16.dp)
                Dot(color, 10.dp)
            }
        }
    }
}

/** "Built in 412 ms" with a dot; hovering shows compile / guard / load (design D9). */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun BuildStatusLabel(status: BuildStatus) {
    val c = Village.colors
    val (text, dot) = when (status) {
        BuildStatus.WarmingUp -> "Compiler warming up…" to c.muted
        BuildStatus.Ready -> "Compiler ready" to c.primary
        BuildStatus.Building -> "Building…" to c.glow
        is BuildStatus.Built -> "Built in ${status.timings.totalMs} ms" to c.primary
        is BuildStatus.DidNotBuild -> "Didn't build" to c.error
    }
    val timings = (status as? BuildStatus.Built)?.timings ?: (status as? BuildStatus.DidNotBuild)?.timings
    TooltipArea(tooltip = {
        if (timings != null) {
            Text(
                "compile ${timings.compileMs} ms · guard ${timings.guardMs} ms · load ${timings.loadMs} ms",
                style = Village.type.caption, color = c.editorText,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(c.editorBg).padding(horizontal = 10.dp, vertical = 6.dp),
            )
        }
    }) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Dot(dot)
            Text(text, style = Village.type.small, color = c.inkMuted, maxLines = 1)
        }
    }
}

/** The street: one shop per level, lit when completed, dashed around the open level; Sandbox at the end. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StreetFooter(controller: SpikeController) {
    val c = Village.colors
    val open = controller.levels.count { it.id in controller.litShops }
    Row(
        Modifier.fillMaxWidth().background(c.groundRaised).drawBehind {
            drawLine(c.outline, Offset(0f, 0.5f), Offset(size.width, 0.5f), 1.dp.toPx())
        }.padding(horizontal = VillageShapes.pagePaddingH, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(GameText.STREET, style = Village.type.small, color = c.inkMuted)
        Row(verticalAlignment = Alignment.CenterVertically) {
            for (level in controller.levels) {
                TooltipArea(tooltip = {
                    Text(
                        numbered(controller.levels, level), style = Village.type.caption, color = c.editorText,
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(c.editorBg).padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }) {
                    Shop(
                        lit = level.id in controller.litShops, current = level == controller.level,
                        enabled = !controller.running, onClick = { controller.open(level) },
                    )
                }
            }
        }
        Text("$open of ${controller.levels.size} shops open", style = Village.type.small, color = c.inkMuted)
        Spacer(Modifier.weight(1f))
        TextAction("Sandbox", onClick = { controller.open(null) }, bold = controller.level == null, enabled = !controller.running)
    }
}

/** A 28 x 22 shop front with a 44 dp hit area. */
@Composable
private fun Shop(lit: Boolean, current: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val c = Village.colors
    val color = animateColorAsState(if (lit) c.glow else c.muted, tween(300)).value
    Box(
        Modifier.size(44.dp).clip(RoundedCornerShape(8.dp)).clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(28.dp, 22.dp)
                .drawBehind {
                    if (current) {
                        val gap = 4.dp.toPx()
                        drawRoundRect(
                            c.accent, topLeft = Offset(-gap, -gap), size = Size(size.width + 2 * gap, size.height + 2 * gap),
                            cornerRadius = CornerRadius(8.dp.toPx()),
                            style = Stroke(2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()))),
                        )
                    }
                }
                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 3.dp, bottomEnd = 3.dp))
                .background(color),
        )
    }
}
