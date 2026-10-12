package village.preview

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import village.api.LevelEntry
import java.util.concurrent.TimeUnit
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail

class SnapshotTest {
    private val engine = PreviewEngine()

    @AfterTest fun close() = engine.close()

    private fun entry(content: @Composable () -> Unit) = object : LevelEntry {
        override val content = content
    }

    private fun load(viewport: Viewport? = null, content: @Composable () -> Unit) =
        engine.load(entry(content), playerLines = 10, viewport = viewport).get(30, TimeUnit.SECONDS)

    private fun snapshot() = engine.snapshot().get(10, TimeUnit.SECONDS)

    private val sign: @Composable () -> Unit = {
        Column {
            Text("Rosa's Bakery")
            Text("Fresh bread daily")
        }
    }

    @Test fun columnTextsAreInOrder() {
        load(content = sign)
        val snapshot = assertNotNull(snapshot())
        val title = snapshot.nodes().single { it.texts == listOf("Rosa's Bakery") }
        val subtitle = snapshot.nodes().single { it.texts == listOf("Fresh bread daily") }
        assertTrue(title.bounds.bottom <= subtitle.bounds.top, "$title should be above $subtitle")
        assertTrue(title.bounds.width > 0 && title.bounds.height > 0)
        assertEquals(Viewport(360f, 640f), snapshot.viewport)
    }

    @Test fun buttonIsOneMergedNodeWithRoleAndText() {
        load { Button(onClick = {}) { Text("Order") } }
        val button = assertNotNull(snapshot()).nodes().single { it.role != null }
        assertEquals("Button", button.role)
        assertEquals(listOf("Order"), button.texts)
    }

    @Test fun noSnapshotAfterCrash() {
        val state = load { error("boom") }
        assertNotNull(state.error)
        assertNull(snapshot())
    }

    @Test fun noSnapshotBeforeLoad() = assertNull(snapshot())

    @Test fun snapshotOutlivesTheAttempt() {
        load(content = sign)
        val snapshot = assertNotNull(snapshot())
        engine.unload().get(10, TimeUnit.SECONDS)
        assertEquals(2, snapshot.nodes().count { it.texts.isNotEmpty() })
        assertPlainData(snapshot)
    }

    @Test fun viewportSetsTheSceneSize() {
        PreviewEngine(density = 2f).use { hiDpi ->
            val state = hiDpi.load(entry(sign), 10, viewport = Viewport(400f, 300f)).get(30, TimeUnit.SECONDS)
            val frame = assertNotNull(state.frame)
            assertEquals(800 to 600, frame.width to frame.height)
            assertEquals(Viewport(400f, 300f), hiDpi.snapshot().get(10, TimeUnit.SECONDS)?.viewport)
        }
    }

    /** Only strings, numbers, lists and our own snapshot classes: nothing that can pin a class loader. */
    private fun assertPlainData(value: Any?, path: String = "snapshot") {
        when (value) {
            null, is String, is Number, is Boolean -> {}
            is List<*> -> value.forEachIndexed { i, v -> assertPlainData(v, "$path[$i]") }
            else -> {
                if (value.javaClass.packageName != "village.preview") fail("$path is a ${value.javaClass.name}")
                value.javaClass.declaredFields.filterNot { java.lang.reflect.Modifier.isStatic(it.modifiers) }.forEach {
                    it.isAccessible = true
                    assertPlainData(it.get(value), "$path.${it.name}")
                }
            }
        }
    }
}
