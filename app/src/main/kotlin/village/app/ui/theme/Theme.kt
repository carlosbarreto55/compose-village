package village.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import village.app.ui.theme.cozy.CozyBrandMark
import village.app.ui.theme.cozy.CozyScenery
import village.levels.Profession

/** The cozy palette (`design-docs/cozy/README.md`), plus the in-between shades the mockups use. Light only. */
@Immutable
class VillageColors(
    val ground: Color = Color(0xFFF3EADB),
    val groundRaised: Color = Color(0xFFFBF4E8),
    val surface: Color = Color(0xFFFFF9EF),
    val outline: Color = Color(0xFFE2D3BC),
    val ink: Color = Color(0xFF2B2118),
    val inkMuted: Color = Color(0xFF6B5A48),
    /** Cozy's `wood`: the sign frame, badge rings, the current shop's dashed outline, the dialogue border. */
    val accent: Color = Color(0xFF7A4E2D),
    /** Cozy's `woodDark`: the wordmark, headings, glyphs on badges, the dialogue's shadow. */
    val accentStrong: Color = Color(0xFF5A3820),
    /** Text on an `accent` fill: the street's "Start here" and "Up next" pills. */
    val onAccent: Color = Color(0xFFFFFFFF),
    val primary: Color = Color(0xFF2F6B45),
    /** Text and glyphs on `primary`. */
    val onPrimary: Color = Color(0xFFFFFFFF),
    val primaryShadow: Color = Color(0xFF1E4A2F),
    val error: Color = Color(0xFFA3402A),
    /** The cross on a failed check. */
    val onError: Color = Color(0xFFFFFFFF),
    val errorContainer: Color = Color(0xFFF8E3DC),
    val onErrorContainer: Color = Color(0xFF7E2E1C),
    val glow: Color = Color(0xFFF2C46B),
    val chip: Color = Color(0xFFF3E3C8),
    // Shades the mockups use between the named tokens.
    /** Not-yet levels: progress dots and dark shops. */
    val muted: Color = Color(0xFFD8C7AE),
    /** The ring around the current level's progress dot. */
    val currentRing: Color = Color(0xFFEBCFA8),
    /** The view switch's track, and the selected side's thumb. */
    val track: Color = Color(0xFFF0E2CB),
    val switchThumb: Color = Color(0xFFFFFFFF),
    /** The sign's inside, behind the player's frame. */
    val signBoard: Color = Color(0xFFFFFDF8),
    /** The glow's inner ring around a completed sign. */
    val glowRing: Color = Color(0xFFF7DFA3),
    /** The completion card's background and its stat tiles. */
    val celebration: Color = Color(0xFFFFFDF6),
    val statTile: Color = Color(0xFFF6ECDA),
    /** A failed checklist row's border and its detail text. */
    val errorOutline: Color = Color(0xFFE8BFB1),
    val errorDetail: Color = Color(0xFF5E2416),
    /** The villager's reaction bubble, and its border after a complete Run. */
    val bubble: Color = Color(0xFFFFFFFF),
    val successOutline: Color = Color(0xFFBFDCC6),
    // Editor and terminal panels (dark, warm).
    val editorBg: Color = Color(0xFF2A2320),
    val editorDivider: Color = Color(0xFF3A302B),
    val editorRaised: Color = Color(0xFF352C28),
    /** Quiet text and outlines on the editor background, as on the Sandbox card. */
    val editorMuted: Color = Color(0xFFC9B8A4),
    val editorOutline: Color = Color(0xFF5A4A3E),
    val editorText: Color = Color(0xFFF1E6D6),
    val lineNumber: Color = Color(0xFF7D6C5B),
    val syntaxKeyword: Color = Color(0xFFE6A15C),
    val syntaxString: Color = Color(0xFFA8D08D),
    val syntaxFunction: Color = Color(0xFF8FC1E3),
    val syntaxComment: Color = Color(0xFFA89784),
    val syntaxAnnotation: Color = Color(0xFFD9B8F0),
    /** Lines with compile errors, on the editor background. */
    val editorErrorBand: Color = Color(0xFF4A2E25),
    val editorErrorText: Color = Color(0xFFE8A08C),
    val editorOk: Color = Color(0xFF8FBF8F),
)

/**
 * The bundled families (D2): Fredoka is cozy's display family; Atkinson Hyperlegible and JetBrains Mono
 * are shared by every theme. Files and their license are in `resources/font/`.
 */
object VillageFonts {
    val Fredoka = FontFamily(
        Font("font/Fredoka-SemiBold.ttf", FontWeight.SemiBold),
        Font("font/Fredoka-Bold.ttf", FontWeight.Bold),
    )
    val Atkinson = FontFamily(
        Font("font/AtkinsonHyperlegible-Regular.ttf", FontWeight.Normal),
        Font("font/AtkinsonHyperlegible-Bold.ttf", FontWeight.Bold),
    )
    val JetBrainsMono = FontFamily(
        Font("font/JetBrainsMono-Regular.ttf", FontWeight.Normal),
        Font("font/JetBrainsMono-SemiBold.ttf", FontWeight.SemiBold),
    )
}

/**
 * Text styles named by role, sizes from the mockups. Display roles (wordmark, titles, names, buttons) use
 * the theme's [display] family; body and code are the same in every city (themeable-ui D4).
 */
@Immutable
class VillageType(display: FontFamily) {
    val wordmark: TextStyle = TextStyle(fontFamily = display, fontWeight = FontWeight.Bold, fontSize = 22.sp)
    /** "Tom's Flowers is open!" */
    val celebration: TextStyle = TextStyle(fontFamily = display, fontWeight = FontWeight.Bold, fontSize = 24.sp)
    /** Villager name, stat values. */
    val name: TextStyle = TextStyle(fontFamily = display, fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
    /** Card titles ("Tom's checklist") and buttons. */
    val title: TextStyle = TextStyle(fontFamily = display, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
    val button: TextStyle = TextStyle(fontFamily = display, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
    /** The villager's request. */
    val request: TextStyle = TextStyle(fontFamily = VillageFonts.Atkinson, fontSize = 18.sp, lineHeight = 27.sp)
    val bubble: TextStyle = TextStyle(fontFamily = VillageFonts.Atkinson, fontSize = 16.sp, lineHeight = 23.sp)
    val body: TextStyle = TextStyle(fontFamily = VillageFonts.Atkinson, fontSize = 15.sp, lineHeight = 21.sp)
    val small: TextStyle = TextStyle(fontFamily = VillageFonts.Atkinson, fontSize = 14.sp, lineHeight = 20.sp)
    val caption: TextStyle = TextStyle(fontFamily = VillageFonts.Atkinson, fontSize = 13.sp, lineHeight = 18.sp)
    /** "CONSOLE" over terminal panels. */
    val panelLabel: TextStyle = TextStyle(fontFamily = VillageFonts.Atkinson, fontWeight = FontWeight.Bold, fontSize = 13.sp, letterSpacing = 0.5.sp)
    val code: TextStyle = TextStyle(fontFamily = VillageFonts.JetBrainsMono, fontSize = 15.sp, lineHeight = 26.sp)
    val codeSmall: TextStyle = TextStyle(fontFamily = VillageFonts.JetBrainsMono, fontSize = 13.sp, lineHeight = 20.sp)
}

/** A theme's corners, and where the primary button's solid `primaryShadow` sits (themeable-ui D5). */
@Immutable
class VillageShapes(
    /** Cards, the dialogue box, the completion card. */
    val card: Shape,
    /** The editor, the terminal panels and the Sandbox card. */
    val editor: Shape,
    /** The sign frame and the street's buildings. */
    val sign: Shape,
    /** Checklist rows, stat tiles, the panel tip. */
    val row: Shape,
    /** Icon tiles, like the Sandbox card's. */
    val tile: Shape,
    /** Small controls and boxes: header buttons, timing chips, problem rows, the sign board. */
    val small: Shape,
    /** The reaction bubble, its tail at the bottom left. */
    val bubble: Shape,
    val button: Shape,
    /** Chips, pills, progress bars and dots. */
    val chip: Shape,
    /** The "Your screen / sketch" switch and its thumb. */
    val viewSwitch: Shape,
    /** The primary button's shadow offset. */
    val buttonShadow: DpOffset,
)

/**
 * A theme's villager badges (visual-style: Villager profession icons): a tint per profession, the ring
 * and glyph colors, and how a locked badge looks. The glyphs themselves are shared.
 */
@Immutable
class BadgeColors(
    private val tints: Map<Profession, Color>,
    val ring: Color,
    val glyph: Color,
    val lockedFill: Color,
    val lockedRing: Color,
    val lockedAlpha: Float,
) {
    init {
        require(tints.keys == Profession.entries.toSet()) { "badge tints must cover every profession; missing ${Profession.entries - tints.keys}" }
    }

    fun tint(profession: Profession): Color = tints.getValue(profession)
}

/** Page padding and gaps from the README: layout is the same in every city. */
object VillageSpacing {
    val pagePaddingH = 28.dp
    val pagePaddingV = 24.dp
    val cardGap = 16.dp
    val columnGap = 24.dp
}

/** Everything that makes a city look like itself (city-themes, themeable-ui D1). */
@Immutable
class CityTheme(
    val id: String,
    val name: String,
    /** Picks the Material 3 scheme; the system appearance never does. */
    val dark: Boolean,
    val colors: VillageColors,
    /** Titles, names and buttons; body and code text are shared. */
    val display: FontFamily,
    val shapes: VillageShapes,
    val badges: BadgeColors,
    /** The header's 32 × 32 mark beside "Compose Village", drawn untinted. */
    val brandMark: ImageVector,
    /** The street on the home screen. */
    val scenery: Scenery,
) {
    val type: VillageType = VillageType(display)

    override fun toString() = "CityTheme($id)"
}

object CityThemes {
    /** Main Street's look: parchment, wood and lamplight (`design-docs/cozy/README.md`). */
    private val cozyColors = VillageColors()

    val Cozy = CityTheme(
        id = "cozy", name = "Cozy", dark = false,
        colors = cozyColors,
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
            ring = cozyColors.accent,
            glyph = cozyColors.accentStrong,
            lockedFill = Color(0xFFE2D6C2),
            lockedRing = Color(0xFFA8927A),
            lockedAlpha = 0.55f,
        ),
        brandMark = CozyBrandMark,
        scenery = CozyScenery,
    )
}

val LocalCityTheme = staticCompositionLocalOf { CityThemes.Cozy }

/** Shorthand for the current theme's parts. */
object Village {
    val theme: CityTheme @Composable get() = LocalCityTheme.current
    val colors: VillageColors @Composable get() = LocalCityTheme.current.colors
    val type: VillageType @Composable get() = LocalCityTheme.current.type
    val shapes: VillageShapes @Composable get() = LocalCityTheme.current.shapes
    val badges: BadgeColors @Composable get() = LocalCityTheme.current.badges
    val scenery: Scenery @Composable get() = LocalCityTheme.current.scenery
}

/**
 * Draws [content] in [theme] (themeable-ui D1): its tokens, plus a Material 3 scheme built from them so
 * Material components (checkbox, tooltip, progress bar) match.
 */
@Composable
fun VillageTheme(theme: CityTheme = CityThemes.Cozy, reducedMotion: Boolean = ReducedMotion.system, content: @Composable () -> Unit) {
    val c = theme.colors
    val scheme = if (theme.dark) {
        darkColorScheme(
            primary = c.primary, onPrimary = c.onPrimary,
            secondary = c.accent, onSecondary = c.onAccent,
            tertiary = c.glow, onTertiary = c.ground,
            background = c.ground, onBackground = c.ink,
            surface = c.surface, onSurface = c.ink, surfaceVariant = c.chip, onSurfaceVariant = c.inkMuted,
            outline = c.outline, outlineVariant = c.outline,
            error = c.error, onError = c.onError, errorContainer = c.errorContainer, onErrorContainer = c.onErrorContainer,
        )
    } else {
        lightColorScheme(
            primary = c.primary, onPrimary = c.onPrimary,
            secondary = c.accent, onSecondary = c.onAccent,
            tertiary = c.glow, onTertiary = c.ink,
            background = c.ground, onBackground = c.ink,
            surface = c.surface, onSurface = c.ink, surfaceVariant = c.chip, onSurfaceVariant = c.inkMuted,
            outline = c.outline, outlineVariant = c.outline,
            error = c.error, onError = c.onError, errorContainer = c.errorContainer, onErrorContainer = c.onErrorContainer,
        )
    }
    val a = VillageFonts.Atkinson
    val f = theme.display
    val m3 = Typography().let { t ->
        Typography(
            titleLarge = t.titleLarge.copy(fontFamily = f), titleMedium = t.titleMedium.copy(fontFamily = f), titleSmall = t.titleSmall.copy(fontFamily = f),
            bodyLarge = t.bodyLarge.copy(fontFamily = a), bodyMedium = t.bodyMedium.copy(fontFamily = a), bodySmall = t.bodySmall.copy(fontFamily = a),
            labelLarge = t.labelLarge.copy(fontFamily = a), labelMedium = t.labelMedium.copy(fontFamily = a), labelSmall = t.labelSmall.copy(fontFamily = a),
        )
    }
    CompositionLocalProvider(
        LocalCityTheme provides theme,
        LocalReducedMotion provides reducedMotion,
    ) {
        MaterialTheme(colorScheme = scheme, typography = m3, content = content)
    }
}
