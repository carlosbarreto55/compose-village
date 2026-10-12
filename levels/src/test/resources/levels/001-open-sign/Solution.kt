import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PlayerScreen() {
    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("Tom's Flowers")
        Spacer(Modifier.width(12.dp))
        Button(onClick = { println("New order!") }) { Text("Order") }
    }
}
