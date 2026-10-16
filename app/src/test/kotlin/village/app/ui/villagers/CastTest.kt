package village.app.ui.villagers

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import village.app.Expression
import village.app.ui.theme.VillageTheme
import village.app.ui.villagers.cast.Cast
import village.levels.LevelRepository
import village.levels.Profession
import village.levels.Villager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** villager-dialogue: Villager busts. Every bundled level has its own character, with a face. */
class CastTest {
    @Test fun everyBundledVillagerHasABust() {
        val missing = LevelRepository().all().filter { Cast.forId(it.villager.id) == null }
        assertTrue(missing.isEmpty(), missing.joinToString("; ") { "level '${it.id}' has no bust for villager '${it.villager.id}'" })
    }

    @Test fun noTwoBundledLevelsShowTheSameBust() {
        val looks = LevelRepository().all().map { Cast.forId(it.villager.id) }
        assertEquals(looks.size, looks.toSet().size)
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Test fun aVillagerWithoutABustGetsTheirBadge() {
        val pedro = Villager("pedro", "Pedro", Profession.FISHMONGER)
        assertNull(Cast.forId("pedro"))
        for (drawn in listOf("bust", "portrait")) {
            val scene = ImageComposeScene(400, 400, Density(1f)) {
                VillageTheme(reducedMotion = true) {
                    if (drawn == "bust") VillagerBust(pedro, Expression.NEUTRAL, mouthOpen = false, height = 240.dp)
                    else VillagerPortrait(pedro, 80.dp)
                }
            }
            try {
                scene.render()
                val nodes = scene.semanticsOwners.flatMap { all(it.rootSemanticsNode) }
                val pedroNode = nodes.single { it.config.getOrNull(SemanticsProperties.ContentDescription) == listOf("Pedro the fishmonger") }
                // The profession badge is an Icon (an image); a bust is a plain canvas.
                assertEquals(Role.Image, pedroNode.config.getOrNull(SemanticsProperties.Role), "Pedro's $drawn should be his badge")
            } finally {
                scene.close()
            }
        }
    }

    private fun all(n: SemanticsNode): List<SemanticsNode> = listOf(n) + n.children.flatMap(::all)
}
