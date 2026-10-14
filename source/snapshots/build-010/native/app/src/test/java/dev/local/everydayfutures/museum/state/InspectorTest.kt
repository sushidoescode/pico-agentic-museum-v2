package dev.local.everydayfutures.museum.state

import dev.local.everydayfutures.museum.catalogue.Catalogue
import dev.local.everydayfutures.museum.content.FadeIn
import dev.local.everydayfutures.museum.content.ModelSwap
import dev.local.everydayfutures.museum.content.RootWrite
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Host checks of the Open in 3D inspector's presentation state, driven through the shared controller the windows use.
 * They establish state and markers, not what the inspector window draws, whether spin looks smooth or whether a drag
 * reaches it on any device.
 */
class InspectorTest {
    private class RecordingNavigator : WindowNavigator {
        val calls = mutableListOf<String>()

        override fun open(id: String, tag: String) {
            calls += "open $id $tag"
        }

        override fun close(id: String, tag: String) {
            calls += "close $id $tag"
        }
    }

    private class MemoryPersistence : MuseumPersistence {
        var stored = MuseumState()
        var saves = 0

        override fun load(): MuseumState = MuseumState(savedIds = stored.savedIds, options = stored.options)

        override fun save(state: MuseumState) {
            saves += 1
            stored = state
        }
    }

    private fun owner(log: MutableList<String> = mutableListOf(), disk: MemoryPersistence = MemoryPersistence()) =
        MuseumController(persistence = disk, log = { log += it })

    /** Selects [id], turns the study twice to the right (60°) and opens the inspector, which then attaches. */
    private fun openedAt60(museum: MuseumController, nav: WindowNavigator, id: String = "coffeepod") {
        museum.windowAttached(Surface.PRIMARY)
        museum.select(id, Surface.PRIMARY)
        museum.turn(Turn.RIGHT, Surface.PRIMARY)
        museum.turn(Turn.RIGHT, Surface.PRIMARY)
        museum.openInspector(nav, Surface.PRIMARY)
        museum.windowAttached(Surface.INSPECTOR)
    }

    @Test
    fun openingShowsTheSelectionAtTheStudyPoseNotSpinning() {
        val log = mutableListOf<String>()
        val museum = owner(log)
        val nav = RecordingNavigator()
        openedAt60(museum, nav)
        assertEquals(InspectorState("coffeepod", 60f, spinning = false), museum.state.inspector)
        assertEquals(listOf("open ${WorkspaceReducer.INSPECTOR_ID} ${WorkspaceReducer.INSPECTOR_TAG}"), nav.calls)
        assertTrue(log.any { it.startsWith("inspector-open request=#1 mode=open tag=inspector instances=0 source=primary") })
        assertTrue(log.any { it.startsWith("window-attached surface=inspector instances=1") && it.contains("inspector=coffeepod:60:still") })
        // The study is untouched by opening it.
        assertEquals(2, museum.state.turnSteps)
        assertEquals(View.INSPECT, museum.state.view)
    }

    @Test
    fun turnSpinDragAndResetChangeOnlyTheInspector() {
        val log = mutableListOf<String>()
        val museum = owner(log)
        openedAt60(museum, RecordingNavigator())
        val study = museum.state

        museum.inspectorToggleSpin(liveYaw = 60f, source = Surface.INSPECTOR)
        assertTrue(museum.state.inspector!!.spinning)
        assertTrue(log.last().startsWith("inspector-spin on yaw=60 source=inspector"))

        museum.inspectorTurn(Turn.RIGHT, liveYaw = 95f, source = Surface.INSPECTOR) // spun 35° on screen, then Turn right
        assertEquals(InspectorState("coffeepod", 125f, spinning = false), museum.state.inspector)
        assertTrue(log.last().startsWith("inspector-turn direction=right yaw=125 source=inspector"))
        assertTrue(log[log.size - 2].startsWith("inspector-spin off yaw=95 reason=turn source=inspector")) // where spin stopped

        museum.inspectorTurn(Turn.LEFT, liveYaw = 125f, source = Surface.INSPECTOR)
        museum.inspectorTurn(Turn.LEFT, liveYaw = 95f, source = Surface.INSPECTOR)
        museum.inspectorTurn(Turn.LEFT, liveYaw = 65f, source = Surface.INSPECTOR)
        assertEquals(35f, museum.state.inspector!!.yawDegrees, 1e-4f)

        museum.inspectorToggleSpin(35f, Surface.INSPECTOR)
        museum.inspectorDragged(yaw = 350f, source = Surface.INSPECTOR) // a drag stops spin
        assertEquals(InspectorState("coffeepod", 350f, spinning = false), museum.state.inspector)
        assertTrue(log.last().startsWith("inspector-drag yaw=350 source=inspector"))
        assertTrue(log[log.size - 2].startsWith("inspector-spin off yaw=350 reason=drag"))

        museum.inspectorToggleSpin(350f, Surface.INSPECTOR)
        museum.inspectorToggleSpin(liveYaw = 12f, source = Surface.INSPECTOR) // Pause commits where it is on screen
        assertEquals(InspectorState("coffeepod", 12f, spinning = false), museum.state.inspector)
        assertTrue(log.last().startsWith("inspector-spin off yaw=12"))

        museum.inspectorToggleSpin(12f, Surface.INSPECTOR)
        museum.inspectorResetView(Surface.INSPECTOR, liveYaw = 47f)
        assertEquals(InspectorState("coffeepod", 0f, spinning = false), museum.state.inspector)
        assertTrue(log.last().startsWith("inspector-reset yaw=0"))
        assertTrue(log[log.size - 2].startsWith("inspector-spin off yaw=47 reason=reset"))

        // None of that touched the study, the selection, the story or the saved collection.
        val after = museum.state
        assertEquals(study.copy(inspector = after.inspector), after)
    }

    @Test
    fun spinNeverRunsUnderReduceMotionAndStopsWhenItIsSwitchedOn() {
        val log = mutableListOf<String>()
        val museum = owner(log)
        openedAt60(museum, RecordingNavigator())
        museum.inspectorToggleSpin(60f, Surface.INSPECTOR)
        assertTrue(museum.state.inspector!!.spinning)

        museum.setReduceMotion(true, Surface.PRIMARY)
        assertFalse(museum.state.inspector!!.spinning) // at once
        // Updated in APP-2: the stop marker no longer carries the last committed yaw (it read 0 after 21 s of spin on
        // build-001); the angle on screen follows in `inspector-settle` from the inspector window (next test).
        assertTrue(log.last().startsWith("inspector-spin off reason=reduce-motion source=primary"))
        museum.inspectorSettle(liveYaw = 60f, reason = "reduce-motion", source = Surface.INSPECTOR) // no turn to fold

        museum.inspectorToggleSpin(80f, Surface.INSPECTOR) // Spin while Reduce motion is on: stays off
        assertFalse(museum.state.inspector!!.spinning)
        assertEquals(80f, museum.state.inspector!!.yawDegrees, 0f)
        assertTrue(log.last().startsWith("inspector-spin off yaw=80 reason=reduce-motion"))

        museum.setReduceMotion(false, Surface.PRIMARY)
        assertFalse(museum.state.inspector!!.spinning) // spin is opt-in: it does not resume by itself
        museum.inspectorToggleSpin(80f, Surface.INSPECTOR)
        assertTrue(museum.state.inspector!!.spinning)
    }

    /**
     * APP-2 (build-001 finding): Reduce motion switched on in another window stops spin in state, but the angle the
     * model spun to lives in the inspector window's pose. The window folds it in with inspectorSettle, so the committed
     * yaw equals the angle on screen; the pose arithmetic is the same InspectorPose the window uses.
     */
    @Test
    fun aSpinStoppedByReduceMotionSettlesAtTheAngleOnScreen() {
        val log = mutableListOf<String>()
        val museum = owner(log)
        openedAt60(museum, RecordingNavigator())
        val pose = dev.local.everydayfutures.museum.content.InspectorPose()
        museum.inspectorToggleSpin(pose.commit(60f), Surface.INSPECTOR)
        repeat(1050) { pose.advance(0.02f, spinning = museum.state.inspector!!.spinning) } // 21 s at 20°/s = 420°
        museum.setReduceMotion(true, Surface.PRIMARY)
        assertFalse(pose.advance(0.02f, spinning = museum.state.inspector!!.spinning)) // the next frame sees the stop
        assertEquals(60f, museum.state.inspector!!.yawDegrees, 0f) // not yet folded: state lags the screen
        val onScreen = pose.live(museum.state.inspector!!.yawDegrees)
        assertEquals(120f, onScreen, 0.05f) // 60 + 420 = 480 → 120

        val actionsBefore = log.count { it.contains(" action=#") }
        val before = museum.state
        museum.inspectorSettle(pose.commit(museum.state.inspector!!.yawDegrees), "reduce-motion", Surface.INSPECTOR)
        assertEquals(onScreen, museum.state.inspector!!.yawDegrees, 1e-4f)
        assertEquals(onScreen, pose.live(museum.state.inspector!!.yawDegrees), 1e-4f) // state == screen
        assertFalse(museum.state.inspector!!.spinning)
        assertEquals(before.copy(inspector = museum.state.inspector), museum.state) // nothing else changed
        assertTrue(log.last(), log.last().startsWith("inspector-settle yaw=120 reason=reduce-motion source=inspector"))
        assertEquals(actionsBefore, log.count { it.contains(" action=#") }) // not a visitor action

        // Nothing left to fold, or no inspector: no marker.
        val count = log.size
        museum.inspectorSettle(museum.state.inspector!!.yawDegrees, "reduce-motion", Surface.INSPECTOR)
        assertEquals(count, log.size)
        val bare = owner(log)
        bare.inspectorSettle(33f, "reduce-motion", Surface.INSPECTOR)
        assertNull(bare.state.inspector)
        assertEquals(count, log.size)
    }

    /**
     * APP-3 (build-003 finding): while the inspector is attached and shows the study's own object, the study hides its
     * copy; not while the inspector is only requested, not while it is being closed for the companion, not after it
     * has gone.
     */
    @Test
    fun theStudyHidesItsModelOnlyWhileTheAttachedInspectorShowsTheSameObject() {
        val log = mutableListOf<String>()
        val museum = owner(log)
        val nav = RecordingNavigator()
        assertFalse(museum.state.studyShownInInspector)
        museum.windowAttached(Surface.PRIMARY)
        museum.select(Catalogue.VACUUM, Surface.PRIMARY)
        museum.openInspector(nav, Surface.PRIMARY)
        assertFalse(museum.state.studyShownInInspector) // requested, not attached yet
        museum.windowAttached(Surface.INSPECTOR)
        assertTrue(museum.state.studyShownInInspector)
        museum.select("scooter", Surface.PRIMARY) // the inspector follows the selection: still the same object
        assertTrue(museum.state.studyShownInInspector)
        museum.turn(Turn.RIGHT, Surface.PRIMARY) // the study's own controls do not change it
        assertTrue(museum.state.studyShownInInspector)

        // A presentation of another object (not reachable through the reducers, which follow the selection) is not hidden.
        val other = museum.state.copy(inspector = museum.state.inspector!!.copy(objectId = "rocket"))
        assertFalse(other.studyShownInInspector)

        // Show story on an object with one: the inspector is being closed for the companion → the study shows again.
        museum.select(Catalogue.VACUUM, Surface.PRIMARY)
        museum.revealStory(nav, Surface.PRIMARY)
        assertEquals(Surface.COMPANION, museum.state.workspace.pendingAux)
        assertEquals(1, museum.state.workspace.inspectorInstances) // still attached, closing
        assertFalse(museum.state.studyShownInInspector)
        museum.windowDetached(Surface.INSPECTOR)
        assertFalse(museum.state.studyShownInInspector)
        museum.windowAttached(Surface.COMPANION)
        assertFalse(museum.state.studyShownInInspector)

        // Closed with its own Close: hidden until the detach is observed, then shown.
        museum.openInspector(nav, Surface.PRIMARY) // pending: the companion closes first
        museum.windowDetached(Surface.COMPANION)
        museum.windowAttached(Surface.INSPECTOR)
        assertTrue(museum.state.studyShownInInspector)
        museum.closeInspector(nav, 0f, Surface.INSPECTOR)
        assertTrue(museum.state.studyShownInInspector)
        museum.windowDetached(Surface.INSPECTOR)
        assertFalse(museum.state.studyShownInInspector)

        museum.studyModelHidden(true, Catalogue.VACUUM)
        assertTrue(log.last().startsWith("study-model hidden=1 reason=inspector id=vacuum"))
    }

    @Test
    fun aNewSelectionShowsTheNewObjectAtItsFrontView() {
        val museum = owner()
        openedAt60(museum, RecordingNavigator())
        museum.inspectorToggleSpin(60f, Surface.INSPECTOR)
        museum.select("rocket", Surface.PRIMARY)
        assertEquals(InspectorState("rocket", 0f, spinning = false), museum.state.inspector)
        // Rapid selection: only the last one counts, always at the front view.
        listOf("gascan", "router", Catalogue.VACUUM, "scooter").forEach { museum.select(it, Surface.PRIMARY) }
        assertEquals(InspectorState("scooter", 0f, spinning = false), museum.state.inspector)
        // Selecting the object it already shows changes nothing.
        museum.inspectorTurn(Turn.RIGHT, 0f, Surface.INSPECTOR)
        museum.select("scooter", Surface.COMPANION)
        assertEquals(30f, museum.state.inspector!!.yawDegrees, 0f)
        // An unknown id is ignored.
        museum.select("astronaut", Surface.PRIMARY)
        assertEquals("scooter", museum.state.inspector!!.objectId)
    }

    @Test
    fun aRepeatedOpenFocusesTheSameWindowAndKeepsItsPresentation() {
        val log = mutableListOf<String>()
        val museum = owner(log)
        val nav = RecordingNavigator()
        openedAt60(museum, nav)
        museum.inspectorTurn(Turn.RIGHT, 60f, Surface.INSPECTOR)
        museum.turn(Turn.LEFT, Surface.PRIMARY) // the study moves on its own
        museum.openInspector(nav, Surface.PRIMARY)
        museum.openInspector(nav, Surface.PRIMARY)
        val request = "open ${WorkspaceReducer.INSPECTOR_ID} ${WorkspaceReducer.INSPECTOR_TAG}"
        assertEquals(listOf(request, request, request), nav.calls) // the same id and tag: the platform reuses it
        assertEquals(listOf("open", "focus", "focus"), log.filter { it.startsWith("inspector-open") }.map { it.substringAfter("mode=").substringBefore(' ') })
        assertEquals(InspectorState("coffeepod", 90f, spinning = false), museum.state.inspector)
        assertEquals(1, museum.state.workspace.inspectorInstances)
    }

    @Test
    fun closingClearsThePresentationAndChangesNothingElse() {
        val log = mutableListOf<String>()
        val disk = MemoryPersistence()
        val museum = owner(log, disk)
        val nav = RecordingNavigator()
        openedAt60(museum, nav)
        museum.keep(Surface.PRIMARY)
        museum.revealStory(nav, Surface.PRIMARY) // no story for the coffee pod: nothing opens in the story
        museum.inspectorToggleSpin(60f, Surface.INSPECTOR)
        val before = museum.state
        val saves = disk.saves

        museum.closeInspector(nav, liveYaw = 200f, source = Surface.INSPECTOR)
        assertTrue(log[log.size - 2].startsWith("inspector-spin off yaw=200 reason=close"))
        assertTrue(log.last().startsWith("inspector-close request=#"))
        assertTrue(log.last().contains("tag=inspector instances=1 yaw=200 source=inspector"))
        museum.windowDetached(Surface.INSPECTOR)

        val after = museum.state
        assertNull(after.inspector)
        assertEquals(before.turnSteps, after.turnSteps)
        assertEquals(before.view, after.view)
        assertEquals(before.story, after.story)
        assertEquals(before.savedIds, after.savedIds)
        assertEquals(before.selectedId, after.selectedId)
        assertEquals(saves, disk.saves) // the inspector is never persisted
        assertNull(owner(disk = disk).state.inspector)
    }

    @Test
    fun aReopenReadsTheCurrentSelectionAndTheDefinedInitialView() {
        val museum = owner()
        val nav = RecordingNavigator()
        openedAt60(museum, nav)
        museum.inspectorDragged(222f, Surface.INSPECTOR)
        museum.closeInspector(nav, 222f, Surface.INSPECTOR)
        museum.windowDetached(Surface.INSPECTOR)
        museum.select("smartwatch", Surface.PRIMARY)
        museum.turn(Turn.LEFT, Surface.PRIMARY)
        museum.openInspector(nav, Surface.PRIMARY)
        assertEquals(InspectorState("smartwatch", 330f, spinning = false), museum.state.inspector)
        museum.windowAttached(Surface.INSPECTOR)
        assertEquals(InspectorState("smartwatch", 330f, spinning = false), museum.state.inspector)
    }

    @Test
    fun closingDuringALoadClearsThePresentationAndKeepsTheSelection() {
        // State only: the model load belongs to the window's composition and is cancelled with it (not exercised here); a close during a load is
        // an ordinary close: the presentation goes, the selection it was loading stays selected.
        val museum = owner()
        val nav = RecordingNavigator()
        openedAt60(museum, nav)
        museum.select("scooter", Surface.PRIMARY) // a new model starts loading
        museum.closeInspector(nav, null, Surface.PRIMARY)
        museum.windowDetached(Surface.INSPECTOR)
        assertNull(museum.state.inspector)
        assertEquals("scooter", museum.state.selectedId)
        assertEquals(0, museum.state.workspace.inspectorInstances)
    }

    @Test
    fun aWindowTheSystemRestoresStartsFromTheInitialView() {
        val museum = owner()
        museum.select("remote", Surface.PRIMARY)
        museum.turn(Turn.RIGHT, Surface.PRIMARY)
        museum.windowAttached(Surface.INSPECTOR) // attached without an Open in 3D
        assertEquals(InspectorState("remote", 30f, spinning = false), museum.state.inspector)
    }

    @Test
    fun inspectorActionsWithNoInspectorDoNothing() {
        val log = mutableListOf<String>()
        val museum = owner(log)
        museum.inspectorTurn(Turn.RIGHT, 0f, Surface.INSPECTOR)
        museum.inspectorToggleSpin(0f, Surface.INSPECTOR)
        museum.inspectorDragged(10f, Surface.INSPECTOR)
        museum.inspectorResetView(Surface.INSPECTOR)
        assertEquals(MuseumState(), museum.state)
        assertTrue(log.isEmpty())
    }

    @Test
    fun aSelectionRetargetsTheInspectorAtOnceSoItsOldModelIsHiddenNotTurnedToFront() {
        val opened = InspectorReducer.open(MuseumState(selectedId = Catalogue.QUENCHER))
        val turned = InspectorReducer.turn(opened, Turn.RIGHT, liveYaw = 0f)
        assertEquals(Catalogue.QUENCHER, InspectorReducer.shownObjectId(turned))
        assertEquals(30f, turned.inspector!!.yawDegrees, 0f)
        val next = MuseumReducer.select(turned, Catalogue.VACUUM)
        assertEquals(Catalogue.VACUUM, InspectorReducer.shownObjectId(next))
        assertEquals(0f, next.inspector!!.yawDegrees, 0f)
        // The window has not recomposed yet: the slot still holds the quencher's root, so the front yaw becomes a hide.
        assertEquals(RootWrite.HIDE_STALE, ModelSwap.rootWrite(Catalogue.QUENCHER, InspectorReducer.shownObjectId(next)))
        assertEquals(Catalogue.VACUUM, InspectorReducer.shownObjectId(MuseumState(selectedId = Catalogue.VACUUM)))
        val unknown = MuseumState(inspector = InspectorState("unknown", 0f, spinning = false))
        assertEquals(Catalogue.artifacts.first().id, InspectorReducer.shownObjectId(unknown))
    }

    @Test
    fun theModelSwapMarkerNamesTheSurfaceAndBothObjects() {
        val log = mutableListOf<String>()
        val museum = owner(log)
        museum.modelSwapHidden("inspector", Catalogue.QUENCHER, Catalogue.VACUUM)
        assertEquals(listOf("model-swap surface=inspector hidden=quencher for=vacuum"), log)
        assertEquals(MuseumState(), museum.state) // diagnostic only: no state change, no action number
    }

    @Test
    fun theModelFadeMarkerNamesTheSurfaceTheObjectAndItsLength() {
        val log = mutableListOf<String>()
        val museum = owner(log)
        museum.modelFade(Catalogue.VACUUM, "study", FadeIn.MODEL_MILLIS)
        museum.modelFade(Catalogue.VACUUM, "inspector", 0L, FadeIn.skipReason(reduceMotion = true))
        museum.modelFade(Catalogue.QUENCHER, "study", 0L, FadeIn.skipReason(reduceMotion = false, visible = false))
        assertEquals(
            listOf(
                "model-fade surface=study id=vacuum ms=180",
                "model-fade surface=inspector id=vacuum ms=0 reason=reduce-motion",
                "model-fade surface=study id=quencher ms=0 reason=hidden",
            ),
            log,
        )
        assertEquals(MuseumState(), museum.state) // diagnostic only: no state change, no action number
    }
}
