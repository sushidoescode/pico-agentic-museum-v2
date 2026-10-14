package dev.local.everydayfutures.museum.stage

import dev.local.everydayfutures.museum.content.FadeIn
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Host checks of the arrival ring's timing and size. The inlay sizes are copied from art/room/build_room.py (floor
 * inlays: rings of radius 0.50 m and 1.50 m, 25 and 40 mm wide, 3 mm proud). How the ring looks on the device (its
 * fade, its sorting against the floor) is the native lane's business.
 */
class ArrivalCueTest {
    private val eps = 1e-5f
    private val samples = (0..5).map { it * 0.1f }

    @Test
    fun startsAtFullStrengthAndEnds() {
        assertEquals(ArrivalCue.Frame(0.8f, 0.6f), ArrivalCue.at(0f))
        assertNull(ArrivalCue.at(ArrivalCue.DURATION_SECONDS))
        assertNull(ArrivalCue.at(-0.01f))
        assertNull(ArrivalCue.at(Float.NaN))
        assertNull(ArrivalCue.at(Float.POSITIVE_INFINITY))
        val frames = samples.map { ArrivalCue.at(it)!! }
        frames.zipWithNext().forEach { (a, b) ->
            assertTrue("opacity never rises", b.opacity <= a.opacity)
            assertTrue("radius never shrinks", b.radius >= a.radius)
        }
        // Nearly gone just before the end.
        val last = ArrivalCue.at(ArrivalCue.DURATION_SECONDS - 0.001f)!!
        assertEquals(ArrivalCue.END_RADIUS, last.radius, 1e-3f)
        assertEquals(0f, last.opacity, 1e-3f)
    }

    @Test
    fun staysBetweenTheBrassInlays() {
        val innerInlayOuterEdge = 0.50f + 0.025f / 2
        val outerInlayInnerEdge = 1.50f - 0.040f / 2
        for (t in samples + (ArrivalCue.DURATION_SECONDS - 0.001f)) {
            val frame = ArrivalCue.at(t)!!
            // The mesh is scaled so its outer edge is the frame's radius; its band keeps the mesh's proportion.
            val innerEdge = frame.radius * ArrivalCue.MESH_INNER / ArrivalCue.MESH_OUTER
            assertTrue("inner edge $innerEdge at $t", innerEdge > innerInlayOuterEdge + eps)
            assertTrue("outer edge ${frame.radius} at $t", frame.radius < outerInlayInnerEdge - eps)
        }
        // The band's bottom (the torus is (outer - inner) / 2 thick each side of its centre) clears the 3 mm inlays.
        assertTrue(ArrivalCue.LIFT - (ArrivalCue.MESH_OUTER - ArrivalCue.MESH_INNER) / 2 > 0.003f)
    }

    @Test
    fun neverUnderReduceMotionAndOnlyAfterAMove() {
        assertFalse(ArrivalCue.shouldPlay(reduceMotion = true, moved = true))
        assertFalse(ArrivalCue.shouldPlay(reduceMotion = false, moved = false))
        assertFalse(ArrivalCue.shouldPlay(reduceMotion = true, moved = false))
        assertTrue(ArrivalCue.shouldPlay(reduceMotion = false, moved = true))
    }

    @Test
    fun theRingWaitsForTheRoomsFadeToEnd() {
        val delay = ArrivalCue.delaySeconds(roomFades = true)
        assertEquals(0.25f, delay, eps)
        // It shows once the room is at full opacity, never while it is part-transparent.
        assertTrue(FadeIn.isOver(delay * 1000f, FadeIn.ROOM_MILLIS))
        assertEquals(1f, FadeIn.opacityAt(delay * 1000f, FadeIn.ROOM_MILLIS), 0f)
        // With no fade (none this visit, or it could not start) it plays at once, as before.
        assertEquals(0f, ArrivalCue.delaySeconds(roomFades = false), 0f)
    }
}
