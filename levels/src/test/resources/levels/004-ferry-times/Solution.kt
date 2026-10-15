import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class Departure(val time: String, val destination: String)

val departures = listOf(
    Departure("08:00", "Coral Island"),
    Departure("10:30", "Lighthouse Point"),
    Departure("13:15", "Coral Island"),
    Departure("16:45", "Old Harbor"),
)

@Composable
fun PlayerScreen() {
    var query by remember { mutableStateOf("") }
    val matches = departures.filter { it.destination.contains(query, ignoreCase = true) }
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Iara's Ferry", style = MaterialTheme.typography.headlineSmall)
        TextField(value = query, onValueChange = { query = it }, label = { Text("Where to?") }, modifier = Modifier.fillMaxWidth())
        if (matches.isEmpty()) {
            Text("No ferry goes there")
        } else {
            matches.forEach { departure ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(departure.time)
                    Text(departure.destination)
                }
            }
        }
    }
}
