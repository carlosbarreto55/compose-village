package village.compiler

enum class Severity { ERROR, WARNING }

/** A problem to show in the in-game editor. Lines and columns are 1-based, in the player's file. */
data class Diagnostic(
    val severity: Severity,
    val line: Int,
    val column: Int,
    val message: String,
    val endLine: Int = line,
    val endColumn: Int = column,
    /** Where it came from: the Kotlin compiler, the sandbox check, or the generated wrapper. */
    val source: Source = Source.COMPILER,
) {
    enum class Source { COMPILER, SANDBOX, CONTRACT }

    override fun toString() = "${severity.name.lowercase()} $line:$column [$source] $message"
}

/** Wall-clock timings of one attempt, in milliseconds. */
data class Timings(
    var compileMs: Long = 0,
    var guardMs: Long = 0,
    var loadMs: Long = 0,
) {
    val totalMs get() = compileMs + guardMs + loadMs
}
