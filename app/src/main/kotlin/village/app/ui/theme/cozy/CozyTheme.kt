package village.app.ui.theme.cozy

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import village.app.ui.theme.BadgeColors
import village.app.ui.theme.CityTheme
import village.app.ui.theme.VillageColors
import village.app.ui.theme.VillageFonts
import village.app.ui.theme.VillageShapes
import village.levels.Profession

/** The cozy palette (`design-docs/cozy/README.md`), plus the in-between shades the mockups use. */
private val CozyColors = VillageColors(
    ground = Color(0xFFF3EADB),
    groundRaised = Color(0xFFFBF4E8),
    surface = Color(0xFFFFF9EF),
    outline = Color(0xFFE2D3BC),
    ink = Color(0xFF2B2118),
    inkMuted = Color(0xFF6B5A48),
    accent = Color(0xFF7A4E2D),
    accentStrong = Color(0xFF5A3820),
    onAccent = Color(0xFFFFFFFF),
    primary = Color(0xFF2F6B45),
    onPrimary = Color(0xFFFFFFFF),
    primaryShadow = Color(0xFF1E4A2F),
    error = Color(0xFFA3402A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF8E3DC),
    onErrorContainer = Color(0xFF7E2E1C),
    glow = Color(0xFFF2C46B),
    chip = Color(0xFFF3E3C8),
    muted = Color(0xFFD8C7AE),
    currentRing = Color(0xFFEBCFA8),
    track = Color(0xFFF0E2CB),
    switchThumb = Color(0xFFFFFFFF),
    signBoard = Color(0xFFFFFDF8),
    glowRing = Color(0xFFF7DFA3),
    celebration = Color(0xFFFFFDF6),
    statTile = Color(0xFFF6ECDA),
    errorOutline = Color(0xFFE8BFB1),
    errorDetail = Color(0xFF5E2416),
    bubble = Color(0xFFFFFFFF),
    successOutline = Color(0xFFBFDCC6),
    editorBg = Color(0xFF2A2320),
    editorDivider = Color(0xFF3A302B),
    editorRaised = Color(0xFF352C28),
    editorMuted = Color(0xFFC9B8A4),
    editorOutline = Color(0xFF5A4A3E),
    editorText = Color(0xFFF1E6D6),
    lineNumber = Color(0xFF7D6C5B),
    syntaxKeyword = Color(0xFFE6A15C),
    syntaxString = Color(0xFFA8D08D),
    syntaxFunction = Color(0xFF8FC1E3),
    syntaxComment = Color(0xFFA89784),
    syntaxAnnotation = Color(0xFFD9B8F0),
    editorErrorBand = Color(0xFF4A2E25),
    editorErrorText = Color(0xFFE8A08C),
    editorOk = Color(0xFF8FBF8F),
)

/** Main Street's look: parchment, wood and lamplight. The first city theme. */
internal val CozyTheme = CityTheme(
    id = "cozy", name = "Cozy", dark = false,
    colors = CozyColors,
    display = VillageFonts.Fredoka,
    shapes = VillageShapes(
        card = RoundedCornerShape(22.dp),
        editor = RoundedCornerShape(18.dp),
        sign = RoundedCornerShape(18.dp),
        row = RoundedCornerShape(14.dp),
        tile = RoundedCornerShape(12.dp),
        small = RoundedCornerShape(8.dp),
        bubble = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomEnd = 18.dp, bottomStart = 4.dp),
        button = RoundedCornerShape(percent = 50),
        chip = RoundedCornerShape(percent = 50),
        viewSwitch = RoundedCornerShape(percent = 50),
        buttonShadow = DpOffset(0.dp, 3.dp),
    ),
    // The README's villager table.
    badges = BadgeColors(
        tints = mapOf(
            Profession.BAKER to Color(0xFFF6DFB8),
            Profession.FLORIST to Color(0xFFF7D3DA),
            Profession.CHEF to Color(0xFFE9EBEE),
            Profession.TEACHER to Color(0xFFD7E8D2),
            Profession.BOAT_CAPTAIN to Color(0xFFCFE3F0),
            Profession.FISHMONGER to Color(0xFFD3ECEA),
            Profession.MAIL_CARRIER to Color(0xFFF1DCC8),
            Profession.MAYOR to Color(0xFFE3D9F0),
        ),
        ring = CozyColors.accent,
        glyph = CozyColors.accentStrong,
        lockedFill = Color(0xFFE2D6C2),
        lockedRing = Color(0xFFA8927A),
        lockedAlpha = 0.55f,
    ),
    brandMark = CozyBrandMark,
    scenery = CozyScenery,
)
