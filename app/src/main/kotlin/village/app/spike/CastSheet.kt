package village.app.spike

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import org.jetbrains.skia.EncodedImageFormat
import village.app.Expression
import village.app.ui.theme.Village
import village.app.ui.theme.VillageTheme
import village.app.ui.villagers.VillagerBust
import village.app.ui.villagers.VillagerPortrait
import village.levels.Profession
import village.levels.Villager
import java.io.File

/** The five villagers, as the cast sheet draws them. */
internal val CAST = listOf(
    Villager("rosa", "Rosa", Profession.BAKER),
    Villager("tom", "Tom", Profession.FLORIST),
    Villager("marco", "Marco", Profession.CHEF),
    Villager("lucia", "Dona Lúcia", Profession.TEACHER),
    Villager("iara", "Iara", Profession.BOAT_CAPTAIN),
)

/**
 * Every bust in every expression, plus talking and the card portrait (talking-villagers task 4.4), saved
 * as [file] for a side-by-side look with `design-docs/cozy/screens/villager-cast.html`.
 */
@OptIn(ExperimentalComposeUiApi::class)
internal fun castSheet(file: File) {
    val scene = ImageComposeScene(1180, 1080, Density(1f)) {
        VillageTheme(reducedMotion = true) {
            Column(
                Modifier.fillMaxSize().background(Village.colors.ground).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                for (villager in CAST) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        Text(villager.name, style = Village.type.name, color = Village.colors.ink, modifier = Modifier.padding(end = 8.dp))
                        for (expression in Expression.entries) VillagerBust(villager, expression, mouthOpen = false, height = 190.dp)
                        // Reduced motion closes the mouth, so the talking frame is drawn without it.
                        VillageTheme(reducedMotion = false) { VillagerBust(villager, Expression.NEUTRAL, mouthOpen = true, height = 190.dp) }
                        VillagerPortrait(villager, 88.dp, Expression.PUZZLED)
                    }
                }
            }
        }
    }
    try {
        scene.render()
        file.writeBytes(scene.render(500_000_000).encodeToData(EncodedImageFormat.PNG)!!.bytes)
    } finally {
        scene.close()
    }
}
