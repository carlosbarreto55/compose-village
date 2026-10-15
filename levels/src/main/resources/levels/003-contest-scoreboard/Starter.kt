import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// Dona Lúcia's scoreboard: each team's score with a "+1" button,
// and a "Reset" button.
// Right now the scores are plain variables. The buttons change them,
// but Compose only redraws for state it is watching, so the board
// never changes.
// Tip: var blue by remember { mutableStateOf(0) } gives you a score
// that Compose keeps and watches.
@Composable
fun PlayerScreen() {
    var blue = 0
    var green = 0
    Column(
        Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Contest Scoreboard")
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Blue: $blue")
            Spacer(Modifier.width(16.dp))
            Button(onClick = { blue++ }) { Text("+1 Blue") }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Green: $green")
            Spacer(Modifier.width(16.dp))
            Button(onClick = { green++ }) { Text("+1 Green") }
        }
    }
}
