package dev.local.everydayfutures.museum.state

import dev.local.everydayfutures.museum.catalogue.Catalogue
import dev.local.everydayfutures.museum.catalogue.CatalogueFilter
import dev.local.everydayfutures.museum.catalogue.Section
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.startCoroutine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Host checks of the immersive archive's app state and the Enter archive / Return flow through the controller, with the
 * Stage calls played by a fake. They establish state, requests and markers only: no Stage has been opened by them, and
 * what the Stage shows is the archive presenter's and the native lane's business.
 */
class ArchiveTest {
    private class RecordingNavigator : WindowNavigator {
        val calls = mutableListOf<String>()

        override fun open(id: String, tag: String) {
            calls += "open $id $tag"
        }

        override fun close(id: String, tag: String) {
            calls += "close $id $tag"
        }
    }

    private class FakeStage(var result: StageOpenResult = StageOpenResult.Allowed, var throwOnOpen: Throwable? = null) : StageNavigator {
        val calls = mutableListOf<String>()

        override suspend fun open(id: String): StageOpenResult {
            calls += "open $id"
            throwOnOpen?.let { throw it }
            return result
        }

        override suspend fun close() {
            calls += "close"
        }

        override fun minimize(id: String, tag: String?): Boolean {
            calls += "minimize $id $tag"
            return true
        }

        override fun restore(id: String, tag: String?): Boolean {
            calls += "restore $id $tag"
            return true
        }
    }

    private class MemoryPersistence : MuseumPersistence {
        var saves = 0

        override fun load(): MuseumState = MuseumState()

        override fun save(state: MuseumState) {
            saves += 1
        }
    }

    /** Runs a suspend block that never actually suspends (the fakes return at once). */
    private fun <T> runNow(block: suspend () -> T): T {
        var outcome: Result<T>? = null
        block.startCoroutine(Continuation(EmptyCoroutineContext) { outcome = it })
        return checkNotNull(outcome) { "suspended" }.getOrThrow()
    }

    private fun owner(log: MutableList<String> = mutableListOf(), disk: MemoryPersistence = MemoryPersistence()) =
        MuseumController(persistence = disk, log = { log += it })

    private fun open(state: MuseumState): MuseumState = ArchiveReducer.openResult(ArchiveReducer.enterRequested(state), true, "ok")

    @Test
    fun theReducersFollowThePhases() {
        val fresh = MuseumState()
        assertEquals(ArchiveState(), fresh.archive)
        val opening = ArchiveReducer.enterRequested(fresh)
        assertEquals(ArchiveState(phase = ArchivePhase.OPENING), opening.archive)
        assertSame(opening, ArchiveReducer.enterRequested(opening)) // ignored unless closed

        val opened = ArchiveReducer.openResult(opening, allowed = true, reason = "ok")
        assertEquals(ArchivePhase.OPEN, opened.archive.phase)
        assertSame(opened, ArchiveReducer.openResult(opened, allowed = false, reason = "late")) // only while opening
        assertSame(fresh, ArchiveReducer.openResult(fresh, allowed = true, reason = "ok"))

        val refused = ArchiveReducer.openResult(opening, allowed = false, reason = "not-allowed")
        assertEquals(ArchiveState(phase = ArchivePhase.CLOSED, lastRefusal = "not-allowed"), refused.archive)

        val bay = ArchiveReducer.goTo(opened, ArchiveViewpoint.CONNECTED)
        assertEquals(ArchiveViewpoint.CONNECTED, bay.archive.viewpoint)
        assertSame(fresh, ArchiveReducer.goTo(fresh, ArchiveViewpoint.DAILY)) // OPEN only
        assertSame(opening, ArchiveReducer.goTo(opening, ArchiveViewpoint.DAILY))

        val closing = ArchiveReducer.returnRequested(bay)
        assertEquals(ArchivePhase.CLOSING, closing.archive.phase)
        assertEquals(ArchivePhase.CLOSING, ArchiveReducer.returnRequested(opening).archive.phase)
        assertSame(fresh, ArchiveReducer.returnRequested(fresh))
        val closed = ArchiveReducer.closed(closing)
        assertEquals(ArchiveState(), closed.archive)
        assertSame(fresh, ArchiveReducer.closed(fresh))
        assertEquals(listOf("arrival", "daily", "connected", "domestic"), ArchiveViewpoint.values().map { it.key })
    }

    @Test
    fun focusSelectsTheObjectAndMovesToItsBay() {
        val opened = open(MuseumState(filter = CatalogueFilter(section = Section.DAILY_RITUALS), savedIds = listOf("rocket")))
        val onRocket = ArchiveReducer.focus(opened, "rocket")
        assertEquals("rocket", onRocket.selectedId)
        assertEquals(ArchiveViewpoint.DOMESTIC, onRocket.archive.viewpoint)
        assertEquals("rocket", onRocket.archive.focusedId)
        assertEquals(opened.filter, onRocket.filter) // the catalogue filter is untouched
        assertEquals(opened.savedIds, onRocket.savedIds)
        assertEquals(opened.options, onRocket.options)
        assertEquals(ArchiveViewpoint.CONNECTED, ArchiveReducer.focus(opened, "earbuds").archive.viewpoint)
        assertEquals(ArchiveViewpoint.DAILY, ArchiveReducer.focus(opened, "coffeepod").archive.viewpoint)
        assertNull(ArchiveReducer.goTo(onRocket, ArchiveViewpoint.ARRIVAL).archive.focusedId) // a move clears the focus
        assertSame(opened, ArchiveReducer.focus(opened, "astronaut"))
        val closed = MuseumState()
        assertSame(closed, ArchiveReducer.focus(closed, "rocket")) // OPEN only
    }

    @Test
    fun theArchiveReducersLeaveTheRestOfTheMuseumAlone() {
        val start = MuseumState(
            selectedId = "router",
            turnSteps = 3,
            view = View.INSPECT,
            savedIds = listOf("router"),
            options = ComfortOptions(reduceMotion = true),
            filter = CatalogueFilter(savedOnly = true, query = "ro"),
            story = null,
        )
        var s = ArchiveReducer.enterRequested(start)
        s = ArchiveReducer.openResult(s, true, "ok")
        s = ArchiveReducer.goTo(s, ArchiveViewpoint.DOMESTIC)
        s = ArchiveReducer.returnRequested(s)
        s = ArchiveReducer.closed(s)
        assertEquals(start, s)
    }

    @Test
    fun enterArchiveClosesAnAuxiliaryWindowFirstThenOpensTheFullStage() {
        val log = mutableListOf<String>()
        val museum = owner(log)
        val windows = RecordingNavigator()
        val stage = FakeStage()
        museum.windowAttached(Surface.PRIMARY)
        museum.openInspector(windows, Surface.PRIMARY)
        museum.windowAttached(Surface.INSPECTOR)

        assertTrue(museum.enterArchive(windows, Surface.PRIMARY))
        assertEquals(ArchivePhase.OPENING, museum.state.archive.phase)
        assertEquals("close ${WorkspaceReducer.INSPECTOR_ID} ${WorkspaceReducer.INSPECTOR_TAG}", windows.calls.last())
        assertTrue(log.any { it.startsWith("archive-enter request source=primary") })
        runNow { museum.completeArchiveOpen(stage, Surface.PRIMARY) }
        assertEquals(listOf("open ${WorkspaceReducer.ARCHIVE_ID}"), stage.calls)
        assertEquals(ArchivePhase.OPEN, museum.state.archive.phase)
        assertTrue(log.last().startsWith("archive-open result=allowed reason=ok source=primary"))

        museum.windowAttached(Surface.ARCHIVE)
        assertEquals(1, museum.state.workspace.archiveInstances)
        museum.archiveGoTo(ArchiveViewpoint.DAILY, Surface.ARCHIVE)
        assertTrue(log.last().startsWith("archive-goto viewpoint=daily source=archive"))
        museum.archiveFocus("fidget", Surface.ARCHIVE)
        assertTrue(log.last().startsWith("archive-focus id=fidget source=archive"))
        assertEquals("fidget", museum.state.selectedId)

        // Pressed again while open: ignored, nothing else requested.
        assertFalse(museum.enterArchive(windows, Surface.PRIMARY))
        assertTrue(log.last().startsWith("archive-enter request ignored=open"))

        runNow { museum.leaveArchive(stage, Surface.ARCHIVE) }
        assertEquals("close", stage.calls.last())
        assertEquals(ArchivePhase.CLOSED, museum.state.archive.phase)
        assertTrue(log.any { it.startsWith("archive-return source=archive") })
        assertTrue(log.last().startsWith("archive-closed source=archive"))
        museum.windowDetached(Surface.ARCHIVE) // the Stage root goes: already closed, nothing more is logged
        assertEquals(1, log.count { it.startsWith("archive-closed") })
        assertEquals(0, museum.state.workspace.archiveInstances)
    }

    @Test
    fun aRefusedOrFailedOpenReturnsToClosedWithTheReason() {
        val log = mutableListOf<String>()
        val museum = owner(log)
        val windows = RecordingNavigator()
        museum.enterArchive(windows, Surface.PRIMARY)
        runNow { museum.completeArchiveOpen(FakeStage(StageOpenResult.NotAllowed), Surface.PRIMARY) }
        assertEquals(ArchiveState(phase = ArchivePhase.CLOSED, lastRefusal = "not-allowed"), museum.state.archive)
        assertTrue(log.last().startsWith("archive-open result=not-allowed reason=not-allowed"))

        museum.enterArchive(windows, Surface.PRIMARY)
        assertNull(museum.state.archive.lastRefusal) // a new attempt clears the old note
        runNow { museum.completeArchiveOpen(FakeStage(StageOpenResult.Error(2, "")), Surface.PRIMARY) }
        assertEquals("code-2", museum.state.archive.lastRefusal)
        assertTrue(log.last().startsWith("archive-open result=error reason=code-2 "))

        museum.enterArchive(windows, Surface.PRIMARY)
        runNow { museum.completeArchiveOpen(FakeStage(throwOnOpen = IllegalStateException("Stage not registered")), Surface.PRIMARY) }
        assertEquals(ArchivePhase.CLOSED, museum.state.archive.phase)
        assertEquals("code-${MuseumController.CODE_EXCEPTION}-IllegalStateException", museum.state.archive.lastRefusal)
        assertTrue(windows.calls.isEmpty()) // nothing auxiliary was open, so nothing was closed
    }

    @Test
    fun aCancelledOpenIsRecordedAndRethrown() {
        val log = mutableListOf<String>()
        val museum = owner(log)
        museum.enterArchive(RecordingNavigator(), Surface.PRIMARY)
        var rethrown = false
        try {
            runNow { museum.completeArchiveOpen(FakeStage(throwOnOpen = CancellationException("window gone")), Surface.PRIMARY) }
        } catch (e: CancellationException) {
            rethrown = true
        }
        assertTrue(rethrown)
        assertEquals(ArchivePhase.CLOSED, museum.state.archive.phase)
        assertTrue(log.last().startsWith("archive-open result=error reason=code-${MuseumController.CODE_CANCELLED}-cancelled"))
    }

    @Test
    fun theStageRootGoingAwayClosesTheRecord() {
        val log = mutableListOf<String>()
        val museum = owner(log)
        museum.enterArchive(RecordingNavigator(), Surface.PRIMARY)
        museum.archiveOpenResult(StageOpenResult.Allowed)
        museum.windowAttached(Surface.ARCHIVE)
        museum.windowDetached(Surface.ARCHIVE) // closed from the system menu, not by Return
        assertEquals(ArchivePhase.CLOSED, museum.state.archive.phase)
        assertTrue(log.last().startsWith("archive-closed source=archive"))
    }

    @Test
    fun enteringAndLeavingKeepsSelectionSavedPreferencesAndFilter() {
        val disk = MemoryPersistence()
        val museum = owner(disk = disk)
        museum.select("earbuds", Surface.PRIMARY)
        museum.keep(Surface.PRIMARY)
        museum.setIncreaseContrast(true, Surface.PRIMARY)
        museum.setSection(Section.CONNECTED_LIVES, Surface.PRIMARY)
        val before = museum.state
        val saves = disk.saves
        museum.enterArchive(RecordingNavigator(), Surface.PRIMARY)
        runNow { museum.completeArchiveOpen(FakeStage(), Surface.PRIMARY) }
        museum.archiveGoTo(ArchiveViewpoint.DOMESTIC, Surface.ARCHIVE)
        runNow { museum.leaveArchive(FakeStage(), Surface.ARCHIVE) }
        assertEquals(before, museum.state)
        assertEquals(saves, disk.saves) // the archive is never persisted
    }

    /** An owner whose archive is OPEN, as after Enter archive and an allowed Stage open. */
    private fun openOwner(log: MutableList<String> = mutableListOf(), disk: MemoryPersistence = MemoryPersistence()): MuseumController {
        val museum = owner(log, disk)
        museum.enterArchive(RecordingNavigator(), Surface.PRIMARY)
        museum.archiveOpenResult(StageOpenResult.Allowed)
        return museum
    }

    @Test
    fun aTapFocusesFirstThenTurnsTheFocusedExhibitByOneStep() {
        val log = mutableListOf<String>()
        val museum = openOwner(log)
        museum.turn(Turn.RIGHT, Surface.PRIMARY) // the study's own turn, which focus resets and the room never touches
        museum.archiveTap("doorbell", onDisplay = true, Surface.ARCHIVE)
        assertTrue(log.last().startsWith("archive-focus id=doorbell source=archive"))
        assertEquals("doorbell", museum.state.archive.focusedId)
        assertEquals(0, museum.state.archive.turnSteps)

        museum.archiveTap("doorbell", onDisplay = true, Surface.ARCHIVE)
        assertTrue(log.last().startsWith("archive-turn id=doorbell step=1 source=archive"))
        assertEquals(1, museum.state.archive.turnSteps)
        assertEquals(45f, museum.state.archive.turnDegrees, 0f)
        assertEquals("doorbell", museum.state.selectedId)
        assertEquals(ArchiveViewpoint.DOMESTIC, museum.state.archive.viewpoint) // a turn is not a move
        assertEquals(0, museum.state.turnSteps)
        assertEquals(View.CATALOGUE, museum.state.view)

        museum.archiveTurn("doorbell", Surface.ARCHIVE) // the panel's Turn 45°
        assertTrue(log.last().startsWith("archive-turn id=doorbell step=2 source=archive"))
        assertEquals(90f, museum.state.archive.turnDegrees, 0f)
    }

    @Test
    fun aFloorCircleStepsUpLikeAFirstTapAndNeverTurns() {
        val log = mutableListOf<String>()
        val museum = openOwner(log)
        val tapped = openOwner()
        museum.archiveStep("gascan", Surface.ARCHIVE)
        tapped.archiveTap("gascan", onDisplay = true, Surface.ARCHIVE)
        assertTrue(log.last().startsWith("archive-focus id=gascan source=archive"))
        assertEquals(tapped.state.archive, museum.state.archive) // the same focus action as a first tap on the exhibit
        assertEquals(tapped.state.selectedId, museum.state.selectedId)
        assertEquals(ArchiveViewpoint.DOMESTIC, museum.state.archive.viewpoint)

        // The focused exhibit's circle is hidden; a step on it anyway changes nothing and logs nothing, and never turns.
        val before = museum.state
        val markers = log.size
        museum.archiveStep("gascan", Surface.ARCHIVE)
        assertSame(before, museum.state)
        assertEquals(markers, log.size)

        museum.archiveTap("gascan", onDisplay = true, Surface.ARCHIVE) // the exhibit itself still turns
        assertEquals(1, museum.state.archive.turnSteps)
        museum.archiveStep("quencher", Surface.ARCHIVE) // across the room, to another bay, at its front
        assertEquals("quencher", museum.state.archive.focusedId)
        assertEquals(ArchiveViewpoint.DAILY, museum.state.archive.viewpoint)
        assertEquals(0, museum.state.archive.turnSteps)

        museum.archiveStep("no-such-object", Surface.ARCHIVE)
        museum.archiveReturn(Surface.ARCHIVE)
        val closing = museum.state
        museum.archiveStep("rocket", Surface.ARCHIVE) // CLOSING: no move
        assertSame(closing, museum.state)
        assertEquals("quencher", closing.archive.focusedId)
        assertTrue(log.none { it.startsWith("archive-focus id=no-such-object") || it.startsWith("archive-focus id=rocket") })
    }

    @Test
    fun anExhibitNotOnDisplayIsFocusedButNeverTurned() {
        val log = mutableListOf<String>()
        val museum = openOwner(log)
        museum.archiveTap("rocket", onDisplay = false, Surface.ARCHIVE)
        museum.archiveTap("rocket", onDisplay = false, Surface.ARCHIVE)
        assertEquals("rocket", museum.state.archive.focusedId)
        assertEquals(0, museum.state.archive.turnSteps)
        assertEquals(2, log.count { it.startsWith("archive-focus id=rocket") })
        assertTrue(log.none { it.startsWith("archive-turn") })
    }

    @Test
    fun turningNeedsTheSameFocusedExhibitWhileOpen() {
        val opened = open(MuseumState())
        assertSame(opened, ArchiveReducer.turn(opened, "rocket")) // nothing in focus
        val onRocket = ArchiveReducer.focus(opened, "rocket")
        assertSame(onRocket, ArchiveReducer.turn(onRocket, "gascan")) // another exhibit
        val closing = ArchiveReducer.returnRequested(onRocket)
        assertSame(closing, ArchiveReducer.turn(closing, "rocket")) // CLOSING
        val closed = MuseumState()
        assertSame(closed, ArchiveReducer.turn(closed, "rocket")) // CLOSED
        assertFalse(ArchiveReducer.wouldTurn(closing, "rocket"))
        assertTrue(ArchiveReducer.wouldTurn(onRocket, "rocket"))

        val log = mutableListOf<String>()
        val museum = openOwner(log)
        museum.archiveFocus("rocket", Surface.ARCHIVE)
        val before = museum.state
        val markers = log.size
        museum.archiveTurn("gascan", Surface.ARCHIVE)
        assertSame(before, museum.state)
        assertEquals(markers, log.size) // a refused turn logs nothing
    }

    @Test
    fun aNewFocusBackToTheBayOrLeavingFacesTheFront() {
        val turned = ArchiveReducer.turn(ArchiveReducer.focus(open(MuseumState()), "doorbell"), "doorbell")
        assertEquals(1, turned.archive.turnSteps)
        assertEquals(0, ArchiveReducer.focus(turned, "router").archive.turnSteps)
        assertEquals(0, ArchiveReducer.goTo(turned, ArchiveViewpoint.DOMESTIC).archive.turnSteps)
        assertEquals(0, ArchiveReducer.closed(ArchiveReducer.returnRequested(turned)).archive.turnSteps)
        // Back on the doorbell after another focus: at its front again.
        val back = ArchiveReducer.focus(ArchiveReducer.focus(turned, "router"), "doorbell")
        assertEquals(0, back.archive.turnSteps)
    }

    @Test
    fun refocusingTheSameExhibitFromThePanelKeepsItsTurn() {
        val turned = ArchiveReducer.turn(ArchiveReducer.focus(open(MuseumState()), "scooter"), "scooter")
        val again = ArchiveReducer.focus(turned, "scooter")
        assertEquals(1, again.archive.turnSteps)
        assertEquals("scooter", again.archive.focusedId)
    }

    @Test
    fun onlyATapThatFocusesAnotherExhibitReadsTheHead() {
        val opened = open(MuseumState())
        assertTrue(ArchiveReducer.wouldFocusAnew(opened, "rocket")) // from Arrival
        val onRocket = ArchiveReducer.focus(opened, "rocket")
        assertFalse(ArchiveReducer.wouldFocusAnew(onRocket, "rocket")) // the exhibit in focus: a turn, not a move
        assertTrue(ArchiveReducer.wouldFocusAnew(onRocket, "gascan"))
        assertTrue(ArchiveReducer.wouldFocusAnew(ArchiveReducer.goTo(onRocket, ArchiveViewpoint.DOMESTIC), "rocket")) // from its bay
        assertFalse(ArchiveReducer.wouldFocusAnew(onRocket, "no-such-object"))
        assertFalse(ArchiveReducer.wouldFocusAnew(ArchiveReducer.returnRequested(onRocket), "gascan")) // CLOSING
        assertFalse(ArchiveReducer.wouldFocusAnew(MuseumState(), "rocket")) // CLOSED
        // It is true exactly when focus changes the exhibit in focus, for every object and every kind of state.
        val states = listOf(MuseumState(), opened, onRocket, ArchiveReducer.turn(onRocket, "rocket"), ArchiveReducer.returnRequested(onRocket))
        for (state in states) {
            for (id in Catalogue.artifacts.map { it.id } + "no-such-object") {
                val moves = ArchiveReducer.focus(state, id).archive.focusedId != state.archive.focusedId
                assertEquals("$id from ${state.archive}", moves, ArchiveReducer.wouldFocusAnew(state, id))
            }
        }
    }

    @Test
    fun eightStepsComeBackToTheFront() {
        var s = ArchiveReducer.focus(open(MuseumState()), "vacuum")
        repeat(ArchiveState.STEPS_PER_TURN) { s = ArchiveReducer.turn(s, "vacuum") }
        assertEquals(8, s.archive.turnSteps)
        assertEquals(360f, s.archive.turnDegrees, 0f)
        assertEquals(0, Math.floorMod(s.archive.turnSteps, ArchiveState.STEPS_PER_TURN))
        assertEquals(360f, ArchiveState.STEPS_PER_TURN * ArchiveState.TURN_STEP_DEGREES, 0f)
    }

    @Test
    fun turningLeavesSavedPreferencesFilterAndStoryAlone() {
        val disk = MemoryPersistence()
        val museum = openOwner(disk = disk)
        museum.setReduceMotion(true, Surface.PRIMARY)
        museum.setSection(Section.DOMESTIC_SYSTEMS, Surface.PRIMARY)
        museum.archiveFocus("vacuum", Surface.ARCHIVE)
        museum.keep(Surface.ARCHIVE)
        val before = museum.state
        val saves = disk.saves
        museum.archiveTap("vacuum", onDisplay = true, Surface.ARCHIVE)
        assertEquals(before.copy(archive = before.archive.copy(turnSteps = 1)), museum.state)
        assertEquals(saves, disk.saves) // a turn is never persisted
    }

    @Test
    fun theRoomShortlistToggleSavesAndRemovesTheExhibitInFocus() {
        val log = mutableListOf<String>()
        val disk = MemoryPersistence()
        val museum = openOwner(log, disk)
        museum.select("quencher", Surface.PRIMARY)
        museum.keep(Surface.PRIMARY)
        museum.archiveFocus("gascan", Surface.ARCHIVE) // focus selects it, so Save acts on what the card shows
        museum.keep(Surface.ARCHIVE)
        assertTrue(log.last().startsWith("keep id=gascan source=archive"))
        assertEquals(listOf("quencher", "gascan"), museum.state.savedIds)
        museum.unkeep(Surface.ARCHIVE)
        assertTrue(log.last().startsWith("unkeep id=gascan source=archive"))
        assertEquals(listOf("quencher"), museum.state.savedIds)
        assertEquals(3, disk.saves)
        assertEquals("gascan", museum.state.archive.focusedId) // the close look stays
    }

    @Test
    fun stageResultsMapToMarkerWords() {
        assertEquals("allowed" to "ok", StageOpenResult.Allowed.resultWord to StageOpenResult.Allowed.reasonWord)
        assertEquals("not-allowed" to "not-allowed", StageOpenResult.NotAllowed.resultWord to StageOpenResult.NotAllowed.reasonWord)
        assertEquals("error" to "code-1-no-space", StageOpenResult.Error(1, " no space ").let { it.resultWord to it.reasonWord })
        assertEquals("code--2", StageOpenResult.Error(-2, "").reasonWord)
    }
}
