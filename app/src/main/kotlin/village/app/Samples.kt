package village.app

/** Player code the spike window starts with, also used by the warm-up compile and `--selftest`. */
object Samples {
    val BAKERY = """
        import androidx.compose.foundation.layout.*
        import androidx.compose.material3.*
        import androidx.compose.runtime.*
        import androidx.compose.ui.Alignment
        import androidx.compose.ui.Modifier
        import androidx.compose.ui.unit.dp

        @Composable
        fun Header() {
            Text("Main Street Bakery", style = MaterialTheme.typography.headlineSmall)
        }

        @Composable
        fun Counter(count: Int, onSell: () -> Unit) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Loaves sold: ${'$'}count")
                Spacer(Modifier.width(12.dp))
                Button(onClick = onSell) { Text("Sell") }
            }
        }

        @Composable
        fun PlayerScreen() {
            var count by remember { mutableStateOf(0) }
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Header()
                Counter(count) { count++ }
                if (count >= 3) Text("The baker is happy!")
            }
        }
    """.trimIndent() + "\n"
}
