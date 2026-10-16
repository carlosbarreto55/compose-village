package village.app.ui.villagers.cast

import androidx.compose.ui.graphics.Color

/** Marco, the chef: tall chef's hat, moustache, red neckerchief, double-breasted jacket. */
internal val Marco = VillagerLook(
    Face(
        mouthY = 136f, mouthHalf = 10f, mouthDepth = 6f, openCy = 138f, openRx = 9f, openRy = 7f,
        tongueCy = 142f, tongueRx = 5f, tongueRy = 2.5f, cheekDx = 32f, cheekY = 122f, cheekAlpha = 0.6f,
    ),
    back = {
        shape("M28 240 C28 186 60 164 100 164 C140 164 172 186 172 240 Z", Color(0xFFFFFDF8), Color(0xFFD5D9DE), 3f)
        for (x in listOf(86f, 114f)) for (y in listOf(208f, 228f)) circle(x, y, 4f, Color(0xFFB8A58C))
        neck(Color(0xFFCF9670), 30f)
        shape("M76 166 L100 172 L124 166 L100 196 Z", Color(0xFFC2503A))
        head(Color(0xFFE0A77E))
        shape("M52 104 Q48 82 62 76 L66 104 Z M148 104 Q152 82 138 76 L134 104 Z", Color(0xFF3A2A20))
        shape("M58 78 C42 76 42 48 62 50 C58 18 94 8 100 28 C106 8 142 18 138 50 C158 48 158 76 142 78 Z", Color.White, Color(0xFFD5D9DE), 3f)
        roundRect(58f, 66f, 84f, 18f, 5f, Color.White, Color(0xFFD5D9DE), 3f)
        line("M72 94 L90 91 M110 91 L128 94", Color(0xFF3A2A20), 4f)
    },
    front = {
        shape("M78 126 Q88 116 100 124 Q112 116 122 126 Q112 132 100 127 Q88 132 78 126 Z", Color(0xFF3A2A20)) // moustache
    },
)
