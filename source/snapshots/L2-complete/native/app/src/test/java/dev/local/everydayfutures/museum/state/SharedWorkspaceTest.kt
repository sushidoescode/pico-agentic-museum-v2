package dev.local.everydayfutures.museum.state

import dev.local.everydayfutures.museum.catalogue.Catalogue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Host checks of the L2 app-owned state owner and window record, driven through the same controller calls the two
 * window roots make. They establish state and request behaviour, not rendering, placement or any device behaviour.
 */
class SharedWorkspaceTest {
    private class RecordingNavigator : WindowNavigator {
        val calls = mutableListOf<String>()

        override fun open(id: String, tag: String) {
            calls += "open $id $tag"
        }

        override fun close(id: String, tag: String) {
            calls += "close $id $tag"
        }
    }

    private class MemoryPersistence(var stored: MuseumState = MuseumState()) : MuseumPersistence {
        var saves = 0

        override fun load(): MuseumState = MuseumState(savedIds = stored.savedIds, options = stored.options)

        override fun save(state: MuseumState) {
            saves += 1
            stored = state
        }
    }

    private fun owner(persistence: MuseumPersistence = MemoryPersistence(), log: MutableList<String> = mutableListOf()) =
        MuseumController(persistence = persistence, log = { log += it })

    @Test
    fun repeatedOpenRequestsAskForTheSameInstanceAndNeverASecondOne() {
        val log = mutableListOf<String>()
        val museum = owner(log = log)
        val nav = RecordingNavigator()
        museum.windowAttached(Surface.PRIMARY)

        museum.openCompanion(nav, Surface.PRIMARY)
        museum.windowAttached(Surface.COMPANION) // the platform created the window
        museum.openCompanion(nav, Surface.PRIMARY) // pressed again while open
        museum.openCompanion(nav, Surface.COMPANION)

        val expected = "open ${WorkspaceReducer.COMPANION_ID} ${WorkspaceReducer.COMPANION_TAG}"
        assertEquals(listOf(expected, expected, expected), nav.calls)
        assertEquals(1, museum.state.workspace.companionInstances)
        val modes = log.filter { it.startsWith("companion-open") }.map { it.substringAfter("mode=").substringBefore(' ') }
        assertEquals(listOf("open", "focus", "focus"), modes)
    }

    @Test
    fun aDuplicateCompanionIsCountedRatherThanHidden() {
        val log = mutableListOf<String>()
        val museum = owner(log = log)
        museum.windowAttached(Surface.COMPANION)
        museum.windowAttached(Surface.COMPANION)
        assertEquals(2, museum.state.workspace.companionInstances)
        assertTrue(log.last().contains("surface=companion instances=2"))

        museum.windowDetached(Surface.COMPANION)
        museum.windowDetached(Surface.COMPANION)
        museum.windowDetached(Surface.COMPANION) // an unmatched dispose never goes negative
        assertEquals(0, museum.state.workspace.companionInstances)
        assertFalse(museum.state.workspace.companionOpen)
    }

    @Test
    fun aReopenedCompanionShowsWhatChangedWhileItWasClosed() {
        val museum = owner()
        val nav = RecordingNavigator()
        museum.windowAttached(Surface.PRIMARY)
        museum.openCompanion(nav, Surface.PRIMARY)
        museum.windowAttached(Surface.COMPANION)
        museum.select(Catalogue.VACUUM, Surface.COMPANION)
        museum.setReduceTransparency(true, Surface.PRIMARY)

        museum.closeCompanion(nav, Surface.COMPANION)
        museum.windowDetached(Surface.COMPANION)
        val whileClosed = museum.state
        assertEquals(Catalogue.VACUUM, whileClosed.selectedId) // closing changed no logical state

        museum.select(Catalogue.SMART_SPEAKER, Surface.PRIMARY)
        museum.turn(Turn.RIGHT, Surface.PRIMARY)
        museum.openCompanion(nav, Surface.PRIMARY)
        museum.windowAttached(Surface.COMPANION)

        val reopened = museum.state
        assertEquals(Catalogue.SMART_SPEAKER, reopened.selectedId)
        assertEquals(1, reopened.turnSteps)
        assertEquals(View.INSPECT, reopened.view)
        assertTrue(reopened.options.reduceTransparency)
        assertEquals(1, reopened.workspace.companionInstances)
        assertEquals(
            listOf("open", "close", "open").map { "$it ${WorkspaceReducer.COMPANION_ID} ${WorkspaceReducer.COMPANION_TAG}" },
            nav.calls,
        )
    }

    @Test
    fun selectionTurnAndResetAgreeWhicheverWindowActsAndTheMarkerNamesTheWindow() {
        val log = mutableListOf<String>()
        val mixed = owner(log = log)
        mixed.select(Catalogue.VACUUM, Surface.COMPANION)
        mixed.turn(Turn.RIGHT, Surface.PRIMARY)
        mixed.turn(Turn.RIGHT, Surface.COMPANION)
        val turned = mixed.state
        mixed.reset(Surface.COMPANION)

        val single = owner()
        single.select(Catalogue.VACUUM, Surface.PRIMARY)
        single.turn(Turn.RIGHT, Surface.PRIMARY)
        single.turn(Turn.RIGHT, Surface.PRIMARY)
        single.reset(Surface.PRIMARY)

        assertEquals(2, turned.turnSteps)
        assertEquals(single.state, mixed.state)
        assertEquals(Catalogue.VACUUM, mixed.state.selectedId)
        assertTrue(mixed.state.isFrontView)
        assertTrue(log[0].startsWith("select id=vacuum source=companion steps=0 selected=vacuum"))
        assertTrue(log[1].startsWith("turn direction=right source=primary steps=1"))
        assertTrue(log[3].startsWith("reset source=companion steps=0"))
    }

    @Test
    fun onlySavedObjectsAndPreferencesOutliveTheProcess() {
        val disk = MemoryPersistence()
        val first = owner(disk)
        first.windowAttached(Surface.PRIMARY)
        first.windowAttached(Surface.COMPANION)
        first.select(Catalogue.SMART_SPEAKER, Surface.COMPANION)
        first.turn(Turn.LEFT, Surface.COMPANION)
        assertEquals(0, disk.saves) // selection, orientation and the window record are never written

        first.keep(Surface.COMPANION)
        first.setIncreaseContrast(true, Surface.COMPANION)
        assertEquals(2, disk.saves)

        val next = owner(disk) // a new process
        assertEquals(Catalogue.QUENCHER, next.state.selectedId)
        assertEquals(0, next.state.turnSteps)
        assertEquals(View.CATALOGUE, next.state.view)
        assertEquals(Workspace(), next.state.workspace)
        assertEquals(listOf(Catalogue.SMART_SPEAKER), next.state.savedIds)
        assertTrue(next.state.options.increaseContrast)
    }

    @Test
    fun aPreferenceSetInEitherWindowIsTheOneBothRead() {
        val disk = MemoryPersistence()
        val museum = owner(disk)
        museum.setReduceMotion(true, Surface.COMPANION)
        museum.setReduceTransparency(true, Surface.PRIMARY)
        val options = museum.state.options
        assertEquals(ComfortOptions(reduceMotion = true, reduceTransparency = true, increaseContrast = false), options)
        assertEquals(options, disk.stored.options)
        museum.setReduceMotion(false, Surface.PRIMARY)
        assertFalse(museum.state.options.reduceMotion)
        assertTrue(museum.state.options.reduceTransparency)
    }

    @Test
    fun closingTheCompanionLeavesThePrimaryRouteIntact() {
        val museum = owner()
        val nav = RecordingNavigator()
        museum.windowAttached(Surface.PRIMARY)
        museum.windowAttached(Surface.COMPANION)
        museum.turn(Turn.RIGHT, Surface.COMPANION)
        museum.closeCompanion(nav, Surface.PRIMARY)
        museum.windowDetached(Surface.COMPANION)

        assertEquals(1, museum.state.workspace.primaryInstances)
        assertEquals(1, museum.state.turnSteps)
        assertTrue(museum.state.canReset)
        museum.select(Catalogue.VACUUM, Surface.PRIMARY)
        museum.toggleInspect(Surface.PRIMARY)
        assertEquals(Catalogue.VACUUM, museum.state.selectedId)
        assertEquals(View.INSPECT, museum.state.view)
    }

    @Test
    fun closingThePrimaryLeavesTheCompanionAndTheSharedStateIntact() {
        val museum = owner()
        museum.windowAttached(Surface.PRIMARY)
        museum.windowAttached(Surface.COMPANION)
        museum.select(Catalogue.VACUUM, Surface.PRIMARY)
        museum.windowDetached(Surface.PRIMARY)

        assertEquals(0, museum.state.workspace.primaryInstances)
        assertEquals(1, museum.state.workspace.companionInstances)
        museum.turn(Turn.RIGHT, Surface.COMPANION)
        assertEquals(Catalogue.VACUUM, museum.state.selectedId)
        assertEquals(1, museum.state.turnSteps)
    }
}
