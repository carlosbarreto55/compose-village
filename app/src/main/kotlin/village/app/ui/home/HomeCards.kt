package village.app.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import village.app.GameText
import village.app.OpenShop
import village.app.ui.components.Chevron
import village.app.ui.components.ConceptChip
import village.app.ui.components.PrimaryButton
import village.app.ui.components.ProfessionPill
import village.app.ui.components.SecondaryButton
import village.app.ui.components.VillagerBadge
import village.app.ui.components.icon
import village.app.ui.requestText
import village.app.ui.theme.Village
import village.app.ui.villagers.VillagerPortrait
import village.levels.Level

/** First launch: the first level's villager, their request and chips, and Start (spec: First-launch cards). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FirstVillagerCard(level: Level, numbered: String, onStart: () -> Unit, modifier: Modifier = Modifier) {
    val c = Village.colors
    HomeCard(modifier, gap = 18.dp) {
        Caption(GameText.FIRST_VILLAGER)
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            VillagerPortrait(level.villager, 88.dp)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(level.villager.name, style = Village.type.name.copy(fontSize = 22.sp), color = c.ink)
                    ProfessionPill(level.villager.profession)
                }
                Text(requestText(level.request), style = Village.type.request, color = c.ink)
                if (level.concepts.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (concept in level.concepts) ConceptChip("New: $concept")
                    }
                }
            }
        }
        ActionRow {
            BigButton("Start: $numbered", onStart)
            Text(GameText.STARTER_READY_FIRST, style = Village.type.body, color = c.inkMuted, modifier = Modifier.align(Alignment.CenterVertically))
        }
    }
}

/** Welcome back: the current level, whether its code is saved, its wishes met, and Continue (spec: Welcome back cards). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PickUpCard(level: Level, numbered: String, saved: Boolean, met: Int, onContinue: () -> Unit, modifier: Modifier = Modifier) {
    val c = Village.colors
    HomeCard(modifier, gap = 18.dp) {
        Caption(GameText.PICK_UP)
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            VillagerPortrait(level.villager, 88.dp)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(numbered, style = Village.type.name.copy(fontSize = 22.sp), color = c.ink, modifier = Modifier.align(Alignment.CenterVertically))
                    VillagerPill(level, Modifier.align(Alignment.CenterVertically))
                }
                Text(requestText(level.request), style = Village.type.request, color = c.ink)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Note(if (saved) GameText.CODE_SAVED else GameText.STARTER_READY, saved = true)
                    Note(GameText.wishes(met, level.criteria.size), saved = false)
                }
            }
        }
        ActionRow {
            BigButton("Continue: ${level.title}", onContinue)
            Text(GameText.OR_PICK_ANY, style = Village.type.body, color = c.inkMuted, modifier = Modifier.align(Alignment.CenterVertically))
        }
    }
}

/** Welcome back with every bundled shop open: no current level, so the Sandbox is the way forward. */
@Composable
fun AllOpenCard(onSandbox: () -> Unit, modifier: Modifier = Modifier) {
    val c = Village.colors
    HomeCard(modifier, gap = 14.dp) {
        Caption(GameText.PICK_UP)
        Text(GameText.ALL_OPEN_TITLE, style = Village.type.name.copy(fontSize = 22.sp), color = c.ink)
        Text(GameText.ALL_OPEN_BODY, style = Village.type.request, color = c.ink)
        ActionRow { BigButton(GameText.OPEN_THE_SANDBOX, onSandbox) }
    }
}

/** The three steps of a level, then a way into the Sandbox (spec: First-launch cards). */
@Composable
fun HowALevelWorksCard(onSandbox: () -> Unit, modifier: Modifier = Modifier) {
    val c = Village.colors
    HomeCard(modifier, gap = 16.dp) {
        Text(GameText.HOW_A_LEVEL_WORKS, style = Village.type.name, color = c.ink)
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            GameText.HOW_STEPS.forEachIndexed { i, (title, text) ->
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(Modifier.size(32.dp).clip(Village.shapes.chip).background(c.chip), contentAlignment = Alignment.Center) {
                        Text("${i + 1}", style = Village.type.title.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold), color = c.accentStrong)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(title, style = Village.type.body.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold), color = c.ink)
                            if (i == GameText.HOW_STEPS.lastIndex) Text("⌘↵", style = Village.type.codeSmall, color = c.inkMuted)
                        }
                        Text(text, style = Village.type.body, color = c.inkMuted)
                    }
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(c.outline))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(GameText.JUST_EXPERIMENT, style = Village.type.body, color = c.inkMuted, modifier = Modifier.weight(1f))
            Row(
                Modifier.height(44.dp).clip(Village.shapes.button).border(2.dp, c.accent, Village.shapes.button)
                    .clickable(role = Role.Button, onClick = onSandbox).padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CodeGlyph(c.accentStrong, 18.dp)
                Text(GameText.OPEN_THE_SANDBOX, style = Village.type.title.copy(fontSize = 16.sp), color = c.accentStrong)
            }
        }
    }
}

/** Every completed level with its saved stats and Visit (spec: Welcome back cards). */
@Composable
fun OpenShopsCard(shops: List<OpenShop>, onVisit: (Level) -> Unit, modifier: Modifier = Modifier) {
    val c = Village.colors
    Column(
        modifier.fillMaxWidth().clip(Village.shapes.card).background(c.celebration).border(2.dp, c.glow, Village.shapes.card)
            .padding(horizontal = 22.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(GameText.OPEN_SHOPS, style = Village.type.name, color = c.ink)
        for (shop in shops) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                VillagerBadge(shop.level.villager.profession, size = 44.dp)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(shop.level.shop, style = Village.type.body.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold), color = c.ink)
                    Text(GameText.shopStats(shop.lines, shop.tries), style = Village.type.small, color = c.inkMuted)
                }
                SecondaryButton(GameText.VISIT, onClick = { onVisit(shop.level) }, height = 44.dp)
            }
        }
    }
}

/** The dark Sandbox card with Open. */
@Composable
fun SandboxCard(onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val c = Village.colors
    Row(
        modifier.fillMaxWidth().clip(Village.shapes.editor).background(c.editorBg).padding(horizontal = 22.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.size(44.dp).clip(Village.shapes.tile).background(c.editorRaised), contentAlignment = Alignment.Center) {
            CodeGlyph(c.syntaxKeyword, 24.dp)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(GameText.SANDBOX, style = Village.type.body.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold), color = c.editorText)
            Text(GameText.SANDBOX_BLURB, style = Village.type.small, color = c.editorMuted)
        }
        Box(
            Modifier.height(44.dp).clip(Village.shapes.button).border(1.dp, c.editorOutline, Village.shapes.button)
                .clickable(role = Role.Button, onClick = onOpen).padding(horizontal = 18.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(GameText.OPEN, style = Village.type.body, color = c.editorText)
        }
    }
}

/** A home card: `surface` fill, 1 dp `outline` border, 22 dp corners, 24 × 26 dp padding. */
@Composable
private fun HomeCard(modifier: Modifier, gap: Dp, content: @Composable ColumnScope.() -> Unit) {
    val c = Village.colors
    Column(
        modifier.fillMaxWidth().clip(Village.shapes.card).background(c.surface).border(1.dp, c.outline, Village.shapes.card)
            .padding(horizontal = 26.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(gap),
        content = content,
    )
}

/** "YOUR FIRST VILLAGER": a card's small spaced caption. */
@Composable
private fun Caption(text: String) {
    Text(text.uppercase(), style = Village.type.small.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.06.em), color = Village.colors.accent)
}

/** The primary action and its note, wrapping under each other when narrow. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActionRow(content: @Composable FlowRowScope.() -> Unit) {
    FlowRow(
        Modifier.padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content,
    )
}

/** The home's 52 dp primary button with a chevron: Start and Continue. */
@Composable
private fun BigButton(text: String, onClick: () -> Unit) {
    PrimaryButton(onClick = onClick, height = 52.dp) {
        Text(text, style = Village.type.button.copy(fontSize = 19.sp), color = Village.colors.onPrimary, maxLines = 1)
        Chevron(Village.colors.onPrimary, 18.dp)
    }
}

/** "Tom, florist" in the villager's tint, with their glyph. */
@Composable
private fun VillagerPill(level: Level, modifier: Modifier = Modifier) {
    val b = Village.badges
    val profession = level.villager.profession
    Row(
        modifier.clip(Village.shapes.chip).background(b.tint(profession)).padding(start = 4.dp, end = 10.dp, top = 3.dp, bottom = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(profession.icon, null, Modifier.size(20.dp), tint = b.glyph)
        Text("${level.villager.name}, ${profession.label.lowercase()}", style = Village.type.small, color = b.glyph)
    }
}

/** A small status note under the request: a check for the code, a dot for the wishes. */
@Composable
private fun Note(text: String, saved: Boolean) {
    val c = Village.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Canvas(Modifier.size(14.dp)) {
            val s = size.width / 14f
            if (saved) {
                drawPath(Path().apply { moveTo(3 * s, 7.2f * s); lineTo(6 * s, 10 * s); lineTo(11 * s, 4.5f * s) }, c.primary, style = Stroke(2 * s, cap = StrokeCap.Round))
            } else {
                drawCircle(c.muted, 5 * s, style = Stroke(2 * s))
            }
        }
        Text(text, style = Village.type.small, color = c.inkMuted)
    }
}

/** `</>`: the Sandbox's glyph. */
@Composable
internal fun CodeGlyph(color: Color, size: Dp) {
    Canvas(Modifier.size(size)) {
        val s = this.size.width / 24f
        drawPath(Path().apply {
            moveTo(8 * s, 7 * s); lineTo(3 * s, 12 * s); lineTo(8 * s, 17 * s)
            moveTo(16 * s, 7 * s); lineTo(21 * s, 12 * s); lineTo(16 * s, 17 * s)
            moveTo(14 * s, 5 * s); lineTo(10 * s, 19 * s)
        }, color, style = Stroke(1.9f * s, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}
