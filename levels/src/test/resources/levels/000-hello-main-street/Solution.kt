import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PlayerScreen() {
    Column(Modifier.padding(16.dp)) {
        Text("Rosa's Bakery", style = MaterialTheme.typography.headlineMedium)
        Text("Fresh bread daily")
    }
}
