import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// Rosa's sign: her shop's name on top, "Fresh bread daily" underneath.
// Tip: a Column stacks its children from top to bottom.
@Composable
fun PlayerScreen() {
    Column(Modifier.padding(16.dp)) {
        Text("Your sign goes here")
    }
}
