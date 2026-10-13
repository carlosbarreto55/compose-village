package village.app

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
import village.app.ui.theme.ReducedMotion
import village.app.ui.theme.VillageTheme
import java.awt.Dimension
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    if ("--selftest" in args) exitProcess(selfTest())
    ReducedMotion.system // read the OS setting before the first frame
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Compose Village",
            state = rememberWindowState(width = 1280.dp, height = 860.dp),
        ) {
            LaunchedEffect(Unit) { window.minimumSize = Dimension(900, 640) } // AWT points = dp at the window's scale
            val density = LocalDensity.current.density
            val scope = rememberCoroutineScope()
            val controller = remember { SpikeController(scope, density, ProgressStore.default()) }
            LaunchedEffect(controller) { controller.warmUp() }
            DisposableEffect(controller) { onDispose { controller.close() } }
            VillageTheme { SpikeScreen(controller) }
        }
    }
}
