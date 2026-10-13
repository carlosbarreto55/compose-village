package village.app.server

import village.preview.DpBounds
import village.preview.SemanticsSnapshot
import village.preview.SnapshotNode
import village.preview.Viewport
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class ProtocolTest {
    private fun result(snapshot: SemanticsSnapshot?) =
        Reply.Result(true, emptyList(), null, null, null, 1.0, 2.0, 3.0, snapshot)

    private fun roundTrip(reply: Reply.Result) = assertIs<Reply.Result>(Reply.decode(reply.encode()))

    @Test fun snapshotSurvivesTheWire() {
        val snapshot = SemanticsSnapshot(
            Viewport(400f, 300f),
            listOf(
                SnapshotNode(
                    bounds = DpBounds(0f, 0f, 400f, 300f),
                    children = listOf(
                        SnapshotNode(id = 7, texts = listOf("Rosa's Bakery"), bounds = DpBounds(16f, 16f, 180.5f, 52f)),
                        SnapshotNode(texts = listOf("Order", "now"), role = "Button", bounds = DpBounds(16f, 60f, 120f, 100f),
                            children = listOf(SnapshotNode(contentDescriptions = listOf("bread ✓"), role = "Image", bounds = DpBounds(20f, 64f, 44f, 88f)))),
                    ),
                ),
                SnapshotNode(texts = listOf("popup"), bounds = DpBounds(100f, 100f, 200f, 150f)),
            ),
        )
        val decoded = roundTrip(result(snapshot))
        assertEquals(snapshot, decoded.snapshot)
        assertEquals(3.0, decoded.serverMillis)
    }

    @Test fun noSnapshot() = assertNull(roundTrip(result(null)).snapshot)
}
