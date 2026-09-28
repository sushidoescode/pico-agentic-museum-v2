package dev.local.everydayfutures.museum.state

import dev.local.everydayfutures.museum.catalogue.Catalogue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Host checks of the one-auxiliary-window policy (the primary plus at most one of the companion and the inspector),
 * driven through the controller calls the window roots make, with the platform's attach and detach played by hand.
 * They establish the requests the app makes and when, not how the platform places, focuses or closes windows.
 */
class AuxWindowTest {
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
        override fun load(): MuseumState = MuseumState()

        override fun save(state: MuseumState) = Unit
    }

    private val openCompanion = "open ${WorkspaceReducer.COMPANION_ID} ${WorkspaceReducer.COMPANION_TAG}"
    private val closeCompanion = "close ${WorkspaceReducer.COMPANION_ID} ${WorkspaceReducer.COMPANION_TAG}"
    private val openInspector = "open ${WorkspaceReducer.INSPECTOR_ID} ${WorkspaceReducer.INSPECTOR_TAG}"
    private val closeInspector = "close ${WorkspaceReducer.INSPECTOR_ID} ${WorkspaceReducer.INSPECTOR_TAG}"

    private fun owner(log: MutableList<String>, clock: () -> Long = { 0L }) =
        MuseumController(persistence = MemoryPersistence(), log = { log += it }, clock = clock)

    /** How many auxiliary windows are attached right now: the invariant is never more than one. */
    private fun MuseumController.auxAttached(): Int = state.workspace.companionInstances + state.workspace.inspectorInstances

    @Test
    fun openIn3dWhileTheCompanionIsOpenClosesItFirstAndOpensOnlyAfterItsDetach() {
        val log = mutableListOf<String>()
        val museum = owner(log)
        val nav = RecordingNavigator()
        museum.windowAttached(Surface.PRIMARY)
        museum.openCompanion(nav, Surface.PRIMARY)
        museum.windowAttached(Surface.COMPANION)

        museum.openInspector(nav, Surface.PRIMARY)
        assertEquals(listOf(openCompanion, closeCompanion), nav.calls) // no inspector yet
        assertEquals(Surface.INSPECTOR, museum.state.workspace.pendingAux)
        assertTrue(log.any { it.startsWith("aux-switch from=companion to=inspector step=close source=primary") })

        // A repeated press while the companion is still attached re-issues the close and opens nothing.
        museum.openInspector(nav, Surface.PRIMARY)
        assertEquals(listOf(openCompanion, closeCompanion, closeCompanion), nav.calls)
        assertTrue(log.any { it.startsWith("aux-switch from=companion to=inspector step=pending") })

        museum.windowDetached(Surface.COMPANION) // the platform closed it
        assertEquals(listOf(openCompanion, closeCompanion, closeCompanion, openInspector), nav.calls)
        assertNull(museum.state.workspace.pendingAux)
        assertTrue(log.any { it.startsWith("aux-switch from=companion to=inspector step=open source=primary") })
        assertTrue(log.any { it.startsWith("inspector-open request=#1 mode=open tag=inspector instances=0 source=primary") })

        museum.windowAttached(Surface.INSPECTOR)
        assertTrue(log.last().startsWith("aux-switch from=companion to=inspector step=done"))
        assertEquals(1, museum.auxAttached())
        assertEquals(Catalogue.QUENCHER, museum.state.inspector!!.objectId)
    }

    @Test
    fun showStoryOrOpenCompanionWhileTheInspectorIsOpenClosesItFirst() {
        val log = mutableListOf<String>()
        val museum = owner(log)
        val nav = RecordingNavigator()
        museum.select(Catalogue.VACUUM, Surface.PRIMARY)
        museum.openInspector(nav, Surface.PRIMARY)
        museum.windowAttached(Surface.INSPECTOR)

        museum.revealStory(nav, Surface.PRIMARY) // the story is set up at once; the window waits for the inspector to go
        assertEquals(StoryState(Catalogue.VACUUM, 0, EntryContext(View.CATALOGUE, 0)), museum.state.story)
        assertEquals(listOf(openInspector, closeInspector), nav.calls)
        assertEquals(Surface.COMPANION, museum.state.workspace.pendingAux)

        museum.openCompanion(nav, Surface.PRIMARY) // pressed again while pending: the close again, nothing opened
        assertEquals(listOf(openInspector, closeInspector, closeInspector), nav.calls)

        museum.windowDetached(Surface.INSPECTOR)
        assertNull(museum.state.inspector) // its presentation went with it
        assertEquals(openCompanion, nav.calls.last())
        museum.windowAttached(Surface.COMPANION)
        assertEquals(1, museum.auxAttached())
        assertTrue(log.last().startsWith("aux-switch from=inspector to=companion step=done"))
        assertEquals(StoryState(Catalogue.VACUUM, 0, EntryContext(View.CATALOGUE, 0)), museum.state.story)
    }

    @Test
    fun aThirdWindowIsNeverRequestedWhateverIsPressed() {
        val museum = owner(mutableListOf())
        val nav = RecordingNavigator()
        museum.windowAttached(Surface.PRIMARY)
        museum.openCompanion(nav, Surface.PRIMARY)
        museum.windowAttached(Surface.COMPANION)
        // Mash both buttons while the companion is still attached.
        repeat(3) {
            museum.openInspector(nav, Surface.PRIMARY)
            museum.openCompanion(nav, Surface.PRIMARY)
            museum.openInspector(nav, Surface.COMPANION)
        }
        assertTrue("no inspector open while the companion is attached: ${nav.calls}", nav.calls.none { it == openInspector })
        // The last press asked for the inspector, so that is what opens once the companion has gone.
        museum.windowDetached(Surface.COMPANION)
        assertEquals(openInspector, nav.calls.last())
        assertEquals(1, nav.calls.count { it == openInspector })
        museum.windowAttached(Surface.INSPECTOR)
        assertEquals(1, museum.auxAttached())
    }

    @Test
    fun askingForTheWindowThatIsClosingOpensItAgainAfterItsDetach() {
        val log = mutableListOf<String>()
        val museum = owner(log)
        val nav = RecordingNavigator()
        museum.openCompanion(nav, Surface.PRIMARY)
        museum.windowAttached(Surface.COMPANION)
        museum.openInspector(nav, Surface.PRIMARY) // pending inspector, the companion's close is in flight
        museum.openCompanion(nav, Surface.PRIMARY) // changed their mind before the detach arrived
        // A focus now would be undone by the close already sent (the visitor would be left with neither window).
        assertEquals(listOf(openCompanion, closeCompanion), nav.calls)
        assertEquals(Surface.COMPANION, museum.state.workspace.pendingAux)
        assertTrue(log.any { it.startsWith("aux-switch from=companion to=companion step=reopen") })
        museum.windowDetached(Surface.COMPANION)
        assertEquals(listOf(openCompanion, closeCompanion, openCompanion), nav.calls) // the inspector never opens
        museum.windowAttached(Surface.COMPANION)
        assertEquals(1, museum.auxAttached())
        assertNull(museum.state.workspace.pendingAux)
    }

    @Test
    fun askingForTheOtherWindowWhileOneIsStillOpeningWaitsForItsAttachThenSwitches() {
        val log = mutableListOf<String>()
        val museum = owner(log)
        val nav = RecordingNavigator()
        museum.openInspector(nav, Surface.PRIMARY) // requested; its root has not attached yet
        museum.revealStory(nav, Surface.PRIMARY) // pressed inside the attach gap (about 120 ms on the emulator)
        assertEquals(listOf(openInspector), nav.calls) // no companion requested beside an opening inspector
        assertEquals(Surface.COMPANION, museum.state.workspace.pendingAux)
        assertTrue(log.any { it.startsWith("aux-switch from=inspector to=companion step=after-open") })
        museum.windowAttached(Surface.INSPECTOR) // it arrives and is closed at once
        assertEquals(listOf(openInspector, closeInspector), nav.calls)
        museum.windowDetached(Surface.INSPECTOR)
        assertEquals(listOf(openInspector, closeInspector, openCompanion), nav.calls)
        museum.windowAttached(Surface.COMPANION)
        assertEquals(1, museum.auxAttached())
        assertEquals(Catalogue.QUENCHER, museum.state.story!!.objectId)
    }

    @Test
    fun askingAgainForTheWindowThatIsOpeningRequestsNothingMore() {
        val log = mutableListOf<String>()
        val museum = owner(log)
        val nav = RecordingNavigator()
        museum.openInspector(nav, Surface.PRIMARY)
        museum.openInspector(nav, Surface.PRIMARY)
        assertEquals(listOf(openInspector), nav.calls)
        assertTrue(log.any { it.startsWith("inspector-open ignored=opening") })
        museum.windowAttached(Surface.INSPECTOR)
        museum.openInspector(nav, Surface.PRIMARY) // attached now: the same instance is focused
        assertEquals("focus", log.last { it.startsWith("inspector-open request") }.substringAfter("mode=").substringBefore(' '))
        assertEquals(1, museum.state.workspace.inspectorInstances)
    }

    @Test
    fun enterArchiveClosesAWindowThatIsStillOpeningOnceItAttaches() {
        val museum = owner(mutableListOf())
        val nav = RecordingNavigator()
        museum.openInspector(nav, Surface.PRIMARY)
        assertTrue(museum.enterArchive(nav, Surface.PRIMARY))
        assertEquals(listOf(openInspector), nav.calls) // nothing attached yet to close
        museum.windowAttached(Surface.INSPECTOR)
        assertEquals(listOf(openInspector, closeInspector), nav.calls)
        museum.windowDetached(Surface.INSPECTOR)
        assertEquals(listOf(openInspector, closeInspector), nav.calls) // and nothing opens after it
    }

    @Test
    fun aLostOpenDoesNotHoldThePolicyForever() {
        var now = 0L
        val museum = owner(mutableListOf(), clock = { now })
        val nav = RecordingNavigator()
        museum.openInspector(nav, Surface.PRIMARY) // its attach never arrives
        now = WorkspaceReducer.IN_FLIGHT_MS - 1
        museum.openCompanion(nav, Surface.PRIMARY) // still in flight: waits
        assertEquals(listOf(openInspector), nav.calls)
        now = WorkspaceReducer.IN_FLIGHT_MS + 1
        museum.openCompanion(nav, Surface.PRIMARY) // past the bound the lost open no longer holds the slot
        assertEquals(listOf(openInspector, openCompanion), nav.calls)
    }

    /**
     * A platform that answers requests in order, one at a time, interleaved with presses at seeded random points. The
     * invariant: an auxiliary window never attaches while the other one is attached.
     */
    @Test
    fun noInterleavingOfPressesAndPlatformEventsEverShowsTwoAuxiliaryWindows() {
        repeat(40) { seed ->
            val rnd = kotlin.random.Random(seed)
            val museum = owner(mutableListOf())
            val queue = ArrayDeque<String>()
            val nav = object : WindowNavigator {
                override fun open(id: String, tag: String) { queue.addLast("open $id") }
                override fun close(id: String, tag: String) { queue.addLast("close $id") }
            }
            val attached = mutableSetOf<Surface>()
            fun surfaceOf(id: String) = if (id == WorkspaceReducer.COMPANION_ID) Surface.COMPANION else Surface.INSPECTOR
            museum.windowAttached(Surface.PRIMARY)
            repeat(200) { step ->
                if (queue.isNotEmpty() && rnd.nextInt(3) == 0) {
                    val (kind, id) = queue.removeFirst().split(' ')
                    val surface = surfaceOf(id)
                    if (kind == "open" && surface !in attached) {
                        assertTrue("seed $seed step $step: $surface attached beside $attached", attached.isEmpty())
                        attached += surface
                        museum.windowAttached(surface)
                    } else if (kind == "close" && surface in attached) {
                        attached -= surface
                        museum.windowDetached(surface)
                    }
                } else {
                    when (rnd.nextInt(5)) {
                        0 -> museum.openInspector(nav, Surface.PRIMARY)
                        1 -> museum.openCompanion(nav, Surface.PRIMARY)
                        2 -> museum.revealStory(nav, Surface.PRIMARY)
                        3 -> if (Surface.INSPECTOR in attached) museum.closeInspector(nav, liveYaw = 0f, source = Surface.INSPECTOR)
                        else -> if (Surface.COMPANION in attached) museum.closeCompanion(nav, Surface.COMPANION)
                    }
                }
                assertTrue("seed $seed step $step: ${museum.auxAttached()} attached", museum.auxAttached() <= 1)
            }
        }
    }

    @Test
    fun closingOrOpeningWithNothingElseAttachedIsDirect() {
        val log = mutableListOf<String>()
        val museum = owner(log)
        val nav = RecordingNavigator()
        museum.openInspector(nav, Surface.PRIMARY)
        museum.windowAttached(Surface.INSPECTOR)
        museum.closeInspector(nav, liveYaw = 0f, source = Surface.INSPECTOR)
        museum.windowDetached(Surface.INSPECTOR)
        museum.openCompanion(nav, Surface.PRIMARY)
        assertEquals(listOf(openInspector, closeInspector, openCompanion), nav.calls)
        assertTrue(log.none { it.startsWith("aux-switch") })
    }

    @Test
    fun aDuplicateInspectorIsCountedRatherThanHidden() {
        val log = mutableListOf<String>()
        val museum = owner(log)
        museum.windowAttached(Surface.INSPECTOR)
        museum.windowAttached(Surface.INSPECTOR)
        assertEquals(2, museum.state.workspace.inspectorInstances)
        assertTrue(log.last().contains("surface=inspector instances=2"))
        museum.windowDetached(Surface.INSPECTOR)
        assertEquals(1, museum.state.workspace.inspectorInstances)
        assertTrue(museum.state.inspector != null) // one instance is still showing it
        museum.windowDetached(Surface.INSPECTOR)
        museum.windowDetached(Surface.INSPECTOR) // unmatched: never negative
        assertEquals(0, museum.state.workspace.inspectorInstances)
        assertNull(museum.state.inspector)
    }

    @Test
    fun theWorkspacePlanIsPureAndTotal() {
        val empty = MuseumState()
        assertEquals(AuxPlan.Show(WindowRequest(WorkspaceReducer.INSPECTOR_ID, WorkspaceReducer.INSPECTOR_TAG, WindowRequest.Kind.OPEN)), WorkspaceReducer.planAux(empty, Surface.INSPECTOR))
        val companionUp = empty.copy(workspace = Workspace(companionInstances = 1))
        val plan = WorkspaceReducer.planAux(companionUp, Surface.INSPECTOR)
        assertEquals(
            AuxPlan.CloseFirst(Surface.COMPANION, Surface.INSPECTOR, WindowRequest(WorkspaceReducer.COMPANION_ID, WorkspaceReducer.COMPANION_TAG, WindowRequest.Kind.CLOSE), repeated = false),
            plan,
        )
        val pending = WorkspaceReducer.applyPlan(companionUp, plan)
        assertEquals(Surface.INSPECTOR, pending.workspace.pendingAux)
        assertNull(WorkspaceReducer.pendingOpen(pending)) // the companion is still attached
        assertTrue((WorkspaceReducer.planAux(pending, Surface.INSPECTOR) as AuxPlan.CloseFirst).repeated)
        val gone = WorkspaceReducer.detached(pending, Surface.COMPANION)
        assertEquals(WindowRequest(WorkspaceReducer.INSPECTOR_ID, WorkspaceReducer.INSPECTOR_TAG, WindowRequest.Kind.OPEN), WorkspaceReducer.pendingOpen(gone))
        assertEquals(
            WindowRequest.Kind.FOCUS,
            (WorkspaceReducer.planAux(empty.copy(workspace = Workspace(inspectorInstances = 1)), Surface.INSPECTOR) as AuxPlan.Show).request.kind,
        )
        assertEquals(listOf(Surface.COMPANION, Surface.INSPECTOR), WorkspaceReducer.AUXILIARY)
        assertEquals("EverydayFuturesInspector", WorkspaceReducer.INSPECTOR_ID)
        assertEquals("inspector", WorkspaceReducer.INSPECTOR_TAG)
        assertEquals("EverydayFuturesArchive", WorkspaceReducer.ARCHIVE_ID)
    }
}
