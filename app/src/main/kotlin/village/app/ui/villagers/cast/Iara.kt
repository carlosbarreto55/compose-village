package village.app.ui.villagers.cast

import androidx.compose.ui.graphics.Color

/** Iara, the boat captain: captain's hat with an anchor badge, long black hair, navy coat. */
internal val Iara = VillagerLook(
    Face(eyeY = 108f, mouthY = 130f, openCy = 133f, tongueCy = 138f, cheekY = 126f, cheek = Color(0xFFD9806A), cheekAlpha = 0.6f),
    back = {
        shape("M46 104 Q40 176 66 186 H134 Q160 176 154 104 Q150 52 100 50 Q50 52 46 104 Z", Color(0xFF2B2118)) // long hair
        shape("M28 240 C28 186 60 164 100 164 C140 164 172 186 172 240 Z", Color(0xFF2F4A6B))
        line("M76 168 L100 200 L124 168", Color(0xFFF3E3C8), 4f)
        circle(88f, 216f, 4f, Color(0xFFF2C46B)); circle(112f, 216f, 4f, Color(0xFFF2C46B))
        neck(Color(0xFF9C6B46), 30f)
        head(Color(0xFFB07A52), cy = 104f, r = 48f, earY = 108f)
        shape("M56 82 Q54 38 100 36 Q146 38 144 82 Z", Color.White, Color(0xFFCFD8E0), 3f)
        roundRect(56f, 68f, 88f, 14f, 0f, Color(0xFF1E2F45))
        shape("M40 84 Q100 70 160 84 Q158 94 100 90 Q42 94 40 84 Z", Color(0xFF1E2F45))
        circle(100f, 54f, 8f, Color(0xFFF2C46B))
        line("M100 49 V59 M96 56 Q100 61 104 56", Color(0xFF1E2F45), 1.8f) // the anchor
    },
)
