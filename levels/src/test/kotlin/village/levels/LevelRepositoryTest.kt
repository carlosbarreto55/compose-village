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
        """{ "formatVersion": 1, "id": "$id", "title": "Sign", "villager": { "id": "rosa", "name": "Rosa", "profession": "baker" }, "shop": "Rosa's Bakery", "request": "Make me a sign." $extra, "criteria": [ $criteria ] }"""

    @Test fun minimalLevelGetsDefaults() {
        val level = level("sign", minimal())
        assertEquals("sign", level.id)
        assertEquals("PlayerScreen", level.entryFunction)
        assertEquals("fun PlayerScreen() {}", level.starterCode)
        assertEquals(Viewport(360f, 640f), level.viewport)
        assertEquals(emptyList(), level.panels)
        assertEquals("Sign", level.title)
        assertEquals(Villager("rosa", "Rosa", Profession.BAKER), level.villager)
        assertEquals(emptyList(), level.intro)
        assertEquals(emptyList(), level.celebration)
        assertEquals("Rosa's Bakery", level.shop)
        assertEquals(emptyList(), level.concepts)
        assertNull(level.thanks)
        assertNull(level.targetPng)
        assertEquals(emptyList(), level.actions)
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
        val old = minimal().replace(""""villager": { "id": "rosa", "name": "Rosa", "profession": "baker" }""", """"resident": "Rosa, the baker"""")
        val message = problems("sign", old)
        assertContains(message, "resident")
        assertContains(message, "villager")
    }

    @Test fun villagerWithoutId() =
        assertContains(problems("sign", minimal().replace(""""id": "rosa", """, "")), "villager.id is missing")

    @Test fun badVillagerId() = assertContains(problems("sign", minimal().replace(""""id": "rosa"""", """"id": "Rosa 1"""")), "villager.id 'Rosa 1' must be lowercase")

    @Test fun introAndCelebrationLines() {
        val level = level("sign", minimal(extra = """, "intro": [" Hello! ", "A sign, please."], "celebration": ["Wow!"] """))
        assertEquals(listOf("Hello!", "A sign, please."), level.intro)
        assertEquals(listOf("Wow!"), level.celebration)
    }

    @Test fun tooManyIntroLines() {
        val lines = (1..5).joinToString { "\"Line $it\"" }
        assertContains(problems("sign", minimal(extra = """, "intro": [$lines]""")), "intro has 5 lines; at most 4 are allowed")
    }

    @Test fun tooLongIntroLine() {
        val long = "a".repeat(141)
        assertContains(problems("sign", minimal(extra = """, "intro": ["Hi", "$long"]""")), "intro line 2 is 141 characters; a line can have at most 140")
    }

    @Test fun blankCelebrationLineAndLongThanks() {
        val message = problems("sign", minimal(extra = """, "celebration": [" "], "thanks": "${"b".repeat(141)}" """))
        assertContains(message, "celebration line 1 must not be blank")
        assertContains(message, "thanks is 141 characters")
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

    // ---- the customer's steps (customer-bot) ----

    private val press = """{ "id": "order", "type": "press", "node": { "role": "Button", "text": "Order" } }"""
    private val bell = """{ "type": "prints", "id": "bell", "description": "d", "after": "order", "text": "New order!", "message": "My bell stayed quiet." }"""

    private fun withSteps(steps: String, criteria: String = "$exists, $bell") = minimal(criteria = criteria, extra = """, "actions": [ $steps ]""")

    @Test fun stepsAndPrintsDecode() {
        val type = """{ "id": "name", "type": "enterText", "node": { "text": "Your name" }, "text": "" }"""
        val level = level("sign", withSteps("$press, $type"))
        assertEquals(
            listOf(Action.Press("order", Selector(role = "Button", text = "Order")), Action.EnterText("name", Selector(text = "Your name"), "")),
            level.actions,
        )
        assertEquals(Criterion.Prints("bell", "d", "New order!", message = "My bell stayed quiet.", after = "order"), level.criteria.last())
        assertNull(level.criteria.first().after)
    }

    @Test fun afterAnUnknownStep() {
        val message = problems("sign", withSteps(press, "$exists, ${bell.replace("\"after\": \"order\"", "\"after\": \"ordr\"")}"))
        assertContains(message, "criterion 'bell': after 'ordr' is not one of the level's steps; its steps are order")
    }

    @Test fun afterWithNoSteps() =
        assertContains(problems("sign", minimal(criteria = "$exists, $bell")), "criterion 'bell': after 'order' is not one of the level's steps; the level has no steps")

    @Test fun unknownStepType() {
        val message = problems("sign", withSteps("""{ "id": "order", "type": "click", "node": { "text": "Order" } }""", exists))
        assertContains(message, "step 'order': unknown type 'click'; valid step types are press, enterText")
    }

    @Test fun badStepIdsSelectorsAndText() {
        val steps = """
            { "id": "Order", "type": "press", "node": { "text": "Order" } },
            { "id": "", "type": "press", "node": { "ignoreCase": true } },
            { "id": "twice", "type": "press", "node": { "text": "A" } },
            { "id": "twice", "type": "press", "node": { "text": "B" } }
        """
        val message = problems("sign", withSteps(steps, exists))
        assertContains(message, "step 'Order': id must be lowercase words and digits separated by '-'")
        assertContains(message, "step #2: id must not be blank")
        assertContains(message, "step #2: node must set at least one of text, role, contentDescription")
        assertContains(message, "step id 'twice' is used more than once")
    }

    @Test fun enterTextNeedsText() {
        val message = problems("sign", withSteps("""{ "id": "name", "type": "enterText", "node": { "text": "Your name" } }""", exists))
        assertContains(message, "step 'name': ")
        assertContains(message, "'text'")
    }

    @Test fun blankPrintsText() =
        assertContains(problems("sign", withSteps(press, "$exists, ${bell.replace("New order!", " ")}")), "criterion 'bell': text must not be blank")

    // ---- grouped criteria (main-street-levels) ----

    private fun group(checks: String, extra: String = "") =
        """{ "type": "all", "id": "dishes", "description": "Every dish is on screen", "message": "m" $extra, "checks": [ $checks ] }"""

    @Test fun allDecodesItsChecks() {
        val checks = """{ "type": "exists", "node": { "text": "Spaghetti" } }, { "type": "below", "a": { "text": "Lasagna" }, "b": { "text": "Spaghetti" } }"""
        val level = level("sign", withSteps(press, group(checks, ", \"after\": \"order\"")))
        assertEquals(
            Criterion.All(
                "dishes", "Every dish is on screen", message = "m", after = "order",
                checks = listOf(
                    Criterion.Exists("dishes.1", "Every dish is on screen", Selector(text = "Spaghetti")),
                    Criterion.Below("dishes.2", "Every dish is on screen", Selector(text = "Lasagna"), Selector(text = "Spaghetti")),
                ),
            ),
            level.criteria.single(),
        )
    }

    @Test fun aGroupOfOne() =
        assertContains(
            problems("sign", minimal(criteria = group("""{ "type": "exists", "node": { "text": "Spaghetti" } }"""))),
            "criterion 'dishes': an 'all' criterion needs at least two checks",
        )

    @Test fun aGroupWithoutChecks() =
        assertContains(problems("sign", minimal(criteria = """{ "type": "all", "id": "dishes", "description": "d" }""")), "criterion 'dishes': checks is missing")

    @Test fun aCheckWithItsOwnStep() {
        val checks = """{ "type": "exists", "node": { "text": "08:00" } }, { "type": "absent", "after": "order", "node": { "text": "10:30" } }"""
        assertContains(
            problems("sign", withSteps(press, group(checks))),
            "criterion 'dishes', check 2: grouped checks have no after; they use the criterion's after",
        )
    }

    @Test fun aCheckWithAnIdOrDescription() {
        val checks = """{ "type": "exists", "id": "x", "description": "d", "node": { "text": "A" } }, { "type": "exists", "node": { "text": "B" } }"""
        val message = problems("sign", minimal(criteria = group(checks)))
        assertContains(message, "criterion 'dishes', check 1: grouped checks have no id")
        assertContains(message, "criterion 'dishes', check 1: grouped checks have no description")
    }

    @Test fun nestedGroupsAndPrintsCannotBeGrouped() {
        val checks = """{ "type": "all", "checks": [] }, { "type": "prints", "text": "New order!" }"""
        val message = problems("sign", minimal(criteria = group(checks)))
        assertContains(message, "criterion 'dishes', check 1: type 'all' can't be grouped; a grouped check is one of exists, absent, above, below, leftOf, rightOf")
        assertContains(message, "criterion 'dishes', check 2: type 'prints' can't be grouped")
    }

    @Test fun badSelectorInACheck() {
        val checks = """{ "type": "exists", "node": { "text": "A" } }, { "type": "rightOf", "a": { "ignoreCase": true }, "b": { "text": "A" } }"""
        assertContains(
            problems("sign", minimal(criteria = group(checks))),
            "criterion 'dishes', check 2: a must set at least one of text, role, contentDescription",
        )
    }

    @Test fun typoInACheck() {
        val checks = """{ "type": "exists", "nod": { "text": "A" } }, { "type": "exists", "node": { "text": "B" } }"""
        val message = problems("sign", minimal(criteria = group(checks)))
        assertContains(message, "criterion 'dishes', check 1: ")
        assertContains(message, "nod")
    }
}
