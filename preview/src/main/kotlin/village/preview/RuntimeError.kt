package village.preview

import village.api.PlayerAbort

/** A failure of the player's running code, in terms the editor can show. */
data class RuntimeError(
    val kind: Kind,
    val message: String,
    /** 1-based line in the player's file, or null when no stack frame points into it. */
    val line: Int?,
    /** Simple name of the exception, e.g. `NullPointerException`. */
    val type: String,
) {
    enum class Kind { EXCEPTION, INFINITE_LOOP, TOO_MUCH_MEMORY, STACK_OVERFLOW, STOPPED, RESTARTED }

    companion object {
        /**
         * Uses the first stack frame in [playerFile] whose line is inside the file. Frames with a
         * larger line number come from code inlined into the player's classes.
         */
        fun from(t: Throwable, playerFile: String, playerLines: Int): RuntimeError {
            val chain = generateSequence(t) { it.cause }.take(10).toList()
            val cause = chain.firstOrNull { it is PlayerAbort || it is StackOverflowError || it is OutOfMemoryError }
                ?: chain.firstOrNull { lineOf(it, playerFile, playerLines) != null }
                ?: t
            val line = lineOf(cause, playerFile, playerLines)
            val type = cause::class.java.simpleName
            return when {
                cause is PlayerAbort -> RuntimeError(
                    when (cause.kind) {
                        PlayerAbort.Kind.TIME -> Kind.INFINITE_LOOP
                        PlayerAbort.Kind.ALLOCATION -> Kind.TOO_MUCH_MEMORY
                        PlayerAbort.Kind.HOST -> Kind.STOPPED
                    },
                    cause.message ?: "Your code was stopped.", line, type,
                )
                cause is StackOverflowError -> RuntimeError(
                    Kind.STACK_OVERFLOW,
                    "Your code called itself too many times (StackOverflowError). Is a recursive function missing its base case?",
                    line, type,
                )
                cause is OutOfMemoryError -> RuntimeError(Kind.TOO_MUCH_MEMORY, "Your code ran out of memory.", line, type)
                else -> RuntimeError(Kind.EXCEPTION, listOfNotNull(type, cause.message).joinToString(": "), line, type)
            }
        }

        private fun lineOf(t: Throwable, playerFile: String, playerLines: Int): Int? =
            t.stackTrace.firstOrNull { it.fileName == playerFile && it.lineNumber in 1..playerLines }?.lineNumber
    }
}
