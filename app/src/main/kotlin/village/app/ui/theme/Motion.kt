package village.app.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import java.util.concurrent.TimeUnit

/** Whether to skip the shake and the pulse rings (design D11). Color changes and fades always run. */
val LocalReducedMotion = staticCompositionLocalOf { false }

object ReducedMotion {
    /** `-Dvillage.reducedMotion=true|false`, which overrides the system setting. */
    const val PROPERTY = "village.reducedMotion"

    /** The launch override if set, else macOS "Reduce motion"; false where it can't be read. Read once. */
    val system: Boolean by lazy {
        System.getProperty(PROPERTY)?.toBooleanStrictOrNull() ?: macOsReduceMotion()
    }

    private fun macOsReduceMotion(): Boolean {
        if (!System.getProperty("os.name").orEmpty().startsWith("Mac")) return false
        return runCatching {
            val process = ProcessBuilder("defaults", "read", "com.apple.universalaccess", "reduceMotion")
                .redirectErrorStream(true).start()
            if (!process.waitFor(500, TimeUnit.MILLISECONDS)) { process.destroy(); return false }
            process.inputStream.bufferedReader().readText().trim() == "1"
        }.getOrDefault(false)
    }
}
