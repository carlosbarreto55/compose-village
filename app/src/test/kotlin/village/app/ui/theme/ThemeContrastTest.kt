package village.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.test.Test
import kotlin.test.assertTrue

/** city-themes: Contrast in every theme. Body text pairs reach 4.5:1 in each theme the game defines. */
class ThemeContrastTest {
    /** WCAG 2 contrast ratio of [a] on [b]. */
    private fun contrast(a: Color, b: Color): Double {
        val (light, dark) = listOf(a.luminance(), b.luminance()).sortedDescending()
        return (light + 0.05) / (dark + 0.05)
    }

    /** The text-on-background pairs, named as the spec names them. */
    private fun pairs(c: VillageColors) = listOf(
        "ink on surface" to (c.ink to c.surface),
        "ink on ground" to (c.ink to c.ground),
        "inkMuted on surface" to (c.inkMuted to c.surface),
        "inkMuted on ground" to (c.inkMuted to c.ground),
        "onPrimary on primary" to (c.onPrimary to c.primary),
        "onErrorContainer on errorContainer" to (c.onErrorContainer to c.errorContainer),
        "editorText on editorBg" to (c.editorText to c.editorBg),
    )

    @Test fun everyThemeMeetsTheBodyTextContrast() {
        val failing = CityThemes.all.flatMap { theme ->
            pairs(theme.colors).mapNotNull { (name, colors) ->
                val ratio = contrast(colors.first, colors.second)
                if (ratio < 4.5) "${theme.id}: $name is ${"%.2f".format(ratio)}:1" else null
            }
        }
        assertTrue(failing.isEmpty(), "below 4.5:1:\n" + failing.joinToString("\n"))
    }

    @Test fun theTracerIsChecked() {
        assertTrue(CityThemes.Tracer in CityThemes.all)
    }

    @Test fun cozyMutedTextOnSurface() {
        val c = CityThemes.Cozy.colors
        assertTrue(contrast(c.inkMuted, c.surface) >= 4.5)
    }

    @Test fun aLowContrastPairIsCaught() {
        // The ratio can fail: cozy's muted ink on the `muted` shade (never a text background) is below 4.5:1.
        val c = CityThemes.Cozy.colors
        assertTrue(contrast(c.inkMuted, c.muted) < 4.5)
    }
}
