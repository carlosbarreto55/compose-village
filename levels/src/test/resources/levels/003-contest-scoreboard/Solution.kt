import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PlayerScreen() {
    var blue by remember { mutableStateOf(0) }
    var green by remember { mutableStateOf(0) }
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Contest Scoreboard", style = MaterialTheme.typography.headlineSmall)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Blue: $blue", Modifier.width(96.dp))
            Button(onClick = { blue++ }) { Text("+1 Blue") }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Green: $green", Modifier.width(96.dp))
            Button(onClick = { green++ }) { Text("+1 Green") }
        }
        OutlinedButton(onClick = { blue = 0; green = 0 }) { Text("Reset") }
    }
}
