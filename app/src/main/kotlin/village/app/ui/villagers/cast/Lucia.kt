package village.app.ui.villagers.cast

import androidx.compose.ui.graphics.Color

/** Dona Lúcia, the teacher: grey hair in a bun, round glasses, green cardigan. */
internal val Lucia = VillagerLook(
    Face(
        mouthY = 130f, openCy = 133f, openRx = 10f, openRy = 8f, tongueCy = 137f, tongueRx = 5.5f, tongueRy = 3f,
        mouthFill = Color(0xFF4A1E12), tongue = Color(0xFFC46A6A),
        cheekDx = 32f, cheekY = 128f, cheek = Color(0xFFB5644E), cheekAlpha = 0.6f,
    ),
    back = {
        circle(100f, 100f, 56f, Color(0xFFCFC6BA))
        circle(100f, 42f, 20f, Color(0xFFCFC6BA)) // her bun
        shape("M28 240 C28 186 60 164 100 164 C140 164 172 186 172 240 Z", Color(0xFF5E8A6A))
        shape("M80 166 L100 192 L120 166 Z", Color(0xFFFFF9EF))
        line("M100 192 V240", Color(0xFF4A6F55), 2f)
        circle(108f, 208f, 3.5f, Color(0xFFF2C46B)); circle(108f, 226f, 3.5f, Color(0xFFF2C46B))
        neck(Color(0xFF7E5036), 30f)
        head(Color(0xFF8D5B3E))
        shape("M52 98 Q58 58 100 56 Q142 58 148 98 Q132 74 100 76 Q68 74 52 98 Z", Color(0xFFCFC6BA))
        circle(82f, 106f, 13f, Color.White, alpha = 0.35f); circle(118f, 106f, 13f, Color.White, alpha = 0.35f)
    },
    front = {
        ring(82f, 106f, 13f, Color(0xFF3A2A20), 3f); ring(118f, 106f, 13f, Color(0xFF3A2A20), 3f)
        line("M95 106 H105", Color(0xFF3A2A20), 3f)
    },
)
