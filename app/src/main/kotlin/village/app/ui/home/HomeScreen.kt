package village.app.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import village.app.GameText
import village.app.HomeModel
import village.app.SpikeController
import village.app.homeModel
import village.app.ui.BuildStatusLabel
import village.app.ui.TWO_COLUMNS_MIN_WIDTH
import village.app.ui.icons.BrandMark
import village.app.ui.numbered
import village.app.ui.theme.Village
import village.app.ui.theme.VillageSpacing

/**
 * The home screen (spec: Home is the first screen): header, the street card, then the first-launch or
 * welcome back cards, two columns when wide and stacked below `TWO_COLUMNS_MIN_WIDTH` (home-screen D6).
 */
@Composable
fun HomeScreen(controller: SpikeController) {
    val home = homeModel(controller.levels, controller.progress)
    Column(Modifier.fillMaxSize().background(Village.colors.ground)) {
        HomeHeader(controller, home)
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val wide = maxWidth >= TWO_COLUMNS_MIN_WIDTH
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
                Column(
                    Modifier.widthIn(max = 1440.dp).fillMaxWidth().padding(horizontal = VillageSpacing.pagePaddingH, vertical = VillageSpacing.pagePaddingV),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    StreetCard(
                        home, enabled = !controller.running,
                        onOpen = { building -> building.level?.let(controller::open) },
                        onSandbox = { controller.open(null) },
                    )
                    if (wide) {
                        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                            Column(Modifier.weight(1f)) { MainCard(controller, home) }
                            Column(Modifier.width(420.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) { SideCards(controller, home) }
                        }
                    } else {
                        MainCard(controller, home)
                        SideCards(controller, home)
                    }
                }
            }
        }
    }
}

/** Your first villager, Pick up where you left off, or every shop open. */
@Composable
private fun MainCard(controller: SpikeController, home: HomeModel) {
    val current = home.current
    when {
        current == null -> AllOpenCard(onSandbox = { controller.open(null) })
        home.firstLaunch -> FirstVillagerCard(current, numbered(controller.levels, current), onStart = { controller.open(current) })
        else -> {
            val saved = controller.progress[current.id]
            PickUpCard(current, numbered(controller.levels, current), saved = saved.draft != null, met = saved.met, onContinue = { controller.open(current) })
        }
    }
}

/** How a level works on first launch; otherwise Open shops (once one is open) and the Sandbox card. */
@Composable
private fun ColumnScope.SideCards(controller: SpikeController, home: HomeModel) {
    if (home.firstLaunch) {
        HowALevelWorksCard(onSandbox = { controller.open(null) })
        return
    }
    if (home.openShops.isNotEmpty()) OpenShopsCard(home.openShops, onVisit = controller::open)
    SandboxCard(onOpen = { controller.open(null) })
}

/** Brand, then "N of M shops open" once anything is saved, then the build status; no Run button. */
@Composable
private fun HomeHeader(controller: SpikeController, home: HomeModel) {
    val c = Village.colors
    Row(
        Modifier.fillMaxWidth().background(c.groundRaised).drawBehind {
            drawLine(c.outline, Offset(0f, size.height - 0.5f), Offset(size.width, size.height - 0.5f), 1.dp.toPx())
        }.padding(horizontal = VillageSpacing.pagePaddingH, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Image(BrandMark, contentDescription = null, Modifier.size(32.dp))
            Text("Compose Village", style = Village.type.wordmark, color = c.accentStrong, maxLines = 1)
        }
        if (!home.firstLaunch) Text(GameText.shopsOpen(home.shopsOpen, home.shopsTotal), style = Village.type.small, color = c.inkMuted)
        Spacer(Modifier.weight(1f))
        BuildStatusLabel(controller.buildStatus)
    }
}
