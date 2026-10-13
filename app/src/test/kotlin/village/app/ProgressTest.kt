package village.app

import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProgressTest {
    private val dir = createTempDirectory("progress").toFile().apply { deleteOnExit() }
    private val file = File(dir, "progress.json")

    @Test fun triesCountUntilTheFirstCompletion() {
        val p = Progress().afterRun("a", passed = false, lines = 3, met = 1).afterRun("a", passed = true, lines = 5, met = 2)
            .afterRun("a", passed = false, lines = 9, met = 1).afterRun("a", passed = true, lines = 7, met = 2)
        assertEquals(LevelProgress(completed = true, tries = 2, lines = 5, met = 2), p["a"])
        assertEquals(setOf("a"), p.completed)
    }

    @Test fun metFollowsTheLatestRunAfterCompletion() {
        val p = Progress().afterRun("a", passed = true, lines = 5, met = 3).afterRun("a", passed = false, lines = 6, met = 1)
        assertEquals(LevelProgress(completed = true, tries = 1, lines = 5, met = 1), p["a"])
    }

    @Test fun startOverResetsMetOnly() {
        val p = Progress().afterRun("a", passed = true, lines = 5, met = 3).withStartOver("a")
        assertEquals(LevelProgress(completed = true, tries = 1, lines = 5, met = 0), p["a"])
    }

    @Test fun introSeenRoundTripsAndSurvivesStartOver() {
        val store = ProgressStore(file)
        store.save(Progress().withIntroSeen("a").withStartOver("a"))
        assertTrue(store.load()["a"].introSeen)
    }

    @Test fun fileWithoutMetLoadsWithZero() {
        file.writeText("""{ "version": 1, "levels": { "a": { "completed": true, "tries": 2, "lines": 6 } } }""")
        val loaded = ProgressStore(file).load()
        assertEquals(LevelProgress(completed = true, tries = 2, lines = 6, met = 0, introSeen = false), loaded["a"])
        assertEquals(1, loaded.version)
    }

    @Test fun starterCodeIsNoDraft() {
        val p = Progress().withDraft("a", "edited", starter = "start")
        assertEquals("edited", p["a"].draft)
        val back = p.withDraft("a", "start", starter = "start")
        assertNull(back["a"].draft)
        assertTrue(back.levels.isEmpty(), "a level with nothing to remember leaves no entry")
    }

    @Test fun roundTripKeepsUnknownLevels() {
        val store = ProgressStore(file)
        store.save(Progress(levels = mapOf("gone" to LevelProgress(completed = true, tries = 4))))
        val loaded = store.load().afterRun("a", passed = true, lines = 2, met = 1)
        store.save(loaded)
        assertEquals(setOf("gone", "a"), ProgressStore(file).load().completed)
        assertFalse(File(dir, "progress.json.tmp").exists())
    }

    @Test fun missingFileIsNoProgress() {
        assertEquals(Progress(), ProgressStore(File(dir, "none.json")).load())
        assertEquals(Progress(), ProgressStore(null).load())
    }

    @Test fun corruptFileIsBackedUp() {
        file.writeText("{ not json")
        assertEquals(Progress(), ProgressStore(file).load())
        assertFalse(file.exists())
        assertEquals("{ not json", File(dir, "progress.json.bad").readText())
    }

    @Test fun newerVersionIsBackedUp() {
        file.writeText("""{ "version": 99, "levels": {} }""")
        assertEquals(Progress(), ProgressStore(file).load())
        assertTrue(File(dir, "progress.json.bad").exists())
    }

    @Test fun appDataDirPerPlatform() {
        assertEquals(File("/Users/p/Library/Application Support/Compose Village"), appDataDir("Mac OS X", "/Users/p") { null })
        assertEquals(File("/home/p/.local/share/compose-village"), appDataDir("Linux", "/home/p") { null })
        assertEquals(File("/data/compose-village"), appDataDir("Linux", "/home/p") { if (it == "XDG_DATA_HOME") "/data" else null })
        assertEquals(File("C:/Users/p/AppData/Roaming/Compose Village"), appDataDir("Windows 11", "C:/Users/p") { if (it == "APPDATA") "C:/Users/p/AppData/Roaming" else null })
    }
}
