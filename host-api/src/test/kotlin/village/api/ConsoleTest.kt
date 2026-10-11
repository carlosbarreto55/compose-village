package village.api

import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ConsoleTest {
    @BeforeTest fun reset() = Console.clear()

    @Test fun capturesOutAndErrInOrder() {
        Console.out.println("hello")
        Console.err.println("oops")
        Console.out.print("ünïcødé")
        assertEquals("hello\noops\nünïcødé", Console.text().replace(System.lineSeparator(), "\n"))
        assertFalse(Console.truncated)
    }

    @Test fun clearStartsAFreshAttempt() {
        Console.out.println("first attempt")
        Console.clear()
        assertEquals("", Console.text())
    }

    @Test fun capsTheBuffer() {
        val line = "x".repeat(1000)
        repeat(200) { Console.out.println(line) }
        assertEquals(Console.MAX_BYTES, Console.text().length)
        assertTrue(Console.truncated)
    }
}
