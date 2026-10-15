import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// Iara's timetable: a "Where to?" field, and under it only the
// ferries going where the passenger typed.
data class Departure(val time: String, val destination: String)

val departures = listOf(
    Departure("08:00", "Coral Island"),
    Departure("10:30", "Lighthouse Point"),
    Departure("13:15", "Coral Island"),
    Departure("16:45", "Old Harbor"),
)

// Tip: TextField(value = ..., onValueChange = { ... },
//     label = { Text("Where to?") }) shows a field. It only keeps
// what's typed if its value comes from state that you update in
// onValueChange.
@Composable
fun PlayerScreen() {
    Column(Modifier.padding(16.dp)) {
        Text("Iara's Ferry")
        departures.forEach { departure ->
            Text("${departure.time}  ${departure.destination}")
        }
    }
}
