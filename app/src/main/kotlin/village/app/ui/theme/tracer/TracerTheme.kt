package village.app.ui.theme.tracer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import village.app.ShopState
import village.app.ui.components.VillagerBadge
import village.app.ui.home.CodeGlyph
import village.app.ui.theme.BUILDING_SIZE
import village.app.ui.theme.BadgeColors
import village.app.ui.theme.CityTheme
import village.app.ui.theme.Scenery
import village.app.ui.theme.Village
import village.app.ui.theme.VillageColors
import village.app.ui.theme.VillageFonts
import village.app.ui.theme.VillageShapes
import village.levels.Profession

/**
 * Loud colors on near-black, none of them close to a cozy token, so a surface still drawn in cozy values
 * stands out in the window check's tracer screenshots (themeable-ui D9).
 */
private val TracerColors = VillageColors(
    ground = Color(0xFF0B0B1E),
    groundRaised = Color(0xFF15153A),
    surface = Color(0xFF1C1C4A),
    outline = Color(0xFF5C5CFF),
    ink = Color(0xFFFFFFFF),
    inkMuted = Color(0xFFB8B8FF),
    accent = Color(0xFFFF00CC),
    accentStrong = Color(0xFFFF66E0),
    onAccent = Color(0xFF000000),
    primary = Color(0xFF00FF88),
    onPrimary = Color(0xFF000000),
    primaryShadow = Color(0xFF008844),
    error = Color(0xFFFF3355),
    onError = Color(0xFF000000),
    errorContainer = Color(0xFF3A0012),
    onErrorContainer = Color(0xFFFF99AA),
    glow = Color(0xFFFFEE00),
    chip = Color(0xFF003344),
    muted = Color(0xFF444466),
    currentRing = Color(0xFFFF00CC),
    track = Color(0xFF222255),
    switchThumb = Color(0xFF00FFFF),
    // The board behind the player's screen stays light: their UI is drawn in its own (default) colors.
    signBoard = Color(0xFFE0FFE0),
    glowRing = Color(0xFFFFF380),
    celebration = Color(0xFF1A1A40),
    statTile = Color(0xFF2A2A60),
    errorOutline = Color(0xFFFF3355),
    errorDetail = Color(0xFFFFD0DA),
    bubble = Color(0xFF102040),
    successOutline = Color(0xFF00FF88),
    editorBg = Color(0xFF000000),
    editorDivider = Color(0xFF333366),
    editorRaised = Color(0xFF111133),
    editorMuted = Color(0xFF9999CC),
    editorOutline = Color(0xFF5555AA),
    editorText = Color(0xFF00FFFF),
    lineNumber = Color(0xFF6666AA),
    syntaxKeyword = Color(0xFFFF00CC),
    syntaxString = Color(0xFF00FF88),
    syntaxFunction = Color(0xFFFFEE00),
    syntaxComment = Color(0xFF8888BB),
    syntaxAnnotation = Color(0xFFFF8800),
    editorErrorBand = Color(0xFF440022),
    editorErrorText = Color(0xFFFF6688),
    editorOk = Color(0xFF00FF88),
)

/** A square house in flat magenta and yellow. */
private val TracerBrandMark: ImageVector by lazy {
    ImageVector.Builder("tracer", 32.dp, 32.dp, 32f, 32f).apply {
        addPath(addPathNodes("M4 4 H28 V28 H4 Z"), fill = SolidColor(Color(0xFFFF00CC)))
        addPath(addPathNodes("M12 14 H20 V28 H12 Z"), fill = SolidColor(Color(0xFFFFEE00)))
    }.build()
}

/** Flat rectangles only, so it is obvious which code drew the street. */
private object TracerScenery : Scenery {
    override val sky = Color(0xFF101030)
    override val onSky = Color(0xFFCCCCFF)
    override val footerShop = RectangleShape

    override fun DrawScope.drawSky() {
        val d = 1.dp.toPx()
        for (i in 0 until 6) drawRect(Color(0xFF2A2A70), Offset(size.width * (0.08f + i * 0.16f), 30 * d), Size(40 * d, 10 * d))
    }

    override fun DrawScope.drawGround() {
        drawRect(Color(0xFF203020), Offset(0f, size.height / 2), Size(size.width, size.height / 2))
    }

    override fun DrawScope.drawRoad() {
        drawRect(Color(0xFF303030))
        drawRect(Color(0xFFFFEE00), Offset(0f, size.height / 2 - 1.dp.toPx()), Size(size.width, 2.dp.toPx()))
    }

    @Composable
    override fun ShopFront(state: ShopState, profession: Profession) {
        val c = Village.colors
        val lit = state == ShopState.OPEN
        val locked = state == ShopState.NOT_YET || state == ShopState.COMING_SOON
        Box(Modifier.size(BUILDING_SIZE)) {
            Canvas(Modifier.size(BUILDING_SIZE)) {
                val s = size.width / 150f
                drawRect(if (lit) c.glow else c.muted, Offset(20 * s, 40 * s), Size(110 * s, 110 * s))
                drawRect(c.accent, Offset(20 * s, 40 * s), Size(110 * s, 110 * s), style = Stroke(3 * s))
                drawRect(c.ground, Offset(62 * s, 100 * s), Size(26 * s, 50 * s))
            }
            Box(Modifier.align(Alignment.TopCenter).offset(y = 4.dp)) { VillagerBadge(profession, size = 40.dp, locked = locked) }
        }
    }

    @Composable
    override fun SandboxFront() {
        val c = Village.colors
        Box(Modifier.size(BUILDING_SIZE)) {
            Canvas(Modifier.size(BUILDING_SIZE)) {
                val s = size.width / 150f
                drawRect(c.editorBg, Offset(20 * s, 40 * s), Size(110 * s, 110 * s))
                drawRect(c.syntaxKeyword, Offset(20 * s, 40 * s), Size(110 * s, 110 * s), style = Stroke(3 * s))
            }
            Box(Modifier.align(Alignment.Center).size(56.dp).background(c.chip).border(3.dp, c.accent), contentAlignment = Alignment.Center) {
                CodeGlyph(c.syntaxKeyword, 36.dp)
            }
        }
    }
}

/** The tracer theme: dark, square, JetBrains Mono titles. Only the window check uses it (`--theme tracer`). */
internal val TracerTheme = CityTheme(
    id = "tracer", name = "Tracer", dark = true,
    colors = TracerColors,
    display = VillageFonts.JetBrainsMono,
    shapes = VillageShapes(
        card = RectangleShape,
        editor = RectangleShape,
        sign = RectangleShape,
        row = RectangleShape,
        tile = RectangleShape,
        small = RectangleShape,
        bubble = RectangleShape,
        button = RectangleShape,
        chip = RectangleShape,
        viewSwitch = RectangleShape,
        buttonShadow = DpOffset(4.dp, 4.dp),
    ),
    badges = BadgeColors(
        tints = mapOf(
            Profession.BAKER to Color(0xFF663300),
            Profession.FLORIST to Color(0xFF660044),
            Profession.CHEF to Color(0xFF334455),
            Profession.TEACHER to Color(0xFF224400),
            Profession.BOAT_CAPTAIN to Color(0xFF003366),
            Profession.FISHMONGER to Color(0xFF005555),
            Profession.MAIL_CARRIER to Color(0xFF553300),
            Profession.MAYOR to Color(0xFF330066),
        ),
        ring = Color(0xFF00FFFF),
        glyph = Color(0xFFFFFFFF),
        lockedFill = Color(0xFF222233),
        lockedRing = Color(0xFF555577),
        lockedAlpha = 0.4f,
    ),
    brandMark = TracerBrandMark,
    scenery = TracerScenery,
)
