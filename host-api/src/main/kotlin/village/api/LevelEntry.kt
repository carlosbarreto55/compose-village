package village.api

import androidx.compose.runtime.Composable

/**
 * The only contract between the game and a compiled player attempt.
 *
 * The game never calls player functions reflectively (compiled composables carry synthetic
 * `$composer`/`$changed` parameters). Instead a generated wrapper object implements this
 * interface, is compiled alongside the player's file, and is cast to it by the host.
 * This interface is loaded by the host ClassLoader, so the cast works across loaders.
 */
interface LevelEntry {
    val content: @Composable () -> Unit
}
