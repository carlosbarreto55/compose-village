package village.app

import village.levels.Level
import village.levels.Villager

/** A shop on Main Street whose level isn't bundled yet (home-screen D3). App data, not level data. */
data class UpcomingShop(val villager: Villager, val shop: String, val title: String)

/**
 * The placeholder shops after the bundled levels, until their levels exist. Empty while Main Street's
 * five levels are bundled and no later chapter is announced (main-street-levels D8).
 */
val Upcoming: List<UpcomingShop> = emptyList()

/** How a building on the home street looks and what its status says (spec: Shop states). */
enum class ShopState { OPEN, START_HERE, UP_NEXT, NOT_YET, COMING_SOON }

/** One building on the home street: a bundled [level], or a placeholder when [level] is null. */
data class Building(val number: Int, val title: String, val shop: String, val villager: Villager, val state: ShopState, val level: Level?) {
    /** "2 · Open Sign". */
    val numbered: String get() = "$number · $title"
}

/** A completed level on the Open shops card, with its saved stats. */
data class OpenShop(val level: Level, val lines: Int?, val tries: Int)

/** Everything the home screen shows, derived from the levels and saved progress (home-screen D2). */
data class HomeModel(
    /** No level completed and none with saved code: the first-launch form. */
    val firstLaunch: Boolean,
    val buildings: List<Building>,
    /** The first level not completed, in play order; null when every shop is open. */
    val current: Level?,
    val openShops: List<OpenShop>,
    val shopsOpen: Int,
    val shopsTotal: Int,
    /** The line under "Welcome back". */
    val welcomeLine: String,
)

fun homeModel(levels: List<Level>, progress: Progress, upcoming: List<UpcomingShop> = Upcoming): HomeModel {
    val done = levels.filter { progress[it.id].completed }
    val current = levels.firstOrNull { !progress[it.id].completed }
    val firstLaunch = done.isEmpty() && levels.none { progress[it.id].draft != null }
    val buildings = levels.mapIndexed { i, level ->
        val state = when {
            progress[level.id].completed -> ShopState.OPEN
            level == current -> if (done.isEmpty()) ShopState.START_HERE else ShopState.UP_NEXT
            else -> ShopState.NOT_YET
        }
        Building(i + 1, level.title, level.shop, level.villager, state, level)
    }
    val names = levels.map { it.villager.name }.toSet()
    val placeholders = upcoming.filter { it.villager.name !in names }.mapIndexed { i, shop ->
        Building(levels.size + i + 1, shop.title, shop.shop, shop.villager, ShopState.COMING_SOON, null)
    }
    val welcomeLine = when {
        current == null -> GameText.EVERY_SHOP_OPEN
        done.isEmpty() -> GameText.stillWaiting(current.villager.name)
        else -> GameText.openAndWaiting(done.last().shop, current.villager.name)
    }
    return HomeModel(
        firstLaunch = firstLaunch,
        buildings = buildings + placeholders,
        current = current,
        openShops = done.map { OpenShop(it, progress[it.id].lines, progress[it.id].tries) },
        shopsOpen = done.size,
        shopsTotal = levels.size,
        welcomeLine = welcomeLine,
    )
}
