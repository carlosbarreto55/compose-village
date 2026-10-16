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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import village.app.ui.icons.VillagerIcons
import village.app.ui.theme.Village
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

/** The round profession badge in the theme's badge colors: tint fill, 3 dp ring, glyph; [locked] drops the tint. */
@Composable
fun VillagerBadge(profession: Profession, size: Dp = 72.dp, locked: Boolean = false, contentDescription: String? = null) {
    val b = Village.badges
    Box(
        Modifier.size(size).alpha(if (locked) b.lockedAlpha else 1f)
            .clip(CircleShape).background(if (locked) b.lockedFill else b.tint(profession))
            .border(3.dp, if (locked) b.lockedRing else b.ring, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(profession.icon, contentDescription, Modifier.size(size * 0.5f), tint = b.glyph)
    }
}

/** "🌸 Florist": the profession's glyph and label in a tinted pill. */
@Composable
fun ProfessionPill(profession: Profession) {
    val b = Village.badges
    Row(
        Modifier.clip(Village.shapes.chip).background(b.tint(profession)).padding(start = 4.dp, end = 10.dp, top = 3.dp, bottom = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(profession.icon, null, Modifier.size(20.dp), tint = b.glyph)
        Text(profession.label, style = Village.type.small, color = b.glyph)
    }
}
