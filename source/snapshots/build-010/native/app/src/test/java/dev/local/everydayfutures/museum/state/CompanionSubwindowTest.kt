package dev.local.everydayfutures.museum.state

import dev.local.everydayfutures.museum.catalogue.Catalogue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Host checks of the companion's Subwindow host (CompanionHost.SUBWINDOW, a prototype): its open and close set the flag
 * the primary composes the Subwindow on instead of calling the navigator, while the one-auxiliary-window policy, its
 * markers and the inspector's navigator calls stay the same. The Subwindow's root is played by hand like a window's.
 * The window host is checked by AuxWindowTest and the others, which use the default and are unchanged.
 */
class CompanionSubwindowTest {
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

    private fun owner(log: MutableList<String>) =
        MuseumController(persistence = MemoryPersistence(), log = { log += it }, clock = { 0L }, companionHost = CompanionHost.SUBWINDOW)

    private val MuseumController.requested: Boolean get() = state.workspace.companionRequested

    @Test
    fun openingTheCompanionRequestsTheSubwindowAndCallsNoNavigator() {
        val log = mutableListOf<String>()
        val museum = owner(log)
        val nav = RecordingNavigator()
        museum.windowAttached(Surface.PRIMARY)
        museum.openCompanion(nav, Surface.PRIMARY)
        assertTrue(museum.requested)
        assertTrue(log.last().startsWith("companion-open request=#1 mode=open tag=companion instances=0 source=primary"))

        museum.windowAttached(Surface.COMPANION) // the primary composed the Subwindow and its root
        assertNull(museum.state.workspace.opening(0L))
        museum.closeCompanion(nav, Surface.COMPANION) // its own Close
        assertFalse(museum.requested)
        assertTrue(log.last().startsWith("companion-close request=#2 tag=companion instances=1 source=companion"))
        museum.windowDetached(Surface.COMPANION)
        assertEquals(0, museum.state.workspace.companionInstances)
        assertTrue(nav.calls.isEmpty())
    }

    @Test
    fun aRepeatedOpenWhileAttachedIsAFocusAndChangesNothing() {
        val log = mutableListOf<String>()
        val museum = owner(log)
        val nav = RecordingNavigator()
        museum.windowAttached(Surface.PRIMARY)
        museum.openCompanion(nav, Surface.PRIMARY)
        museum.windowAttached(Surface.COMPANION)
        val before = museum.state

        museum.openCompanion(nav, Surface.PRIMARY)
        assertTrue(log.last().startsWith("companion-open request=#2 mode=focus tag=companion instances=1 source=primary"))
        assertTrue(museum.requested)
        assertEquals(before, museum.state)
        assertTrue(nav.calls.isEmpty())
    }

    @Test
    fun openIn3dClosesTheSubwindowFirstAndOpensTheInspectorOnlyAfterItsDetach() {
        val log = mutableListOf<String>()
        val museum = owner(log)
        val nav = RecordingNavigator()
        museum.windowAttached(Surface.PRIMARY)
        museum.openCompanion(nav, Surface.PRIMARY)
        museum.windowAttached(Surface.COMPANION)

        museum.openInspector(nav, Surface.PRIMARY)
        assertFalse(museum.requested) // the primary stops composing the Subwindow
        assertEquals(Surface.INSPECTOR, museum.state.workspace.pendingAux)
        assertTrue(nav.calls.isEmpty()) // no inspector yet
        assertTrue(log.any { it.startsWith("aux-switch from=companion to=inspector step=close source=primary") })

        museum.windowDetached(Surface.COMPANION)
        assertEquals(listOf(openInspector), nav.calls)
        museum.windowAttached(Surface.INSPECTOR)
        assertEquals(listOf(Surface.INSPECTOR), museum.state.workspace.attachedAux)
        assertFalse(museum.requested)
    }

    @Test
    fun storyWhileTheInspectorIsOpenRequestsTheSubwindowAfterTheInspectorDetaches() {
        val museum = owner(mutableListOf())
        val nav = RecordingNavigator()
        museum.windowAttached(Surface.PRIMARY)
        museum.select(Catalogue.VACUUM, Surface.PRIMARY)
        museum.openInspector(nav, Surface.PRIMARY)
        museum.windowAttached(Surface.INSPECTOR)

        museum.revealStory(nav, Surface.PRIMARY) // the story is set up at once; the guide waits for the inspector to go
        val story = StoryState(Catalogue.VACUUM, 0, EntryContext(View.CATALOGUE, 0))
        assertEquals(story, museum.state.story)
        assertEquals(listOf(openInspector, closeInspector), nav.calls)
        assertFalse(museum.requested)

        museum.windowDetached(Surface.INSPECTOR)
        assertTrue(museum.requested)
        assertEquals(listOf(openInspector, closeInspector), nav.calls) // no companion call to the navigator
        museum.windowAttached(Surface.COMPANION)
        assertEquals(listOf(Surface.COMPANION), museum.state.workspace.attachedAux)
        assertEquals(story, museum.state.story)
    }

    @Test
    fun enterArchiveClosesTheSubwindow() {
        val log = mutableListOf<String>()
        val museum = owner(log)
        val nav = RecordingNavigator()
        museum.windowAttached(Surface.PRIMARY)
        museum.openCompanion(nav, Surface.PRIMARY)
        museum.windowAttached(Surface.COMPANION)

        assertTrue(museum.enterArchive(nav, Surface.PRIMARY))
        assertFalse(museum.requested)
        assertTrue(log.any { it.startsWith("companion-close request=#2 tag=companion instances=1 source=primary") })
        museum.windowDetached(Surface.COMPANION)
        assertFalse(museum.requested) // nothing reopens it
        assertTrue(nav.calls.isEmpty())
    }

    @Test
    fun enterArchiveBeforeTheSubwindowAttachesClosesItOnceItDoes() {
        val museum = owner(mutableListOf())
        val nav = RecordingNavigator()
        museum.windowAttached(Surface.PRIMARY)
        museum.openCompanion(nav, Surface.PRIMARY) // requested; its root has not attached yet
        assertTrue(museum.enterArchive(nav, Surface.PRIMARY))
        assertTrue(museum.requested) // nothing attached to close yet
        museum.windowAttached(Surface.COMPANION)
        assertFalse(museum.requested)
        museum.windowDetached(Surface.COMPANION)
        assertFalse(museum.requested)
        assertTrue(nav.calls.isEmpty())
    }

    @Test
    fun primaryDetachDropsTheSubwindowRequestAndKeepsLogicalState() {
        val museum = owner(mutableListOf())
        val nav = RecordingNavigator()
        museum.windowAttached(Surface.PRIMARY)
        museum.select(Catalogue.VACUUM, Surface.PRIMARY)
        museum.keep(Surface.PRIMARY)
        museum.revealStory(nav, Surface.PRIMARY)
        museum.windowAttached(Surface.COMPANION)
        museum.setReduceMotion(true, Surface.COMPANION)
        val before = museum.state
        assertTrue(before.workspace.companionRequested)

        museum.windowDetached(Surface.PRIMARY) // the system closed the primary, and the Subwindow went with it
        assertFalse(museum.requested)
        museum.windowDetached(Surface.COMPANION)
        assertEquals(before.selectedId, museum.state.selectedId)
        assertEquals(before.savedIds, museum.state.savedIds)
        assertEquals(before.options, museum.state.options)
        assertEquals(before.story, museum.state.story)
        assertTrue(nav.calls.isEmpty())
    }

    @Test
    fun onlyTheLastPrimaryDetachDropsTheRequest() {
        val requested = WorkspaceReducer.companionRequested(MuseumState().copy(workspace = Workspace(primaryInstances = 2)), true)
        assertSame(requested, WorkspaceReducer.companionRequested(requested, true))
        val one = WorkspaceReducer.detached(requested, Surface.PRIMARY)
        assertTrue(one.workspace.companionRequested)
        assertTrue(WorkspaceReducer.detached(one, Surface.COMPANION).workspace.companionRequested)
        assertFalse(WorkspaceReducer.detached(one, Surface.PRIMARY).workspace.companionRequested)
    }

    @Test
    fun theWindowHostOpensTheContainerAndNeverRequestsTheSubwindow() {
        assertEquals(
            "accepted builds use the window host; only the separately identified prototype flips it",
            CompanionHost.WINDOW,
            CompanionHosting.HOST,
        )
        val museum = MuseumController(persistence = MemoryPersistence(), clock = { 0L }) // the default host
        val nav = RecordingNavigator()
        museum.windowAttached(Surface.PRIMARY)
        museum.openCompanion(nav, Surface.PRIMARY)
        assertFalse(museum.requested)
        museum.windowAttached(Surface.COMPANION)
        museum.closeCompanion(nav, Surface.COMPANION)
        museum.windowDetached(Surface.COMPANION)
        assertEquals(listOf(openCompanion, closeCompanion), nav.calls)
        assertFalse(museum.requested)
    }
}
