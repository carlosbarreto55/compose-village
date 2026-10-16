package village.app.ui.villagers.cast

import androidx.compose.ui.graphics.Color

/** Rosa, the baker: baker's hat, brown hair, white jacket, flour on her cheek. */
internal val Rosa = VillagerLook(Face(), back = {
    circle(100f, 108f, 58f, Color(0xFF5A3820))
    shape("M28 240 C28 186 60 164 100 164 C140 164 172 186 172 240 Z", Color(0xFFFFFDF8), Color(0xFFE2D3BC), 3f)
    line("M100 170 V240", Color(0xFFE2D3BC), 2f)
    circle(112f, 198f, 4f, Color(0xFFB8A58C)); circle(112f, 222f, 4f, Color(0xFFB8A58C))
    neck(Color(0xFFB57D55), 30f)
    head(Color(0xFFC98E64), cy = 104f, r = 48f, earY = 108f)
    shape("M54 98 Q62 70 100 68 Q138 70 146 98 Q126 82 100 84 Q74 82 54 98 Z", Color(0xFF5A3820))
    shape("M50 74 C34 74 34 46 56 48 C58 22 90 18 100 34 C110 18 142 22 144 48 C166 46 166 74 150 74 Z", Color.White, Color(0xFFE2D3BC), 3f)
    roundRect(54f, 64f, 92f, 18f, 6f, Color.White, Color(0xFFE2D3BC), 3f)
}, front = {
    circle(136f, 114f, 3f, Color.White, alpha = 0.85f) // flour on her cheek
})
