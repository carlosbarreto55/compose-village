package village.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import village.app.ui.SpikeScreen
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    if ("--selftest" in args) exitProcess(selfTest())
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Compose Village: compilation spike",
            state = rememberWindowState(width = 1280.dp, height = 860.dp),
        ) {
            val density = LocalDensity.current.density
            val scope = rememberCoroutineScope()
            val controller = remember { SpikeController(scope, density) }
            LaunchedEffect(controller) { controller.warmUp() }
            DisposableEffect(controller) { onDispose { controller.close() } }
            MaterialTheme(colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()) {
                SpikeScreen(controller)
            }
        }
    }
}
