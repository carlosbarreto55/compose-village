package village.preview

import village.api.PlayerAbort
import kotlin.test.Test
import kotlin.test.assertEquals

class RuntimeErrorTest {
    private fun frame(method: String, line: Int, file: String = "Player.kt") = StackTraceElement("PlayerKt", method, file, line)

    private fun abortAt(vararg frames: StackTraceElement) =
        PlayerAbort("infinite loop?", PlayerAbort.Kind.TIME).apply { stackTrace = arrayOf(frame("tick", 40, "Guard.kt"), *frames) }

    @Test fun usesTheFirstInFileFrame() {
        val error = RuntimeError.from(abortAt(frame("PlayerScreen", 6)), "Player.kt", playerLines = 11)
        assertEquals(RuntimeError.Kind.INFINITE_LOOP, error.kind)
        assertEquals(6, error.line)
    }

    @Test fun skipsInlinedFramesBeyondTheFile() {
        assertEquals(8, RuntimeError.from(abortAt(frame("getValue", 24), frame("invokeSuspend", 8)), "Player.kt", 11).line)
    }

    // The flaky case from the harness: the abort fired in the entry tick of a delegated var's setter
    // (no line yet), called through Kotlin's synthetic accessor (line 1), from the loop on line 8.
    @Test fun skipsSyntheticAccessors() {
        val t = abortAt(frame("PlayerScreen\$lambda\$2", -1), frame("access\$PlayerScreen\$lambda\$2", 1), frame("invokeSuspend", 8))
        assertEquals(8, RuntimeError.from(t, "Player.kt", 11).line)
    }
}
