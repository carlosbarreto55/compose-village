package village.app.spike

// Player files the harness compiles. Line numbers in comments are the ones checks assert on.

/** A ~30-line composable touching state, effects, layout and Material3: the latency and memory workload. */
internal val counterApp = """
    import androidx.compose.foundation.layout.Arrangement
    import androidx.compose.foundation.layout.Column
    import androidx.compose.foundation.layout.Row
    import androidx.compose.foundation.layout.padding
    import androidx.compose.material3.Button
    import androidx.compose.material3.MaterialTheme
    import androidx.compose.material3.Text
    import androidx.compose.runtime.*
    import androidx.compose.ui.Modifier
    import androidx.compose.ui.graphics.Color
    import androidx.compose.ui.unit.dp
    import kotlinx.coroutines.delay

    data class Bread(val name: String, val price: Int)

    @Composable
    fun PlayerScreen() {
        var count by remember { mutableStateOf(0) }
        var ticks by remember { mutableStateOf(0) }
        val breads = listOf(Bread("Baguette", 3), Bread("Rye", 4))
        LaunchedEffect(Unit) { while (true) { delay(1000); ticks++ } }
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Bakery", style = MaterialTheme.typography.headlineSmall, color = Color(0xFF6B4226))
            breads.forEach { Row { Text("${'$'}{it.name}: ${'$'}{it.price} coins") } }
            Button(onClick = { count++ }) { Text("Sold ${'$'}count") }
            Text("Open for ${'$'}ticks s")
        }
    }
""".trimIndent()

/** Counter app split into a header and a counter composable, for recomposition counts (task 6.8). */
internal val heatmapApp = """
    import androidx.compose.foundation.layout.Column
    import androidx.compose.material3.Button
    import androidx.compose.material3.Text
    import androidx.compose.runtime.*

    @Composable
    fun Header() {
        Text("Bakery")
    }

    @Composable
    fun Counter(count: Int, onClick: () -> Unit) {
        Button(onClick = onClick) { Text("Sold ${'$'}count") }
    }

    @Composable
    fun PlayerScreen() {
        var count by remember { mutableStateOf(0) }
        println("composing PlayerScreen")
        Column {
            Header()
            Counter(count) { count++ }
        }
    }
""".trimIndent()

// ---- crashes (task 6.4) ----

/** NullPointerException on line 7, during the first composition. */
internal val crashInComposition = """
    import androidx.compose.material3.Text
    import androidx.compose.runtime.Composable

    @Composable
    fun PlayerScreen() {
        val name: String? = null
        Text(name!!)
    }
""".trimIndent()

/** Throws on line 8 when "Bake" is clicked. */
internal val crashInClick = """
    import androidx.compose.material3.Button
    import androidx.compose.material3.Text
    import androidx.compose.runtime.Composable

    @Composable
    fun PlayerScreen() {
        Button(onClick = {
            error("the oven is on fire")
        }) { Text("Bake") }
    }
""".trimIndent()

/** Throws on line 10, inside a LaunchedEffect after a delay. */
internal val crashInEffect = """
    import androidx.compose.material3.Text
    import androidx.compose.runtime.Composable
    import androidx.compose.runtime.LaunchedEffect
    import kotlinx.coroutines.delay

    @Composable
    fun PlayerScreen() {
        LaunchedEffect(Unit) {
            delay(50)
            throw IllegalArgumentException("no flour")
        }
        Text("Waiting")
    }
""".trimIndent()

// ---- runaway code (task 6.5) ----

/** Loops forever on line 6 during the first composition. */
internal val loopInComposition = """
    import androidx.compose.runtime.Composable

    @Composable
    fun PlayerScreen() {
        var i = 0
        while (true) { i++ }
    }
""".trimIndent()

/** The inner catch swallows the first abort; the outer loop's next tick (line 7) throws it again. */
internal val loopSwallowingAbort = """
    import androidx.compose.runtime.Composable

    @Composable
    fun PlayerScreen() {
        var i = 0
        while (true) {
            try { while (true) { i++ } } catch (t: Throwable) { i = 0 }
        }
    }
""".trimIndent()

/** Grows a list forever on line 7. */
internal val unboundedList = """
    import androidx.compose.runtime.Composable

    @Composable
    fun PlayerScreen() {
        val loaves = mutableListOf<String>()
        while (true) {
            loaves.add("loaf #" + loaves.size)
        }
    }
""".trimIndent()

/** Recurses without a base case on line 4. */
internal val unboundedRecursion = """
    import androidx.compose.material3.Text
    import androidx.compose.runtime.Composable

    fun bake(n: Int): Int = bake(n + 1) + 1

    @Composable
    fun PlayerScreen() {
        Text("Baked ${'$'}{bake(0)}")
    }
""".trimIndent()

/** Loops forever on line 8 in a LaunchedEffect: the dispatched slice is stopped, not composition. */
internal val loopInEffect = """
    import androidx.compose.material3.Text
    import androidx.compose.runtime.*

    @Composable
    fun PlayerScreen() {
        var i by remember { mutableStateOf(0) }
        LaunchedEffect(Unit) {
            while (true) { i++ }
        }
        Text("Busy")
    }
""".trimIndent()

/** About 10 s inside java.math (uninstrumented library code), where no tick ever runs. */
internal val wedgedInLibrary = """
    import androidx.compose.material3.Text
    import androidx.compose.runtime.Composable

    @Composable
    fun PlayerScreen() {
        val digits = java.math.BigInteger.valueOf(7).pow(10_000_000).toString().length
        Text("7^10000000 has ${'$'}digits digits")
    }
""".trimIndent()

// ---- sandbox (task 6.6) ----

/** Ordinary player code that must pass the sandbox: data classes, collections, state, effects, Material3, println. */
internal val allowedCode = """
    import androidx.compose.foundation.layout.Column
    import androidx.compose.material3.Button
    import androidx.compose.material3.Card
    import androidx.compose.material3.Text
    import androidx.compose.runtime.*
    import kotlinx.coroutines.delay

    data class Bread(val name: String, val price: Int)
    enum class Kind { Fresh, Stale }

    fun menu(): List<Bread> = listOf(Bread("Rye", 4), Bread("Baguette", 3))
        .map { if (it.name.startsWith("R")) it.copy(price = it.price + 1) else it }
        .sortedBy { it.price }

    @Composable
    fun PlayerScreen() {
        var sold by remember { mutableStateOf(0) }
        var ticks by remember { mutableStateOf(0) }
        val totals = remember { mutableMapOf<Kind, Int>() }
        LaunchedEffect(Unit) { delay(10); ticks++ }
        println("menu: ${'$'}{menu().joinToString { "${'$'}{it.name}=${'$'}{it.price}" }} ${'$'}{Kind.Fresh}")
        Column {
            Card { Text("Bakery (${'$'}{menu().size} breads)") }
            menu().forEach { b -> Text("${'$'}{b.name}: ${'$'}{b.price}") }
            Button(onClick = { sold++; totals[Kind.Fresh] = sold }) { Text("Sold ${'$'}sold") }
            Text(buildString { append("ticks "); append(ticks) })
        }
    }
""".trimIndent()
