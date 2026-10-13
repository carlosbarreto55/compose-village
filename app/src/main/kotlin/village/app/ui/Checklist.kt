package village.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import village.app.GameText
import village.app.LevelProgress
import village.app.Reaction
import village.app.ui.components.Mark
import village.app.ui.components.PrimaryButton
import village.app.ui.components.SecondaryButton
import village.app.ui.components.StatusMark
import village.app.ui.components.VillageCard
import village.app.ui.components.VillagerBadge
import village.app.ui.components.nudge
import village.app.ui.theme.LocalReducedMotion
import village.app.ui.theme.Village
import village.app.ui.theme.VillageShapes
import village.levels.CriterionResult
import village.levels.Level
import village.levels.Verdict

/**
 * The level's requirements (spec: Requirements checklist): "<Name>'s checklist · k of n", one line per
 * criterion. After each Run ([runCount]) the lines resolve one by one; a failed line expands with the
 * generated detail and, unless the [reaction] bubble already says it, the criterion's message.
 */
@Composable
fun ChecklistCard(level: Level, verdict: Verdict?, reaction: Reaction?, runCount: Int) {
    val c = Village.colors
    val name = level.villager.name
    val results = (verdict as? Verdict.Evaluated)?.results
    val complete = verdict is Verdict.Evaluated && verdict.passed
    val passing = results?.count { it.passed } ?: 0
    VillageCard(gap = if (complete) 10.dp else 12.dp) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(GameText.checklistTitle(name), style = if (complete) Village.type.title.copy(fontSize = 16.sp) else Village.type.title, color = c.ink, modifier = Modifier.weight(1f))
            Text(
                "$passing of ${level.criteria.size}",
                style = if (complete) Village.type.small.copy(fontWeight = FontWeight.Bold) else Village.type.small,
                color = if (complete) c.primary else c.inkMuted,
            )
        }
        if (verdict is Verdict.NotRun) {
            Text(
                when (verdict.reason) {
                    Verdict.NotRun.Reason.DID_NOT_COMPILE -> "Your code didn't compile. Fix the problems in the editor, then Run again."
                    Verdict.NotRun.Reason.CRASHED -> "Your screen crashed before $name could see it."
                },
                style = Village.type.small, color = c.inkMuted,
            )
        }
        if (complete) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(level.criteria.size) { Box(Modifier.weight(1f).height(8.dp).clip(VillageShapes.pill).background(c.primary)) }
            }
            return@VillageCard
        }
        val firstFailure = results?.indexOfFirst { !it.passed } ?: -1
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            level.criteria.forEachIndexed { i, criterion ->
                val result = results?.getOrNull(i)
                ChecklistRow(
                    index = i, description = criterion.description, result = result,
                    showMessage = result?.message != null && result.message != reaction?.text,
                    shake = i == firstFailure, runCount = runCount,
                )
            }
        }
    }
}

/** One requirement; resolves [index] x 120 ms after each Run, then shakes once if it's the first failure. */
@Composable
private fun ChecklistRow(index: Int, description: String, result: CriterionResult?, showMessage: Boolean, shake: Boolean, runCount: Int) {
    val c = Village.colors
    val reduced = LocalReducedMotion.current
    val target = when (result?.passed) { true -> Mark.PASSED; false -> Mark.FAILED; null -> Mark.PENDING }
    var shown by remember { mutableStateOf(target) }
    val offset = remember { Animatable(0f) }
    LaunchedEffect(runCount, target) {
        if (target == Mark.PENDING) { shown = Mark.PENDING; return@LaunchedEffect }
        shown = Mark.PENDING
        delay(index * 120L)
        shown = target
        if (target == Mark.FAILED && shake && !reduced) {
            offset.animateTo(0f, keyframes { durationMillis = 300; 6f at 50; -6f at 100; 4f at 175; -2f at 250 })
        }
    }
    val failed = shown == Mark.FAILED
    val fill by animateColorAsState(if (failed) c.error else c.primary, tween(200))
    val background by animateColorAsState(if (failed) c.errorContainer else Color.Transparent, tween(200))
    val border by animateColorAsState(if (failed) c.errorOutline else Color.Transparent, tween(200))
    val pad by animateDpAsState(if (failed) 12.dp else 0.dp, tween(200))
    Column(
        Modifier.fillMaxWidth().nudge(offset.value).clip(VillageShapes.row).background(background).border(1.dp, border, VillageShapes.row)
            .padding(horizontal = pad * 14 / 12, vertical = pad),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatusMark(shown, fill)
            Text(
                description,
                style = if (failed) Village.type.body.copy(fontWeight = FontWeight.Bold) else Village.type.body,
                color = if (failed) c.onErrorContainer else c.ink,
            )
        }
        AnimatedVisibility(failed, enter = expandVertically(tween(220)) + fadeIn(tween(220)), exit = shrinkVertically(tween(150)) + fadeOut(tween(150))) {
            Column(Modifier.padding(start = 30.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                result?.detail?.let { Text(it, style = Village.type.small, color = c.errorDetail) }
                if (showMessage) result?.message?.let { Text(it, style = Village.type.small.copy(fontStyle = FontStyle.Italic), color = c.errorDetail) }
            }
        }
    }
}

/**
 * The completion card (spec: Level complete), driven by the screen's completion [transition]: it slides
 * 24 dp up and fades in after the sign's glow, unless the player chose Keep tinkering. With [welcomeBack],
 * the level's saved progress, it is the card for a completed level the player came back to (village-progress D6).
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun CompletionCard(
    transition: Transition<Boolean>, dismissed: Boolean,
    level: Level, lines: Int, tries: Int, next: String?, onNext: () -> Unit, onKeepTinkering: () -> Unit,
    welcomeBack: LevelProgress? = null, onPlayAgain: () -> Unit = {},
) {
    val c = Village.colors
    val rise = with(LocalDensity.current) { 24.dp.roundToPx() }
    transition.AnimatedVisibility(
        visible = { it && !dismissed },
        enter = slideInVertically(tween(300, delayMillis = 600)) { rise } + fadeIn(tween(300, delayMillis = 600)),
        exit = fadeOut(tween(150)),
    ) {
        Column(
            Modifier.fillMaxWidth().clip(VillageShapes.card).background(c.celebration).border(2.dp, c.glow, VillageShapes.card).padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                VillagerBadge(level.villager.profession, size = 48.dp)
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(GameText.shopOpen(level.shop), style = Village.type.celebration, color = c.ink)
                    Text(
                        welcomeBack?.let { GameText.finishedIn(it.tries) } ?: GameText.wishesCameTrue(level.criteria.size, level.villager.name),
                        style = Village.type.body, color = c.inkMuted,
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                val stats = welcomeBack?.let { listOf("Lines" to (it.lines ?: lines), "Tries" to it.tries) } ?: listOf("Lines" to lines, "Tries" to tries)
                for ((label, value) in stats) {
                    Column(
                        Modifier.weight(1f).clip(VillageShapes.row).background(c.statTile).padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(label, style = Village.type.caption, color = c.inkMuted)
                        Text(value.toString(), style = Village.type.name.copy(fontSize = 22.sp), color = c.ink)
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (next != null) {
                    PrimaryButton(onClick = onNext, modifier = Modifier.weight(1f), height = 52.dp) {
                        Text("Next: $next", style = Village.type.button, color = Color.White, maxLines = 1)
                    }
                } else {
                    Text(GameText.COMING_SOON, style = Village.type.body, color = c.inkMuted, modifier = Modifier.weight(1f))
                }
                if (welcomeBack != null) SecondaryButton(GameText.PLAY_AGAIN, onClick = onPlayAgain, height = 52.dp)
                else SecondaryButton("Keep tinkering", onClick = onKeepTinkering, height = 52.dp)
            }
        }
    }
}
