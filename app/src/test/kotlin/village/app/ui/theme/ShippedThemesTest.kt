package village.app.ui.theme

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

/** city-themes: Tracer theme. Players only ever see shipped themes; the tracer is for the window check. */
class ShippedThemesTest {
    @Test fun onlyCozyShips() {
        assertEquals(listOf(CityThemes.Cozy), CityThemes.shipped)
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Test fun villageThemeDefaultsToCozy() {
        var drawn: CityTheme? = null
        val scene = ImageComposeScene(10, 10, Density(1f)) { VillageTheme(reducedMotion = true) { drawn = Village.theme } }
        try {
            scene.render()
        } finally {
            scene.close()
        }
        assertSame(CityThemes.Cozy, drawn)
    }

    @Test fun theGameNeverPicksAnotherTheme() {
        val main = File("src/main/kotlin/village/app/Main.kt").readText()
        assertTrue("VillageTheme { GameScreens(controller) }" in main, "Main.kt should draw the game with VillageTheme's default (cozy)")
        // Only the theme definitions and the spike harness (the window check's --theme) may name other themes.
        val root = File("src/main/kotlin/village/app")
        val naming = Regex("""CityThemes\.(Tracer|all|byId)|TracerTheme""")
        val offenders = root.walkTopDown().filter { it.isFile && it.extension == "kt" }
            .map { it.relativeTo(root).invariantSeparatorsPath to it.readText() }
            .filter { (path, text) -> !path.startsWith("ui/theme/") && !path.startsWith("spike/") && naming.containsMatchIn(text) }
            .map { it.first }.toList()
        assertTrue(offenders.isEmpty(), "game code names a non-shipped theme: $offenders")
    }
}
