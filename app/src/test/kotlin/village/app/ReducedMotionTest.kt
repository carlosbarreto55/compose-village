package village.app

import village.app.ui.theme.ReducedMotion
import kotlin.test.Test
import kotlin.test.assertTrue

class ReducedMotionTest {
    /** The launch option wins over the OS setting; [ReducedMotion.system] is read once, so set it first. */
    @Test fun launchOptionForcesReducedMotion() {
        System.setProperty(ReducedMotion.PROPERTY, "true")
        assertTrue(ReducedMotion.system)
    }
}
