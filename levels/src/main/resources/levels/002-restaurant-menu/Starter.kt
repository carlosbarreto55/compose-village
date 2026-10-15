import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// Marco's menu: his kitchen's name on top, then every dish
// with its price on the right.
data class Dish(val name: String, val price: String)

val menu = listOf(
    Dish("Spaghetti", "€12"),
    Dish("Lasagna", "€14"),
    Dish("Risotto", "€15"),
    Dish("Tiramisu", "€7"),
)

// Tip: menu.forEach { dish -> ... } runs its block once for every
// dish, in order. A Row inside the Column puts a dish and its price
// side by side.
@Composable
fun PlayerScreen() {
    Column(Modifier.padding(16.dp)) {
        Text("Marco's Kitchen")
        Text("Spaghetti")
    }
}
