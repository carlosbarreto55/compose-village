package village.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import village.app.ShopState
import village.levels.Profession

/** The size of every building's drawing, in every theme, so the street's layout and hit areas never move. */
val BUILDING_SIZE = 150.dp

/**
 * A theme's street (themeable-ui D2): drawing code, not parameters, so each city can have its own kind of
 * buildings. The street card keeps the layout (title block, buildings as buttons, statuses, the road's
 * place) and asks the scenery to draw the parts.
 */
interface Scenery {
    /** The street card's background, behind everything else. */
    val sky: Color

    /** The line under the street's title, drawn on [sky]. */
    val onSky: Color

    /** Drawn over [sky] behind the title block (cozy: two clouds). */
    fun DrawScope.drawSky()

    /** Drawn in the 120 dp band at the bottom of the buildings' row, behind them (cozy: the hill). */
    fun DrawScope.drawGround()

    /** The 44 dp road under the buildings, across the card's full width. */
    fun DrawScope.drawRoad()

    /** A level's building, [BUILDING_SIZE] square, in [state], with the villager's badge on it. */
    @Composable
    fun ShopFront(state: ShopState, profession: Profession)

    /** The Sandbox's building at the end of the street, [BUILDING_SIZE] square. */
    @Composable
    fun SandboxFront()
}
