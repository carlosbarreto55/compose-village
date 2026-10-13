package village.app

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
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
import village.app.ui.home.HomeScreen
import village.app.ui.theme.LocalReducedMotion
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
            VillageTheme { GameScreens(controller) }
        }
    }
}

/** The home street or the level screen, cross-fading in 200 ms; instant under reduced motion (home-screen D1). */
@Composable
fun GameScreens(controller: SpikeController) {
    val spec = if (LocalReducedMotion.current) snap() else tween<Float>(200)
    Crossfade(controller.screen, animationSpec = spec, label = "screen") { screen ->
        when (screen) {
            Screen.HOME -> HomeScreen(controller)
            Screen.PLAY -> SpikeScreen(controller)
        }
    }
}
