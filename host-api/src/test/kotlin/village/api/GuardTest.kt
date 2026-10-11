package village.api

import kotlin.concurrent.thread
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GuardTest {
    @AfterTest fun restore() {
        Guard.endSlice()
        Guard.beginSlice() // clears any sticky abort
        Guard.endSlice()
        Guard.timeBudgetMillis = 1_000
        Guard.allocBudgetBytes = 512L * 1024 * 1024
    }

    /** What instrumented player code does in a loop: tick on every iteration. */
    private fun spin(): Nothing { while (true) Guard.tick() }

    @Test fun timeBudgetStopsALoop() {
        Guard.timeBudgetMillis = 100
        val start = System.nanoTime()
        val abort = assertFailsWith<PlayerAbort> { Guard.slice { spin() } }
        val elapsed = (System.nanoTime() - start) / 1_000_000
        assertEquals(PlayerAbort.Kind.TIME, abort.kind)
        assertTrue(elapsed in 100..150, "aborted after $elapsed ms")
    }

    @Test fun allocationBudgetStopsGrowth() {
        Guard.allocBudgetBytes = 16L * 1024 * 1024
        val list = ArrayList<ByteArray>()
        val abort = assertFailsWith<PlayerAbort> {
            Guard.slice { while (true) { list += ByteArray(1024); Guard.tick() } }
        }
        assertEquals(PlayerAbort.Kind.ALLOCATION, abort.kind)
        assertTrue(list.size < 32 * 1024, "kept ${list.size} KB")
    }

    @Test fun abortIsStickyUntilTheNextSlice() {
        Guard.timeBudgetMillis = 50
        Guard.beginSlice()
        // Player code that swallows the abort and keeps looping: the next tick throws again.
        val abort = assertFailsWith<PlayerAbort> {
            while (true) {
                try { spin() } catch (t: Throwable) { /* swallowed */ }
                Guard.tick()
            }
        }
        assertEquals(PlayerAbort.Kind.TIME, abort.kind)
        assertFailsWith<PlayerAbort> { Guard.tick() }
        Guard.endSlice()
        assertFailsWith<PlayerAbort> { Guard.tick() } // still sticky between slices

        Guard.slice { repeat(10_000) { Guard.tick() } } // a new slice starts clean
    }

    @Test fun externalAbortStopsTheSlice() {
        val stopper = thread { Thread.sleep(50); Guard.abort("stopped by the host") }
        val abort = assertFailsWith<PlayerAbort> { Guard.slice { spin() } }
        stopper.join()
        assertEquals(PlayerAbort.Kind.HOST, abort.kind)
        assertEquals("stopped by the host", abort.message)
    }

    @Test fun budgetsOnlyApplyInsideASliceOnItsThread() {
        Guard.timeBudgetMillis = 0
        repeat(10_000) { Guard.tick() } // no slice open: no budget
        Guard.slice {
            var other: Throwable? = null
            thread { try { repeat(10_000) { Guard.tick() } } catch (t: Throwable) { other = t } }.join()
            assertNull(other, "another thread was charged for the slice")
        }
    }

    @Test fun watchdogSeesTheRunningSlice() {
        assertNull(Guard.sliceElapsedMillis())
        Guard.slice {
            Thread.sleep(20)
            assertTrue(assertNotNull(Guard.sliceElapsedMillis()) >= 20)
        }
        assertNull(Guard.sliceElapsedMillis())
    }
}
