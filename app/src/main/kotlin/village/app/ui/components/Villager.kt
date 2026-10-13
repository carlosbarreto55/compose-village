package village.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import village.app.ui.icons.VillagerIcons
import village.app.ui.theme.Village
import village.app.ui.theme.VillageShapes
import village.levels.Profession

/** Each profession's glyph. */
val Profession.icon: ImageVector
    get() = when (this) {
        Profession.BAKER -> VillagerIcons.baker
        Profession.FLORIST -> VillagerIcons.florist
        Profession.CHEF -> VillagerIcons.chef
        Profession.TEACHER -> VillagerIcons.teacher
        Profession.BOAT_CAPTAIN -> VillagerIcons.boatCaptain
        Profession.FISHMONGER -> VillagerIcons.fishmonger
        Profession.MAIL_CARRIER -> VillagerIcons.mailCarrier
        Profession.MAYOR -> VillagerIcons.mayor
    }

/** Each profession's badge tint (README villager table). Visual, so kept out of level data. */
val Profession.tint: Color
    get() = when (this) {
        Profession.BAKER -> Color(0xFFF6DFB8)
        Profession.FLORIST -> Color(0xFFF7D3DA)
        Profession.CHEF -> Color(0xFFE9EBEE)
        Profession.TEACHER -> Color(0xFFD7E8D2)
        Profession.BOAT_CAPTAIN -> Color(0xFFCFE3F0)
        Profession.FISHMONGER -> Color(0xFFD3ECEA)
        Profession.MAIL_CARRIER -> Color(0xFFF1DCC8)
        Profession.MAYOR -> Color(0xFFE3D9F0)
    }

/** The round profession badge: tint fill, 3 dp wood ring, glyph in woodDark; [locked] drops the tint. */
@Composable
fun VillagerBadge(profession: Profession, size: Dp = 72.dp, locked: Boolean = false, contentDescription: String? = null) {
    val c = Village.colors
    Box(
        Modifier.size(size).alpha(if (locked) 0.55f else 1f)
            .clip(CircleShape).background(if (locked) Color(0xFFE2D6C2) else profession.tint)
            .border(3.dp, if (locked) Color(0xFFA8927A) else c.wood, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(profession.icon, contentDescription, Modifier.size(size * 0.5f), tint = c.woodDark)
    }
}

/** "🌸 Florist": the profession's glyph and label in a tinted pill. */
@Composable
fun ProfessionPill(profession: Profession) {
    val c = Village.colors
    Row(
        Modifier.clip(VillageShapes.pill).background(profession.tint).padding(start = 4.dp, end = 10.dp, top = 3.dp, bottom = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(profession.icon, null, Modifier.size(20.dp), tint = c.woodDark)
        Text(profession.label, style = Village.type.small, color = c.woodDark)
    }
}
