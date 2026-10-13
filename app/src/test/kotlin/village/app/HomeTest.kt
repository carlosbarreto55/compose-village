package village.app

import village.levels.LevelRepository
import village.levels.Profession
import village.levels.Villager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HomeTest {
    private val levels = LevelRepository().all()
    private val hello = levels[0]
    private val openSign = levels[1]

    private fun Progress.completing(id: String) = afterRun(id, passed = true, lines = 6, met = 2)
    private fun HomeModel.state(shop: String) = buildings.single { it.shop == shop }.state

    @Test fun nothingDone() {
        val home = homeModel(levels, Progress())
        assertTrue(home.firstLaunch)
        assertEquals(ShopState.START_HERE, home.state("Rosa's Bakery"))
        assertEquals(ShopState.NOT_YET, home.state("Tom's Flowers"))
        assertEquals(hello, home.current)
        assertTrue(home.openShops.isEmpty())
    }

    @Test fun firstLevelDone() {
        val home = homeModel(levels, Progress().completing(hello.id))
        assertFalse(home.firstLaunch)
        assertEquals(ShopState.OPEN, home.state("Rosa's Bakery"))
        assertEquals(ShopState.UP_NEXT, home.state("Tom's Flowers"))
        assertEquals(openSign, home.current)
        assertEquals(listOf(OpenShop(hello, lines = 6, tries = 1)), home.openShops)
        assertEquals(1 to 2, home.shopsOpen to home.shopsTotal)
        assertEquals("Rosa's Bakery is open. Tom is still waiting for a sign.", home.welcomeLine)
    }

    @Test fun allDone() {
        val home = homeModel(levels, levels.fold(Progress()) { p, l -> p.completing(l.id) })
        assertNull(home.current)
        assertEquals(GameText.EVERY_SHOP_OPEN, home.welcomeLine)
        assertEquals(2, home.shopsOpen)
    }

    @Test fun aDraftIsNotFirstLaunch() {
        val home = homeModel(levels, Progress().withDraft(hello.id, "edited", hello.starterCode))
        assertFalse(home.firstLaunch)
        assertEquals(ShopState.START_HERE, home.state("Rosa's Bakery"))
        assertTrue(home.openShops.isEmpty())
    }

    @Test fun placeholdersComeAfterTheLevelsAndDontCount() {
        val home = homeModel(levels, Progress())
        val soon = home.buildings.filter { it.state == ShopState.COMING_SOON }
        assertEquals(listOf("3 · Restaurant Menu", "4 · Contest Scoreboard", "5 · Ferry Times"), soon.map { it.numbered })
        assertTrue(soon.all { it.level == null })
        assertEquals(2, home.shopsTotal)
    }

    @Test fun aPlaceholderIsDroppedWhenALevelHasItsVillager() {
        val marco = UpcomingShop(Villager("tom", "Tom", Profession.CHEF), "Tom's Kitchen", "Menu")
        val home = homeModel(levels, Progress(), listOf(marco) + Upcoming)
        assertTrue(home.buildings.none { it.shop == "Tom's Kitchen" })
        assertEquals("3 · Restaurant Menu", home.buildings[2].numbered)
    }
}
