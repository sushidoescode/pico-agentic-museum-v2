package dev.local.everydayfutures.museum.state

import dev.local.everydayfutures.museum.catalogue.Catalogue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Host checks of the confirmed collection reset (design-doc "Reset saved collection"), driven through the shared
 * controller both windows use. They establish state, persistence and markers, not the dialog on the device.
 */
class CollectionResetTest {
    private class MemoryPersistence : MuseumPersistence {
        var stored = MuseumState()
        var saves = 0

        override fun load(): MuseumState = MuseumState(savedIds = stored.savedIds, options = stored.options)

        override fun save(state: MuseumState) {
            saves += 1
            stored = state
        }
    }

    private object NoWindows : WindowNavigator {
        override fun open(id: String, tag: String) = Unit

        override fun close(id: String, tag: String) = Unit
    }

    /** Two objects kept, comfort preferences on, the study turned and a story open at chapter 2 in the companion. */
    private fun demoState(disk: MemoryPersistence, log: MutableList<String>): MuseumController {
        val museum = MuseumController(persistence = disk, log = { log += it })
        museum.windowAttached(Surface.PRIMARY)
        museum.select(Catalogue.VACUUM, Surface.PRIMARY)
        museum.keep(Surface.PRIMARY)
        museum.select(Catalogue.SMART_SPEAKER, Surface.PRIMARY)
        museum.keep(Surface.PRIMARY)
        museum.setReduceMotion(true, Surface.PRIMARY)
        museum.setIncreaseContrast(true, Surface.PRIMARY)
        museum.select(Catalogue.VACUUM, Surface.PRIMARY)
        museum.turn(Turn.RIGHT, Surface.PRIMARY)
        museum.revealStory(NoWindows, Surface.PRIMARY)
        museum.windowAttached(Surface.COMPANION)
        museum.nextChapter(Surface.COMPANION)
        return museum
    }

    @Test
    fun keepingTheCollectionAtTheConfirmationChangesNothing() {
        val disk = MemoryPersistence()
        val log = mutableListOf<String>()
        val museum = demoState(disk, log)
        val before = museum.state
        val savesBefore = disk.saves
        museum.collectionClearAsked(Surface.PRIMARY)
        museum.collectionClearCancelled(Surface.PRIMARY)
        assertEquals(before, museum.state)
        assertEquals(savesBefore, disk.saves)
        assertTrue(log.last().startsWith("collection-clear-cancel source=primary steps=1 selected=vacuum view=inspect saved=2"))
    }

    @Test
    fun startFreshClearsOnlyTheSavedObjectsInBothWindowsAndAcrossARelaunch() {
        val disk = MemoryPersistence()
        val log = mutableListOf<String>()
        val museum = demoState(disk, log)
        val before = museum.state
        museum.collectionClearAsked(Surface.PRIMARY)
        museum.clearCollection(Surface.PRIMARY)

        // Only the saved objects change: selection, study, story (chapter 2), windows and preferences are kept.
        assertEquals(before.copy(savedIds = emptyList()), museum.state)
        assertEquals(StoryState(Catalogue.VACUUM, 1, EntryContext(View.INSPECT, 1)), museum.state.story)
        assertTrue(log.last().startsWith("collection-clear cleared=2 source=primary steps=1 selected=vacuum view=inspect saved=0 opts=m1t0c1 story=vacuum:2"))

        // Both windows render the one owner, so both report the same state.
        museum.windowShows(Surface.PRIMARY)
        museum.windowShows(Surface.COMPANION)
        val shows = log.takeLast(2).map { it.substringAfter(" surface=").substringAfter(' ') }
        assertEquals(shows[0], shows[1])
        assertTrue(shows[0].contains("saved=0"))

        // Persisted: a relaunch (a new owner over the same storage) has no saved objects and the same preferences.
        assertTrue(disk.stored.savedIds.isEmpty())
        val relaunched = MuseumController(persistence = disk)
        assertTrue(relaunched.state.savedIds.isEmpty())
        assertEquals(before.options, relaunched.state.options)
        assertNull(relaunched.state.story) // stories never outlive the process, as before
    }
}
