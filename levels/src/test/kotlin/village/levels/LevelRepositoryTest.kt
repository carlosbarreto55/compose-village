package village.levels

import village.preview.Viewport
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull

class LevelRepositoryTest {
    private fun repo(vararg files: Pair<String, String>) = LevelRepository(LevelFiles { files.toMap()[it] })

    private fun level(folder: String, json: String, starter: String? = "fun PlayerScreen() {}") =
        repo(*listOfNotNull("$folder/level.json" to json, starter?.let { "$folder/Starter.kt" to it }).toTypedArray()).load(folder)

    private fun problems(folder: String, json: String, starter: String? = "fun PlayerScreen() {}"): String {
        val e = assertFailsWith<LevelLoadException> { level(folder, json, starter) }
        assertEquals(folder, e.folder)
        return e.message!!
    }

    private val exists = """{ "type": "exists", "id": "title", "description": "The text \"Rosa's Bakery\" is on screen", "node": { "text": "Rosa's Bakery" } }"""

    private fun minimal(id: String = "sign", criteria: String = exists, extra: String = "") =
        """{ "formatVersion": 1, "id": "$id", "title": "Sign", "villager": { "name": "Rosa", "profession": "baker" }, "shop": "Rosa's Bakery", "request": "Make me a sign." $extra, "criteria": [ $criteria ] }"""

    @Test fun minimalLevelGetsDefaults() {
        val level = level("sign", minimal())
        assertEquals("sign", level.id)
        assertEquals("PlayerScreen", level.entryFunction)
        assertEquals("fun PlayerScreen() {}", level.starterCode)
        assertEquals(Viewport(360f, 640f), level.viewport)
        assertEquals(emptyList(), level.panels)
        assertEquals("Sign", level.title)
        assertEquals(Villager("Rosa", Profession.BAKER), level.villager)
        assertEquals("Rosa's Bakery", level.shop)
        assertEquals(emptyList(), level.concepts)
        assertNull(level.thanks)
        assertNull(level.targetPng)
        val c = assertIs<Criterion.Exists>(level.criteria.single())
        assertEquals(Selector(text = "Rosa's Bakery"), c.node)
        assertEquals("The text \"Rosa's Bakery\" is on screen", c.description)
        assertNull(c.message)
    }

    @Test fun allCriterionTypesDecode() {
        val criteria = """
            $exists,
            { "type": "exists", "id": "buttons", "description": "d", "node": { "role": "Button" }, "count": 2, "message": "m" },
            { "type": "absent", "id": "closed", "description": "d", "node": { "text": "closed", "ignoreCase": true, "contains": true } },
            { "type": "above", "id": "order", "description": "d", "a": { "text": "A" }, "b": { "contentDescription": "logo" }, "toleranceDp": 2 },
            { "type": "below", "id": "under", "description": "d", "a": { "text": "B" }, "b": { "text": "A" } },
            { "type": "leftOf", "id": "row", "description": "d", "a": { "text": "A" }, "b": { "text": "B" } },
            { "type": "rightOf", "id": "after", "description": "d", "a": { "text": "B" }, "b": { "text": "A" } }
        """
        val json = minimal("shop", criteria, """, "entryFunction": "Shop", "starter": "Main.kt", "viewport": { "widthDp": 400, "heightDp": 300 }, "panels": ["console", "recompositions"]""")
        val level = repo("shop/level.json" to json, "shop/Main.kt" to "code").load("shop")
        assertEquals("Shop", level.entryFunction)
        assertEquals("code", level.starterCode)
        assertEquals(Viewport(400f, 300f), level.viewport)
        assertEquals(listOf(Panel.CONSOLE, Panel.RECOMPOSITIONS), level.panels)
        assertEquals(
            listOf(
                Criterion.Exists("title", "The text \"Rosa's Bakery\" is on screen", Selector(text = "Rosa's Bakery")),
                Criterion.Exists("buttons", "d", Selector(role = "Button"), count = 2, message = "m"),
                Criterion.Absent("closed", "d", Selector(text = "closed", ignoreCase = true, contains = true)),
                Criterion.Above("order", "d", Selector(text = "A"), Selector(contentDescription = "logo"), toleranceDp = 2f),
                Criterion.Below("under", "d", Selector(text = "B"), Selector(text = "A")),
                Criterion.LeftOf("row", "d", Selector(text = "A"), Selector(text = "B")),
                Criterion.RightOf("after", "d", Selector(text = "B"), Selector(text = "A")),
            ),
            level.criteria,
        )
    }

    @Test fun duplicateCriterionIds() {
        val message = problems("sign", minimal(criteria = "$exists, $exists"))
        assertContains(message, "criterion id 'title' is used more than once")
    }

    @Test fun folderAndIdDisagree() {
        val message = problems("001-open-sign", minimal(id = "001-open-shop"))
        assertContains(message, "'001-open-shop' must equal its folder name '001-open-sign'")
    }

    @Test fun typoInFieldName() {
        val message = problems("sign", minimal(criteria = exists.replace("\"description\"", "\"descripton\"")))
        assertContains(message, "criterion 'title': Encountered an unknown key 'descripton'")
    }

    @Test fun missingDescription() {
        val message = problems("sign", minimal(criteria = """{ "type": "exists", "id": "title", "message": "m", "node": { "text": "A" } }"""))
        assertContains(message, "criterion 'title'")
        assertContains(message, "description")
    }

    @Test fun blankDescriptionAndMessage() {
        val message = problems("sign", minimal(criteria = """{ "type": "exists", "id": "title", "description": " ", "message": "", "node": { "text": "A" } }"""))
        assertContains(message, "criterion 'title': description must not be blank")
        assertContains(message, "criterion 'title': message must not be blank")
    }

    @Test fun unknownPanelListsValidPanels() {
        val message = problems("sign", minimal(extra = """, "panels": ["consol"]"""))
        assertContains(message, "panel 'consol' is unknown; valid panels are console, recompositions")
    }

    @Test fun conceptsAndThanks() {
        val level = level("sign", minimal(extra = """, "concepts": ["Text", "Column"], "thanks": "Lovely!" """))
        assertEquals(listOf("Text", "Column"), level.concepts)
        assertEquals("Lovely!", level.thanks)
    }

    @Test fun repeatedOrBlankConceptAndBlankThanks() {
        val message = problems("sign", minimal(extra = """, "concepts": ["Row", "Row", " "], "thanks": "" """))
        assertContains(message, "concept 'Row' is listed more than once")
        assertContains(message, "concepts must not contain blank names")
        assertContains(message, "thanks must not be blank")
    }

    @Test fun unknownProfessionListsValidProfessions() {
        val message = problems("sign", minimal().replace("\"baker\"", "\"gardener\""))
        assertContains(message, "villager.profession 'gardener' is unknown; valid professions are baker, florist, chef, teacher, boat-captain")
    }

    @Test fun oldResidentFieldIsRejected() {
        val old = minimal().replace(""""villager": { "name": "Rosa", "profession": "baker" }""", """"resident": "Rosa, the baker"""")
        val message = problems("sign", old)
        assertContains(message, "resident")
        assertContains(message, "villager")
    }

    @Test fun repeatedPanel() = assertContains(problems("sign", minimal(extra = """, "panels": ["console", "console"]""")), "panel 'console' is listed more than once")

    @Test fun targetBytesAreLoaded() {
        val files = object : LevelFiles {
            val text = mapOf("sign/level.json" to minimal(), "sign/Starter.kt" to "code")
            override fun read(path: String) = text[path]
            override fun readBytes(path: String) = if (path == "sign/target.png") byteArrayOf(1, 2, 3) else null
        }
        assertContentEquals(byteArrayOf(1, 2, 3), LevelRepository(files).load("sign").targetPng)
    }

    @Test fun firstLevelListingAPanelUnlocksIt() {
        fun lvl(id: String, panels: String) = "$id/level.json" to minimal(id, extra = """, "panels": $panels""")
        val repo = repo(
            "index.json" to """["a", "b", "c", "d"]""",
            lvl("a", "[]"), "a/Starter.kt" to "",
            lvl("b", """["console"]"""), "b/Starter.kt" to "",
            lvl("c", """["console", "recompositions"]"""), "c/Starter.kt" to "",
            lvl("d", """["recompositions"]"""), "d/Starter.kt" to "",
        )
        assertEquals(mapOf(Panel.CONSOLE to "b", Panel.RECOMPOSITIONS to "c"), repo.unlocks())
    }

    @Test fun unknownRoleListsValidRoles() {
        val message = problems("sign", minimal(criteria = exists.replace(""""text": "Rosa's Bakery"""", """"role": "Buton"""")))
        assertContains(message, "role 'Buton' is unknown")
        assertContains(message, "Button, Checkbox, Switch")
    }

    @Test fun unsupportedFormatVersion() {
        val message = problems("sign", minimal().replace("\"formatVersion\": 1", "\"formatVersion\": 2"))
        assertContains(message, "supports format version 1")
    }

    @Test fun unknownCriterionType() {
        assertContains(problems("sign", minimal(criteria = exists.replace("\"exists\"", "\"beneath\""))), "criterion 'title': unknown type 'beneath'")
    }

    @Test fun missingRequiredField() {
        assertContains(problems("sign", minimal().replace(", \"request\": \"Make me a sign.\" ", "")), "request")
    }

    @Test fun invalidJson() = assertContains(problems("sign", "{ nope"), "not valid JSON")

    @Test fun emptySelectorBlankMessageAndBadNumbers() {
        val criteria = """
            { "type": "exists", "id": "a", "description": " ", "node": { "ignoreCase": true }, "count": 0 },
            { "type": "rightOf", "id": "b", "description": "d", "a": { "text": "A" }, "b": { "text": "B" }, "toleranceDp": -1 }
        """
        val message = problems("sign", minimal(criteria = criteria, extra = """, "viewport": { "widthDp": 0, "heightDp": 300 }"""))
        assertContains(message, "criterion 'a': description must not be blank")
        assertContains(message, "criterion 'a': node must set at least one of text, role, contentDescription")
        assertContains(message, "criterion 'a': count must be at least 1")
        assertContains(message, "criterion 'b': toleranceDp must not be negative")
        assertContains(message, "viewport must be positive")
    }

    @Test fun missingStarter() = assertContains(problems("sign", minimal(), starter = null), "starter file 'Starter.kt' not found")

    @Test fun badIdFormat() = assertContains(problems("Sign_1", minimal(id = "Sign_1")), "lowercase words and digits")

    @Test fun indexListsIdsInOrderAndRejectsDuplicates() {
        assertEquals(listOf("b", "a"), repo("index.json" to """["b", "a"]""").ids())
        assertContains(assertFailsWith<LevelLoadException> { repo("index.json" to """["a", "a"]""").ids() }.message!!, "more than once: a")
        assertFailsWith<LevelLoadException> { repo("index.json" to """{"levels": []}""").ids() }
    }
}
