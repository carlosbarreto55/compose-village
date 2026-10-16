package village.app.ui

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * city-themes: Colors only come from themes and the cast. A color written into screen code would look the
 * same in every city, so UI sources outside the theme definitions and the villager cast may not have one.
 */
class ColorLiteralTest {
    private val ui = File("src/main/kotlin/village/app/ui")

    /** Folders that define colors: the themes, and the characters' own art. */
    private val owners = listOf("theme/", "villagers/cast/")

    /** Files allowed a literal, with why. */
    private val allowed = mapOf(
        "icons/VillagerIcons.kt" to "the glyphs are masks drawn in black and tinted when drawn",
    )

    private val literal = Regex("""Color\(0x|Color\.(White|Black)\b""")

    @Test fun uiCodeHasNoColorLiterals() {
        assertTrue(ui.isDirectory, "run from the app module: ${ui.absolutePath} not found")
        val stray = ui.walkTopDown().filter { it.isFile && it.extension == "kt" }.flatMap { file ->
            val path = file.relativeTo(ui).invariantSeparatorsPath
            if (owners.any(path::startsWith) || path in allowed) emptySequence()
            else file.readLines().asSequence().withIndex().filter { literal.containsMatchIn(it.value) }.map { "ui/$path:${it.index + 1}: ${it.value.trim()}" }
        }.toList()
        assertTrue(stray.isEmpty(), "color literals outside the themes and the cast; use a theme token instead:\n" + stray.joinToString("\n"))
    }
}
