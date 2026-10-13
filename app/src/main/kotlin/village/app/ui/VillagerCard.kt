package village.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import village.app.Reaction
import village.app.ui.components.ConceptChip
import village.app.ui.components.ProfessionPill
import village.app.ui.components.VillageCard
import village.app.ui.components.VillagerBadge
import village.app.ui.theme.Village
import village.levels.Level

/**
 * Who asks and what they want (spec: Villager card): badge, name, profession, the request with its
 * quoted phrases in bold; "New: …" chips before the first Run, then the villager's [reaction].
 */
@Composable
fun VillagerCard(level: Level, reaction: Reaction?, modifier: Modifier = Modifier) {
    val c = Village.colors
    VillageCard(modifier, padding = PaddingValues(horizontal = 22.dp, vertical = 20.dp), gap = 14.dp) {
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            VillagerBadge(level.villager.profession, contentDescription = "${level.villager.name} the ${level.villager.profession.label.lowercase()}")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(level.villager.name, style = Village.type.name, color = c.ink)
                    ProfessionPill(level.villager.profession)
                }
                Text(requestText(level.request), style = Village.type.request, color = c.ink)
                if (reaction == null && level.concepts.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (concept in level.concepts) ConceptChip("New: $concept")
                    }
                }
            }
        }
        if (reaction != null) Bubble(reaction)
    }
}

/** The speech bubble, under the villager's badge; its outline turns green when they're happy. */
@Composable
private fun Bubble(reaction: Reaction) {
    val c = Village.colors
    val shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomEnd = 18.dp, bottomStart = 4.dp)
    val outline by animateColorAsState(if (reaction.happy) c.successOutline else c.outline, tween(250))
    AnimatedContent(reaction.text, Modifier.padding(start = 90.dp), transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(150)) }) { text ->
        Text(
            text, style = Village.type.bubble, color = c.ink,
            modifier = Modifier.widthIn(max = 520.dp).clip(shape).background(Color.White).border(2.dp, outline, shape)
                .padding(horizontal = 16.dp, vertical = 10.dp),
        )
    }
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
