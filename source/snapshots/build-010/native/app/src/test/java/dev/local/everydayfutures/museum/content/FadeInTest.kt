package dev.local.everydayfutures.museum.content

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Host checks of the fade-ins' timing: the model switch in the study and the Open in 3D view, and the room after a
 * move. They establish the arithmetic and the Reduce motion rule, not that the SDK's opacity reaches the GLB models,
 * the room or its labels, nor how a part-transparent model sorts on a device.
 */
class FadeInTest {
    @Test
    fun aModelFadeIsShortAndTheRoomsALittleLonger() {
        assertEquals(180L, FadeIn.MODEL_MILLIS)
        assertEquals(250L, FadeIn.ROOM_MILLIS)
        assertTrue(FadeIn.ROOM_MILLIS > FadeIn.MODEL_MILLIS)
    }

    @Test
    fun opacityRisesFromNothingToFullAndNeverFalls() {
        for (duration in listOf(FadeIn.MODEL_MILLIS, FadeIn.ROOM_MILLIS)) {
            assertEquals(0f, FadeIn.opacityAt(0f, duration), 0f)
            assertEquals(0f, FadeIn.opacityAt(-5f, duration), 0f)
            assertEquals(1f, FadeIn.opacityAt(duration.toFloat(), duration), 0f)
            assertEquals(1f, FadeIn.opacityAt(duration + 40f, duration), 0f)
            val samples = (0..duration.toInt() step 5).map { FadeIn.opacityAt(it.toFloat(), duration) }
            samples.zipWithNext().forEach { (a, b) -> assertTrue("opacity never falls ($a then $b)", b >= a) }
            assertTrue(samples.all { it in 0f..1f })
            // Eased out: past half way a third of the way in, nearly there just before the end.
            assertTrue(FadeIn.opacityAt(duration / 3f, duration) > 0.5f)
            assertEquals(1f, FadeIn.opacityAt(duration - 1f, duration), 1e-3f)
            assertFalse(FadeIn.isOver(duration - 1f, duration))
            assertTrue(FadeIn.isOver(duration.toFloat(), duration))
        }
    }

    @Test
    fun atHeadsetFrameRatesAFadeEndsWithinOneFrameOfItsLength() {
        for (frameMillis in listOf(1000f / 90f, 1000f / 72f, 1000f / 60f)) {
            for (duration in listOf(FadeIn.MODEL_MILLIS, FadeIn.ROOM_MILLIS)) {
                var elapsed = 0f
                var frames = 0
                while (!FadeIn.isOver(elapsed, duration)) {
                    elapsed += frameMillis
                    frames += 1
                }
                val took = frames * frameMillis
                assertTrue("$frames frames of $frameMillis ms for $duration ms", took < duration + frameMillis + 1e-3f)
                assertEquals(1f, FadeIn.opacityAt(elapsed, duration), 0f)
            }
        }
    }

    @Test
    fun aBrokenClockNeverLeavesContentTransparent() {
        for (bad in listOf(Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY)) {
            assertTrue(FadeIn.isOver(bad, FadeIn.MODEL_MILLIS))
            assertEquals(1f, FadeIn.opacityAt(bad, FadeIn.ROOM_MILLIS), 0f)
        }
        // A fade with no length shows at once.
        assertEquals(1f, FadeIn.opacityAt(0f, 0L), 0f)
        assertTrue(FadeIn.isOver(0f, 0L))
    }

    @Test
    fun nothingFadesUnderReduceMotionOrWhileHidden() {
        assertNull(FadeIn.skipReason(reduceMotion = false))
        assertTrue(FadeIn.shouldPlay(reduceMotion = false, visible = true))
        assertEquals("reduce-motion", FadeIn.skipReason(reduceMotion = true))
        assertEquals("hidden", FadeIn.skipReason(reduceMotion = false, visible = false))
        // Reduce motion is the reason given when both apply.
        assertEquals("reduce-motion", FadeIn.skipReason(reduceMotion = true, visible = false))
        assertFalse(FadeIn.shouldPlay(reduceMotion = true))
        assertFalse(FadeIn.shouldPlay(reduceMotion = false, visible = false))
    }
}
