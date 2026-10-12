import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// Tom's sign: his shop's name, with an "Order" button to the right of it.
// Tip: a Row places its children from left to right, and Button(onClick = { ... }) { Text("...") } makes a button.
// Anything you println(...) shows up in the Console.
@Composable
fun PlayerScreen() {
    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("Tom's Flowers")
    }
}
