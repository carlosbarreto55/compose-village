package village.app

import village.levels.CriterionResult
import village.levels.Verdict
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DialogueTest {
    private val line = "Oh, hi! You must be the new sign maker." // 39 characters

    @Test fun typesAtFortyCharactersPerSecond() {
        val t = Typewriter(line, startedAt = 1_000)
        assertEquals(0, t.shown(1_000))
        assertEquals(4, t.shown(1_100))
        assertEquals(20, t.shown(1_500))
        assertEquals(line.length, t.shown(5_000))
        assertTrue(t.typing(1_500))
        assertFalse(t.typing(5_000))
    }

    @Test fun mouthFlapsEvery120msAndClosesWhenDone() {
        val t = Typewriter(line, startedAt = 0)
        assertTrue(t.mouthOpen(0))
        assertTrue(t.mouthOpen(119))
        assertFalse(t.mouthOpen(120))
        assertTrue(t.mouthOpen(240))
        assertFalse(t.mouthOpen(10_000), "closed once the line is out, whatever the parity")
    }

    @Test fun finishShowsTheWholeLine() {
        val t = Typewriter(line, startedAt = 0).finish()
        assertEquals(line.length, t.shown(0))
        assertFalse(t.mouthOpen(0))
    }

    @Test fun advanceFinishesThenMovesOnThenStops() {
        var d = DialogueState.start(listOf("One line.", "Two"), now = 0, reducedMotion = false)
        assertEquals("1 of 2", d.pageLabel)
        d = d.advance(now = 50)
        assertEquals(0, d.page)
        assertFalse(d.typewriter.typing(50), "the first click shows the whole line")
        d = d.advance(now = 60)
        assertEquals(1, d.page)
        assertEquals("2 of 2", d.pageLabel)
        assertEquals(60, d.typewriter.startedAt)
        assertFalse(d.atEnd(60))
        assertTrue(d.atEnd(1_000))
        assertEquals(d, d.advance(1_000), "the last line waits for its closing button")
    }

    @Test fun reducedMotionShowsLinesWholeAndSilent() {
        var d = DialogueState.start(listOf("One.", "Two."), now = 0, reducedMotion = true)
        assertEquals(4, d.typewriter.shown(0))
        assertFalse(d.typewriter.mouthOpen(0))
        d = d.advance(0)
        assertEquals(1, d.page)
        assertTrue(d.atEnd(0))
    }

    @Test fun expressionFollowsTheVerdict() {
        fun result(passed: Boolean) = CriterionResult("a", "d", passed, null, null)
        assertEquals(Expression.NEUTRAL, expressionFor(null))
        assertEquals(Expression.NEUTRAL, expressionFor(Verdict.NotRun(Verdict.NotRun.Reason.DID_NOT_COMPILE)))
        assertEquals(Expression.PUZZLED, expressionFor(Verdict.Evaluated(listOf(result(true), result(false)))))
        assertEquals(Expression.DELIGHTED, expressionFor(Verdict.Evaluated(listOf(result(true)))))
    }
}
