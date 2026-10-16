package village.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import village.app.Expression
import village.app.Reaction
import village.app.Typewriter
import village.app.dialogueNow
import village.app.ui.components.ConceptChip
import village.app.ui.components.ProfessionPill
import village.app.ui.components.VillageCard
import village.app.ui.theme.LocalReducedMotion
import village.app.ui.theme.Village
import village.app.ui.villagers.TypedText
import village.app.ui.villagers.VillagerPortrait
import village.app.ui.villagers.typingClock
import village.levels.Level

/**
 * Who asks and what they want (spec: Villager card): the villager's portrait with the [expression] of the
 * latest verdict, name, profession, Talk to <name> when [onTalk] is given, the request with its quoted
 * phrases in bold; "New: …" chips before the first Run, then the villager's [reaction], typed out while
 * the portrait talks. [runCount] restarts the typing on every Run, even when the line is the same.
 */
@Composable
fun VillagerCard(level: Level, reaction: Reaction?, expression: Expression, runCount: Int, onTalk: (() -> Unit)?, modifier: Modifier = Modifier) {
    val c = Village.colors
    val reduced = LocalReducedMotion.current
    val started = remember(reaction?.text, runCount) { reaction?.let { Typewriter(it.text, dialogueNow(), finished = reduced) } }
    var clicked by remember(reaction?.text, runCount) { mutableStateOf(false) }
    val typewriter = if (clicked) started?.finish() else started
    val now = typewriter?.let { typingClock(it) } ?: 0L
    VillageCard(modifier, padding = PaddingValues(horizontal = 22.dp, vertical = 20.dp), gap = 14.dp) {
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            VillagerPortrait(level.villager, AVATAR, expression, mouthOpen = typewriter?.mouthOpen(now) == true)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(level.villager.name, style = Village.type.name, color = c.ink)
                    ProfessionPill(level.villager.profession)
                    Spacer(Modifier.weight(1f))
                    if (level.intro.isNotEmpty()) TalkTo(level.villager.name, onTalk)
                }
                Text(requestText(level.request), style = Village.type.request, color = c.ink)
                if (reaction == null && level.concepts.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (concept in level.concepts) ConceptChip("New: $concept")
                    }
                }
            }
        }
        if (reaction != null && typewriter != null) Bubble(reaction, typewriter.shown(now), onClick = { clicked = true })
    }
}

/** The card's portrait size; the bubble lines up after it. */
private val AVATAR = 88.dp

/** "Talk to Tom": plays the level's intro again; dimmed and inert while [onTalk] is null (a Run is on). */
@Composable
private fun TalkTo(name: String, onTalk: (() -> Unit)?) {
    val c = Village.colors
    Row(
        Modifier.heightIn(min = 44.dp).clip(Village.shapes.button).border(1.dp, c.muted, Village.shapes.button)
            .clickable(enabled = onTalk != null, role = Role.Button) { onTalk?.invoke() }
            .padding(horizontal = 14.dp).alpha(if (onTalk != null) 1f else 0.5f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Canvas(Modifier.size(16.dp)) {
            // A small speech bubble.
            val s = size.width / 16f
            drawRoundRect(c.accent, Offset(1 * s, 2 * s), Size(14 * s, 9 * s), CornerRadius(3 * s), style = Stroke(1.6f * s))
            drawLine(c.accent, Offset(5 * s, 11 * s), Offset(4 * s, 14 * s), 1.6f * s)
        }
        Text("Talk to $name", style = Village.type.small, color = c.accentStrong)
    }
}

/**
 * The speech bubble, under the villager's portrait, with the reaction typed to [shown] characters; its
 * outline turns green when they're happy. The whole line is laid out from the start, so the bubble
 * keeps its size and the editor below doesn't move (talking-villagers D5). Clicking shows the whole line.
 */
@Composable
private fun Bubble(reaction: Reaction, shown: Int, onClick: () -> Unit) {
    val c = Village.colors
    val shape = Village.shapes.bubble
    val outline by animateColorAsState(if (reaction.happy) c.successOutline else c.outline, tween(250))
    TypedText(
        reaction.text, shown, Village.type.bubble, c.ink,
        modifier = Modifier.padding(start = AVATAR + 18.dp).widthIn(max = 520.dp).clip(shape).background(Village.colors.bubble).border(2.dp, outline, shape)
            .clickable(remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    )
}

/** “Put **Tom's Flowers** with…”: quoted phrases in bold without their quotes, the whole in curly quotes. */
fun requestText(request: String): AnnotatedString = buildAnnotatedString {
    append('“')
    val parts = request.split('"')
    parts.forEachIndexed { i, part ->
        // Odd parts were inside quotes; an unmatched last quote keeps its text plain.
        if (i % 2 == 1 && i < parts.lastIndex) withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(part) }
        else append(part)
    }
    append('”')
}
