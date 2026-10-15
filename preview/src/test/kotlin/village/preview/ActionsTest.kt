package village.preview

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import village.api.Console
import village.api.LevelEntry
import java.util.concurrent.TimeUnit
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ActionsTest {
    private val engine = PreviewEngine()

    @AfterTest fun close() = engine.close()

    private fun load(content: @Composable () -> Unit) {
        val entry = object : LevelEntry {
            override val content = content
        }
        engine.load(entry, playerLines = 10).get(30, TimeUnit.SECONDS)
    }

    private fun node(predicate: (SnapshotNode) -> Boolean) =
        assertNotNull(engine.snapshot().get(10, TimeUnit.SECONDS)).nodes().first(predicate)

    @Test fun snapshotNodesHaveIds() {
        load { Column { Text("Rosa's Bakery"); Text("Fresh bread daily") } }
        val ids = assertNotNull(engine.snapshot().get(10, TimeUnit.SECONDS)).nodes().map { it.id }.toList()
        assertTrue(ids.all { it >= 0 }, "$ids")
        assertEquals(ids.size, ids.toSet().size, "ids are unique: $ids")
    }

    @Test fun pressRunsTheHandlerOnceAndReportsWhatItPrinted() {
        // Test code isn't instrumented like player code, so it writes to the player's console directly.
        load { Button(onClick = { Console.out.println("New order!") }) { Text("Order") } }
        val button = node { it.role == "Button" }
        val result = engine.press(button.id).get(10, TimeUnit.SECONDS)
        assertTrue(result.found)
        assertEquals("New order!\n", result.printed)
        assertNotNull(result.snapshot)
    }

    @Test fun snapshotAfterPressShowsTheNewState() {
        load {
            var count by remember { mutableStateOf(0) }
            Column {
                Text("Count: $count")
                Button(onClick = { count++ }) { Text("+") }
            }
        }
        val plus = node { it.role == "Button" }
        val after = assertNotNull(engine.press(plus.id).get(10, TimeUnit.SECONDS).snapshot)
        assertTrue(after.nodes().any { it.texts == listOf("Count: 1") }, "${after.nodes().map { it.texts }.toList()}")
    }

    @Test fun enterTextReplacesTheFieldsText() {
        load {
            var name by remember { mutableStateOf("") }
            Column {
                TextField(value = name, onValueChange = { name = it }, label = { Text("Your name") })
                Text("Hello, $name")
            }
        }
        val field = node { "Your name" in it.texts }
        val result = engine.enterText(field.id, "Rosa").get(10, TimeUnit.SECONDS)
        assertTrue(result.found)
        val after = assertNotNull(result.snapshot)
        assertTrue(after.nodes().any { it.texts == listOf("Hello, Rosa") }, "${after.nodes().map { it.texts }.toList()}")
        assertTrue(after.nodes().any { it.id == field.id && "Rosa" in it.texts }, "the field shows Rosa: ${after.nodes().map { it.texts }.toList()}")
    }

    @Test fun labelStaysInTheFieldAfterTyping() {
        load {
            var query by remember { mutableStateOf("") }
            TextField(value = query, onValueChange = { query = it }, label = { Text("Where to?") })
        }
        val field = node { "Where to?" in it.texts }
        val after = assertNotNull(engine.enterText(field.id, "coral").get(10, TimeUnit.SECONDS).snapshot)
        assertTrue(after.nodes().any { "Where to?" in it.texts && "coral" in it.texts }, "${after.nodes().map { it.texts }.toList()}")
        // A second search finds the field by its label again.
        val again = after.nodes().first { "Where to?" in it.texts }
        assertTrue(engine.enterText(again.id, "Moon Bay").get(10, TimeUnit.SECONDS).found)
    }

    @Test fun anUnboundFieldLeavesTheSameScreen() {
        load { TextField(value = "", onValueChange = { }, label = { Text("Where to?") }) }
        val before = assertNotNull(engine.snapshot().get(10, TimeUnit.SECONDS))
        val field = before.nodes().first { "Where to?" in it.texts }
        val after = assertNotNull(engine.enterText(field.id, "coral").get(10, TimeUnit.SECONDS).snapshot)
        fun SemanticsSnapshot.shape() = nodes().map { Triple(it.texts, it.role, it.bounds) }.toList()
        assertEquals(before.shape(), after.shape())
    }

    @Test fun enterTextIntoPlainTextIsNotFound() {
        load { Text("Your name") }
        val result = engine.enterText(node { "Your name" in it.texts }.id, "Rosa").get(10, TimeUnit.SECONDS)
        assertFalse(result.found)
    }

    @Test fun staleIdIsNotFound() {
        load { Button(onClick = { Console.out.println("pressed") }) { Text("Order") } }
        val result = engine.press(999_999).get(10, TimeUnit.SECONDS)
        assertFalse(result.found)
        assertEquals("", result.printed)
        assertNotNull(result.snapshot)
    }

    @Test fun throwingHandlerIsACrash() {
        load { Button(onClick = { error("bell broke") }) { Text("Order") } }
        val result = engine.press(node { it.role == "Button" }.id).get(10, TimeUnit.SECONDS)
        assertTrue(result.found)
        assertNull(result.snapshot)
        assertEquals("bell broke", engine.state.value.error?.message?.substringAfterLast(": "))
    }
}
