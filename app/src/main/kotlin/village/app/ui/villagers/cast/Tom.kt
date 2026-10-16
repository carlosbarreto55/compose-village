package village.app.ui.villagers.cast

import androidx.compose.ui.graphics.Color

/** Tom, the florist: green cap with a pink flower, green shirt, apron. */
internal val Tom = VillagerLook(Face(), back = {
    shape("M28 240 C28 186 60 164 100 164 C140 164 172 186 172 240 Z", Color(0xFF4E7A3A))
    shape("M72 180 C82 174 118 174 128 180 L134 240 H66 Z", Color(0xFFF3E3C8))
    circle(116f, 204f, 6f, Color(0xFFE07A8A)); circle(116f, 204f, 2.2f, Color(0xFFF7D9A8))
    neck(Color(0xFFD9A27A), 32f)
    head(Color(0xFFE8B48A))
    shape("M46 92 Q100 4 154 92 Q100 70 46 92 Z", Color(0xFF4E7A3A))
    circle(140f, 64f, 14f, Color(0xFFE07A8A)); circle(140f, 64f, 5f, Color(0xFFF7D9A8))
})
