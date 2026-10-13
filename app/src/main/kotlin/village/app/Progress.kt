package village.app

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING

/** One level's saved progress (village-progress D4, D5). */
@Serializable
data class LevelProgress(
    val completed: Boolean = false,
    /** Runs until the first completion, across visits; it stops counting once [completed]. */
    val tries: Int = 0,
    /** The Lines stat of the code that first completed the level. */
    val lines: Int? = null,
    /** The player's latest code, or null when it is the starter code. */
    val draft: String? = null,
)

/** The player's progress per level id, as saved in `progress.json` (village-progress D1, D3). */
@Serializable
data class Progress(val version: Int = VERSION, val levels: Map<String, LevelProgress> = emptyMap()) {
    operator fun get(id: String): LevelProgress = levels[id] ?: LevelProgress()

    /** Ids of the completed levels. */
    val completed: Set<String> get() = levels.filterValues { it.completed }.keys

    /** Records [code] as [id]'s draft; code equal to the [starter] is no draft at all. */
    fun withDraft(id: String, code: String, starter: String): Progress = update(id) { it.copy(draft = code.takeIf { c -> c != starter }) }

    /** Records a Run of [id]: one more try until the first completion, which also saves [lines]. */
    fun afterRun(id: String, passed: Boolean, lines: Int): Progress = update(id) {
        when {
            it.completed -> it
            passed -> it.copy(completed = true, tries = it.tries + 1, lines = lines)
            else -> it.copy(tries = it.tries + 1)
        }
    }

    private fun update(id: String, change: (LevelProgress) -> LevelProgress): Progress {
        val updated = change(get(id))
        return copy(levels = if (updated == LevelProgress()) levels - id else levels + (id to updated))
    }

    companion object {
        const val VERSION = 1
    }
}

/**
 * Reads and writes [Progress] (village-progress D1, D2): a [file], or nowhere when it is null. A missing
 * file is no progress; an unreadable one is kept as `<name>.bad`. Saving is atomic and never throws.
 */
class ProgressStore(val file: File?) {
    fun load(): Progress {
        val file = file?.takeIf { it.exists() } ?: return Progress()
        return try {
            json.decodeFromString<Progress>(file.readText()).also {
                require(it.version <= Progress.VERSION) { "it was saved by a newer version of the game (format ${it.version})" }
            }
        } catch (e: Exception) {
            val backup = File(file.parentFile, "${file.name}.bad")
            System.err.println("Couldn't read saved progress from $file: ${e.message}. Starting fresh; the old file is kept as ${backup.name}.")
            runCatching { Files.move(file.toPath(), backup.toPath(), REPLACE_EXISTING) }
            Progress()
        }
    }

    fun save(progress: Progress) {
        val file = file ?: return
        try {
            file.absoluteFile.parentFile.mkdirs()
            val temp = File(file.absoluteFile.parentFile, "${file.name}.tmp")
            temp.writeText(json.encodeToString(Progress.serializer(), progress))
            Files.move(temp.toPath(), file.toPath(), ATOMIC_MOVE, REPLACE_EXISTING)
        } catch (e: Exception) {
            System.err.println("Couldn't save progress to $file: ${e.message}")
        }
    }

    companion object {
        /** Launch option that overrides where progress is saved: `-Dvillage.progress=<file>`. */
        const val PROPERTY = "village.progress"

        private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

        /** The player's progress file: [PROPERTY], or `progress.json` in the platform's app-data directory. */
        fun default() = ProgressStore(System.getProperty(PROPERTY)?.let(::File) ?: File(appDataDir(), "progress.json"))
    }
}

/** The platform's per-user app-data directory for the game. */
internal fun appDataDir(
    os: String = System.getProperty("os.name"),
    home: String = System.getProperty("user.home"),
    env: (String) -> String? = System::getenv,
): File = when {
    os.startsWith("Mac") -> File(home, "Library/Application Support/Compose Village")
    os.startsWith("Windows") -> File(env("APPDATA")?.takeIf { it.isNotBlank() } ?: "$home/AppData/Roaming", "Compose Village")
    else -> File(env("XDG_DATA_HOME")?.takeIf { it.isNotBlank() } ?: "$home/.local/share", "compose-village")
}
