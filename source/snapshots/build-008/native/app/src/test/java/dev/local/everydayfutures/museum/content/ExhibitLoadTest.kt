package dev.local.everydayfutures.museum.content

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Host checks of the lazy model-loading decisions shared by the study and the inspector: which state a surface shows,
 * which results still count, the bounded Try again, the procedural fallback and the marker words. They establish the
 * decisions, not that the SDK loads, cancels or renders anything.
 */
class ExhibitLoadTest {
    @Test
    fun aLoadStartsAtAttemptOneAndCompletesToReadyWithItsMarkerWords() {
        val loading = ModelLoadMachine.start("coffeepod", attempt = 1, token = 7L)
        assertEquals(ModelLoadState.Loading("coffeepod", 1, 7L), loading)
        assertEquals("coffeepod", loading.objectId)
        val ready = ModelLoadMachine.complete(loading, 7L, "coffeepod", ModelOutcome.Generated(authoredBounds = false))
        assertEquals(ModelLoadState.Ready("coffeepod", 1, ModelOutcome.Generated(false)), ready)
        assertEquals("generated", ModelOutcome.Generated(false).kind.marker)
        assertEquals("ok", ModelOutcome.Generated(false).reason)
        assertEquals("ok-authored-bounds", ModelOutcome.Generated(true).reason)
        assertNull(ModelLoadState.Idle.objectId)
    }

    @Test
    fun aResultForAnOlderRequestIsStaleAndDiscarded() {
        // Rapid selection: 004 was requested (token 1), then 005 (token 2) before 004's model arrived.
        val second = ModelLoadMachine.start("deliverybox", 1, token = 2L)
        assertNull(ModelLoadMachine.complete(second, 1L, "coffeepod", ModelOutcome.Generated(false)))
        // Same token, different object: also stale.
        assertNull(ModelLoadMachine.complete(second, 2L, "coffeepod", ModelOutcome.Generated(false)))
        // Nothing in flight (already ready, or idle): stale.
        val ready = ModelLoadMachine.complete(second, 2L, "deliverybox", ModelOutcome.Generated(false))!!
        assertNull(ModelLoadMachine.complete(ready, 2L, "deliverybox", ModelOutcome.Generated(false)))
        assertNull(ModelLoadMachine.complete(ModelLoadState.Idle, 2L, "deliverybox", ModelOutcome.Generated(false)))
    }

    @Test
    fun aFailureNamesTheObjectAndTryAgainIsBoundedToTwoRetriesPerSelection() {
        var state: ModelLoadState = ModelLoadMachine.start("rocket", 1, 1L)
        state = ModelLoadMachine.complete(state, 1L, "rocket", ModelOutcome.Failed("missing"))!!
        assertEquals(ModelLoadState.Failed("rocket", "missing", 1), state)
        assertTrue((state as ModelLoadState.Failed).canRetry)
        assertEquals(2, ModelLoadMachine.retryAttempt(state))

        state = ModelLoadMachine.complete(ModelLoadMachine.start("rocket", 2, 2L), 2L, "rocket", ModelOutcome.Failed("timeout"))!!
        assertEquals(3, ModelLoadMachine.retryAttempt(state))
        state = ModelLoadMachine.complete(ModelLoadMachine.start("rocket", 3, 3L), 3L, "rocket", ModelOutcome.Failed("timeout"))!!
        assertFalse((state as ModelLoadState.Failed).canRetry)
        assertNull(ModelLoadMachine.retryAttempt(state)) // two retries spent: only another selection helps
        assertEquals(3, ModelLoadMachine.MAX_ATTEMPTS)
        assertEquals(3, ModelLoadMachine.start("rocket", 9, 4L).attempt) // capped
        assertEquals(1, ModelLoadMachine.start("rocket", 0, 5L).attempt)
        assertNull(ModelLoadMachine.retryAttempt(ModelLoadState.Idle))
        assertNull(ModelLoadMachine.retryAttempt(ModelLoadState.Ready("rocket", 1, ModelOutcome.Generated(false))))
    }

    @Test
    fun onlyTheThreeOriginalsFallBackToProceduralAndNeverToAnotherObject() {
        val fallback = ModelLoadMachine.afterModelFailure("timeout", proceduralFallback = true, proceduralBuilt = true)
        assertEquals(ModelOutcome.Procedural("timeout"), fallback)
        assertEquals("procedural", fallback.kind.marker)
        assertEquals("fallback-procedural-timeout", fallback.reason)
        val loaded = ModelLoadMachine.complete(ModelLoadMachine.start("vacuum", 1, 1L), 1L, "vacuum", fallback)
        assertEquals(ModelLoadState.Ready("vacuum", 1, fallback), loaded) // shown: no error card for the originals

        val none = ModelLoadMachine.afterModelFailure("load-NO_ENTRY", proceduralFallback = false, proceduralBuilt = false)
        assertEquals(ModelOutcome.Failed("load-NO_ENTRY"), none)
        assertEquals("none", none.kind.marker)
        assertEquals("load-NO_ENTRY", none.reason)
        val failedFallback = ModelLoadMachine.afterModelFailure("missing", proceduralFallback = true, proceduralBuilt = false)
        assertEquals(ModelOutcome.Failed("missing+procedural-failed"), failedFallback)
    }

    /** APP-2: a missing file (the app's pre-check, or the SDK's NO_ENTRY) is logged as one line; other failures keep a trace. */
    @Test
    fun onlyAMissingFileCountsAsMissing() {
        assertTrue(ModelLoadMachine.isMissing("missing"))
        assertTrue(ModelLoadMachine.isMissing("load-NO_ENTRY"))
        listOf("timeout", "bounds", "load-INVALID_CONTENT", "load-IOException", "fit-IllegalStateException", "attach-X", "missing+procedural-failed")
            .forEach { assertFalse(it, ModelLoadMachine.isMissing(it)) }
    }

    @Test
    fun theSurfaceBoxesMatchTheBrief() {
        with(ExhibitBoxes.STUDY) {
            assertEquals(0.28f, height, 0f)
            assertEquals(-0.075f, baseY, 0f)
            assertEquals(0f, centreX, 0f)
            assertEquals(0f, centreZ, 0f)
        }
        with(ExhibitBoxes.INSPECTOR) {
            assertEquals(0.30f, height, 0f)
            assertEquals(0.30f, width, 0f)
            assertEquals(0.30f, depth, 0f)
        }
    }

    @Test
    fun theInspectorPoseAccumulatesSpinAndDragBetweenCommits() {
        val pose = InspectorPose()
        assertFalse(pose.advance(0.5f, spinning = false)) // spin off: nothing moves
        assertTrue(pose.advance(0.5f, spinning = true)) // clamped to 0.1 s: 2°
        assertEquals(2f, pose.live(committed = 30f) - 30f, 1e-4f)
        pose.startDrag()
        assertFalse(pose.advance(0.05f, spinning = true)) // held still under a drag
        pose.dragBy(20f) // 20 dp → 10°
        assertEquals(42f, pose.live(30f), 1e-4f)
        assertEquals(42f, pose.endDrag(30f), 1e-4f)
        assertFalse(pose.dragging)
        assertEquals(0f, pose.pending, 0f) // committed: app state now holds 42°
        assertEquals(42f, pose.live(42f), 1e-4f)
        pose.dragBy(-100f) // -50° wraps into [0, 360)
        assertEquals(352f, pose.live(42f), 1e-3f)
        pose.clear()
        assertEquals(0f, pose.live(0f), 0f)
        assertFalse(pose.advance(Float.NaN, spinning = true))
        assertFalse(pose.advance(-1f, spinning = true))
        assertEquals(0f, InspectorPose.normalise(360f), 0f)
        assertEquals(330f, InspectorPose.normalise(-30f), 0f)
        assertEquals(0f, InspectorPose.normalise(-0f), 0f)
        assertEquals(0f, InspectorPose.normalise(Float.NaN), 0f)
        // 54 s of spin at 20°/s in 20 ms frames is three whole turns: back where it started.
        repeat(2700) { pose.advance(0.02f, spinning = true) }
        assertEquals(0f, pose.live(0f).let { if (it > 180f) it - 360f else it }, 0.05f)
    }
}
