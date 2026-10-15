import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class Dish(val name: String, val price: String)

val menu = listOf(
    Dish("Spaghetti", "€12"),
    Dish("Lasagna", "€14"),
    Dish("Risotto", "€15"),
    Dish("Tiramisu", "€7"),
)

@Composable
fun PlayerScreen() {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Marco's Kitchen", style = MaterialTheme.typography.headlineSmall)
        menu.forEach { dish ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(dish.name)
                Text(dish.price)
            }
        }
    }
}
