package village.levels

import village.preview.Viewport
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class LevelRepositoryTest {
    private fun repo(vararg files: Pair<String, String>) = LevelRepository(LevelFiles { files.toMap()[it] })

    private fun level(folder: String, json: String, starter: String? = "fun PlayerScreen() {}") =
        repo(*listOfNotNull("$folder/level.json" to json, starter?.let { "$folder/Starter.kt" to it }).toTypedArray()).load(folder)

    private fun problems(folder: String, json: String, starter: String? = "fun PlayerScreen() {}"): String {
        val e = assertFailsWith<LevelLoadException> { level(folder, json, starter) }
        assertEquals(folder, e.folder)
        return e.message!!
    }

    private val exists = """{ "type": "exists", "id": "title", "message": "Rosa doesn't see her name.", "node": { "text": "Rosa's Bakery" } }"""

    private fun minimal(id: String = "sign", criteria: String = exists, extra: String = "") =
        """{ "formatVersion": 1, "id": "$id", "resident": "Rosa, the baker", "request": "Make me a sign." $extra, "criteria": [ $criteria ] }"""

    @Test fun minimalLevelGetsDefaults() {
        val level = level("sign", minimal())
        assertEquals("sign", level.id)
        assertEquals("PlayerScreen", level.entryFunction)
        assertEquals("fun PlayerScreen() {}", level.starterCode)
        assertEquals(Viewport(360f, 640f), level.viewport)
        val c = assertIs<Criterion.Exists>(level.criteria.single())
        assertEquals(Selector(text = "Rosa's Bakery"), c.node)
    }

    @Test fun allCriterionTypesDecode() {
        val criteria = """
            $exists,
            { "type": "exists", "id": "buttons", "message": "m", "node": { "role": "Button" }, "count": 2 },
            { "type": "absent", "id": "closed", "message": "m", "node": { "text": "closed", "ignoreCase": true, "contains": true } },
            { "type": "above", "id": "order", "message": "m", "a": { "text": "A" }, "b": { "contentDescription": "logo" }, "toleranceDp": 2 },
            { "type": "leftOf", "id": "row", "message": "m", "a": { "text": "A" }, "b": { "text": "B" } }
        """
        val json = minimal("shop", criteria, """, "entryFunction": "Shop", "starter": "Main.kt", "viewport": { "widthDp": 400, "heightDp": 300 }""")
        val level = repo("shop/level.json" to json, "shop/Main.kt" to "code").load("shop")
        assertEquals("Shop", level.entryFunction)
        assertEquals("code", level.starterCode)
        assertEquals(Viewport(400f, 300f), level.viewport)
        assertEquals(
            listOf(
                Criterion.Exists("title", "Rosa doesn't see her name.", Selector(text = "Rosa's Bakery")),
                Criterion.Exists("buttons", "m", Selector(role = "Button"), count = 2),
                Criterion.Absent("closed", "m", Selector(text = "closed", ignoreCase = true, contains = true)),
                Criterion.Above("order", "m", Selector(text = "A"), Selector(contentDescription = "logo"), toleranceDp = 2f),
                Criterion.LeftOf("row", "m", Selector(text = "A"), Selector(text = "B")),
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
        val message = problems("sign", minimal(criteria = exists.replace("\"message\"", "\"mesage\"")))
        assertContains(message, "criterion 'title': Encountered an unknown key 'mesage'")
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
        assertContains(problems("sign", minimal(criteria = exists.replace("\"exists\"", "\"below\""))), "criterion 'title': unknown type 'below'")
    }

    @Test fun missingRequiredField() {
        assertContains(problems("sign", minimal().replace(", \"request\": \"Make me a sign.\" ", "")), "request")
    }

    @Test fun invalidJson() = assertContains(problems("sign", "{ nope"), "not valid JSON")

    @Test fun emptySelectorBlankMessageAndBadNumbers() {
        val criteria = """
            { "type": "exists", "id": "a", "message": " ", "node": { "ignoreCase": true }, "count": 0 },
            { "type": "above", "id": "b", "message": "m", "a": { "text": "A" }, "b": { "text": "B" }, "toleranceDp": -1 }
        """
        val message = problems("sign", minimal(criteria = criteria, extra = """, "viewport": { "widthDp": 0, "heightDp": 300 }"""))
        assertContains(message, "criterion 'a': message must not be blank")
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
