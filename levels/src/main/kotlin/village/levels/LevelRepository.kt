package village.levels

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import village.preview.SnapshotNode
import village.preview.Viewport
import java.io.File

/** Reads a level data file by its path under the levels root, e.g. `000-hello-main-street/level.json`. */
fun interface LevelFiles {
    /** The file's text, or null if it does not exist. */
    fun read(path: String): String?

    /** The file's bytes, or null if it does not exist. Text-only sources (test fixtures) have no binary files. */
    fun readBytes(path: String): ByteArray? = null

    companion object {
        /** The levels bundled with the game, under the `levels/` resource root. */
        val BUNDLED: LevelFiles = object : LevelFiles {
            private fun url(path: String) = LevelFiles::class.java.classLoader.getResource("levels/$path")
            override fun read(path: String) = url(path)?.readText()
            override fun readBytes(path: String) = url(path)?.readBytes()
        }

        fun directory(root: File): LevelFiles = object : LevelFiles {
            private fun file(path: String) = File(root, path).takeIf { it.isFile }
            override fun read(path: String) = file(path)?.readText()
            override fun readBytes(path: String) = file(path)?.readBytes()
        }
    }
}

/**
 * Loads levels from data files (design D3/D4): `index.json` lists level ids in play order, and each
 * level's folder holds `level.json` and its starter code. Loading is strict: unknown fields, missing
 * fields and inconsistent data are errors, so typos in level files never reach players.
 */
class LevelRepository(private val files: LevelFiles = LevelFiles.BUNDLED) {
    /** Level ids in play order. */
    fun ids(): List<String> {
        val text = files.read(INDEX) ?: throw LevelLoadException(INDEX, listOf("$INDEX not found"))
        val ids = try {
            json.decodeFromString<List<String>>(text)
        } catch (e: SerializationException) {
            throw LevelLoadException(INDEX, listOf("must be a JSON array of level ids: ${e.message}"))
        } catch (e: IllegalArgumentException) {
            throw LevelLoadException(INDEX, listOf("must be a JSON array of level ids: ${e.message}"))
        }
        val duplicates = ids.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
        if (duplicates.isNotEmpty()) throw LevelLoadException(INDEX, listOf("lists these ids more than once: ${duplicates.joinToString()}"))
        return ids
    }

    fun all(): List<Level> = ids().map(::load)

    /** Each panel mapped to the first level, in play order, that lists it: that level unlocks it. */
    fun unlocks(): Map<Panel, String> = panelUnlocks(all())

    /** Loads and validates the level in folder [folder]; throws [LevelLoadException] listing every problem found. */
    fun load(folder: String): Level {
        val problems = mutableListOf<String>()
        fun invalid(): Nothing = throw LevelLoadException(folder, problems)

        val text = files.read("$folder/$LEVEL_FILE") ?: run { problems += "$folder/$LEVEL_FILE not found"; invalid() }
        // Check the version before decoding: a newer format may have fields this game doesn't know.
        val tree = try {
            json.parseToJsonElement(text).jsonObject
        } catch (e: SerializationException) {
            problems += "$LEVEL_FILE is not valid JSON: ${e.message}"; invalid()
        } catch (e: IllegalArgumentException) {
            problems += "$LEVEL_FILE must be a JSON object"; invalid()
        }
        val version = (tree["formatVersion"] as? JsonPrimitive)?.intOrNull
        if (version != FORMAT_VERSION) {
            problems += if (version == null) "formatVersion is missing or not a number" else
                "formatVersion is $version, but this game supports format version $FORMAT_VERSION"
            invalid()
        }
        val file = decode(LevelFile.serializer(), tree, problems) ?: invalid()
        // Criteria one at a time, so an error names the criterion (decoding a tree has no useful path).
        val criteria = file.criteria.mapIndexedNotNull { i, element ->
            val obj = element as? JsonObject
            val id = (obj?.get("id") as? JsonPrimitive)?.contentOrNull
            val name = if (id.isNullOrBlank()) "criterion #${i + 1}" else "criterion '$id'"
            val type = (obj?.get("type") as? JsonPrimitive)?.contentOrNull
            when {
                obj == null -> { problems += "$name must be a JSON object"; null }
                type !in CRITERION_TYPES -> { problems += "$name: unknown type '$type'; valid types are ${CRITERION_TYPES.joinToString()}"; null }
                else -> decode(Criterion.serializer(), obj, problems, "$name: ")
            }
        }
        if (problems.isNotEmpty()) invalid()

        problems += validate(folder, file, criteria)
        val panels = file.panels.mapNotNull { name ->
            Panel.fromJson(name) ?: run { problems += "panel '$name' is unknown; valid panels are ${Panel.entries.joinToString { it.jsonName }}"; null }
        }
        file.panels.groupingBy { it }.eachCount().filterValues { it > 1 }.keys.forEach { problems += "panel '$it' is listed more than once" }
        val starter = files.read("$folder/${file.starter}")
        if (starter == null) problems += "starter file '${file.starter}' not found in $folder/"
        if (problems.isNotEmpty()) invalid()

        return Level(
            id = file.id, resident = file.resident.trim(), request = file.request.trim(), entryFunction = file.entryFunction,
            starterCode = starter!!, viewport = Viewport(file.viewport.widthDp, file.viewport.heightDp), criteria = criteria,
            panels = panels, targetPng = files.readBytes("$folder/$TARGET_FILE"),
        )
    }

    /**
     * Decodes [element], or adds the error to [problems] and returns null. Only the error's first line
     * is kept: the rest is kotlinx advice and a copy of the input.
     */
    private fun <T : Any> decode(serializer: KSerializer<T>, element: JsonElement, problems: MutableList<String>, prefix: String = ""): T? =
        try {
            json.decodeFromJsonElement(serializer, element)
        } catch (e: SerializationException) {
            problems += prefix + e.message.orEmpty().lineSequence().first().removeSuffix(" at path: $"); null
        } catch (e: IllegalArgumentException) {
            problems += prefix + e.message.orEmpty().lineSequence().first(); null
        }

    private fun validate(folder: String, file: LevelFile, criteria: List<Criterion>) = buildList {
        if (!ID.matches(file.id)) add("id '${file.id}' must be lowercase words and digits separated by '-'")
        if (file.id != folder) add("id '${file.id}' must equal its folder name '$folder'")
        if (file.resident.isBlank()) add("resident must not be blank")
        if (file.request.isBlank()) add("request must not be blank")
        if (!IDENTIFIER.matches(file.entryFunction)) add("entryFunction '${file.entryFunction}' is not a Kotlin function name")
        if (file.viewport.widthDp <= 0 || file.viewport.heightDp <= 0) {
            add("viewport must be positive, was ${file.viewport.widthDp} x ${file.viewport.heightDp} dp")
        }
        if (criteria.isEmpty()) add("criteria must list at least one criterion")
        criteria.groupingBy { it.id }.eachCount().filterValues { it > 1 }.keys.forEach { add("criterion id '$it' is used more than once") }
        criteria.forEachIndexed { i, c ->
            val name = if (c.id.isBlank()) "criterion #${i + 1}" else "criterion '${c.id}'"
            if (c.id.isBlank()) add("$name: id must not be blank")
            if (c.description.isBlank()) add("$name: description must not be blank")
            if (c.message?.isBlank() == true) add("$name: message must not be blank (leave it out instead)")
            when (c) {
                is Criterion.Exists -> {
                    addAll(validate(name, "node", c.node))
                    if (c.count != null && c.count < 1) add("$name: count must be at least 1 (use 'absent' for none)")
                }
                is Criterion.Absent -> addAll(validate(name, "node", c.node))
                is Criterion.Order -> {
                    addAll(validate(name, "a", c.a) + validate(name, "b", c.b))
                    if (c.toleranceDp < 0) add("$name: toleranceDp must not be negative")
                }
            }
        }
    }

    private fun validate(criterion: String, field: String, s: Selector) = buildList {
        if (s.text == null && s.role == null && s.contentDescription == null) {
            add("$criterion: $field must set at least one of text, role, contentDescription")
        }
        if (s.text?.isBlank() == true) add("$criterion: $field.text must not be blank")
        if (s.contentDescription?.isBlank() == true) add("$criterion: $field.contentDescription must not be blank")
        if (s.role != null && s.role !in SnapshotNode.ROLE_NAMES) {
            add("$criterion: $field.role '${s.role}' is unknown; valid roles are ${SnapshotNode.ROLE_NAMES.joinToString()}")
        }
    }

    companion object {
        const val FORMAT_VERSION = 1
        const val INDEX = "index.json"
        const val LEVEL_FILE = "level.json"
        /** Rendered at build time from the level's reference solution (design D1). */
        const val TARGET_FILE = "target.png"
        private val ID = Regex("^[a-z0-9]+(-[a-z0-9]+)*$")
        private val IDENTIFIER = Regex("^[A-Za-z_][A-Za-z0-9_]*$")
        private val CRITERION_TYPES = listOf("exists", "absent", "above", "below", "leftOf", "rightOf")

        // Strict: unknown keys are errors (a typo must not silently drop a field). "type" picks the criterion.
        private val json = Json { ignoreUnknownKeys = false; classDiscriminator = "type" }
    }
}

/** `level.json` as written by level authors. */
@Serializable
private class LevelFile(
    val formatVersion: Int,
    val id: String,
    val resident: String,
    val request: String,
    val entryFunction: String = "PlayerScreen",
    val starter: String = "Starter.kt",
    val viewport: ViewportFile = ViewportFile(),
    /** Extra panels by name; checked in [LevelRepository.load] so an unknown name lists the valid ones. */
    val panels: List<String> = emptyList(),
    /** Decoded one by one into [Criterion]s, see [LevelRepository.load]. */
    val criteria: List<JsonElement>,
)

@Serializable
private class ViewportFile(val widthDp: Float = 360f, val heightDp: Float = 640f)
