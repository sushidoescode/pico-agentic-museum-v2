package dev.local.everydayfutures.museum.state

import dev.local.everydayfutures.museum.catalogue.Catalogue
import dev.local.everydayfutures.museum.catalogue.CatalogueFilter
import dev.local.everydayfutures.museum.catalogue.CatalogueQuery
import dev.local.everydayfutures.museum.catalogue.EmptyReason
import dev.local.everydayfutures.museum.catalogue.Section
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Host checks of the catalogue filter as app state: reducers, markers (the query's length, never its text), what it
 * leaves alone and that it is never persisted. What the grid draws is for the native lane.
 */
class FilterTest {
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

    @Test
    fun eachFilterActionLogsSectionSavedAndQueryLengthOnly() {
        val log = mutableListOf<String>()
        val museum = owner(log)
        museum.setSection(Section.CONNECTED_LIVES, Surface.PRIMARY)
        assertTrue(log.last(), log.last().startsWith("filter section=connected saved=0 query=0 source=primary"))
        museum.setSavedOnly(true, Surface.PRIMARY)
        assertTrue(log.last().startsWith("filter section=connected saved=1 query=0 source=primary"))
        museum.setQuery("Smart", Surface.PRIMARY)
        assertTrue(log.last().startsWith("filter section=connected saved=1 query=5 source=primary"))
        assertTrue(log.none { it.contains("Smart") }) // the text itself never reaches the log
        assertTrue(log.last().contains("filter=connected:1:5"))
        museum.clearQuery(Surface.PRIMARY)
        assertEquals(CatalogueFilter(Section.CONNECTED_LIVES, savedOnly = true), museum.state.filter)
        museum.setSection(null, Surface.PRIMARY)
        assertTrue(log.last().startsWith("filter section=all saved=1 query=0"))
        museum.showAll(Surface.PRIMARY)
        assertEquals(CatalogueFilter(), museum.state.filter)
        val count = log.size
        museum.showAll(Surface.PRIMARY) // nothing to change: no marker, no action
        museum.setQuery("", Surface.PRIMARY)
        assertEquals(count, log.size)
    }

    @Test
    fun aSelectionTheFilterHidesStaysSelectedAndShowBringsItBack() {
        val museum = owner()
        museum.select("rocket", Surface.PRIMARY)
        museum.turn(Turn.RIGHT, Surface.PRIMARY)
        museum.setSection(Section.DAILY_RITUALS, Surface.PRIMARY)
        assertEquals("rocket", museum.state.selectedId) // still selected: study and details keep showing it
        assertEquals(1, museum.state.turnSteps)
        assertTrue(CatalogueQuery.selectionHidden(Catalogue.artifacts, museum.state.filter, museum.state.savedIds, museum.state.selectedId))
        museum.showAll(Surface.PRIMARY) // the note's Show button
        assertFalse(CatalogueQuery.selectionHidden(Catalogue.artifacts, museum.state.filter, museum.state.savedIds, museum.state.selectedId))
        assertEquals("rocket", museum.state.selectedId)
        assertEquals(1, museum.state.turnSteps)
    }

    @Test
    fun filteringChangesNothingButTheFilterAndIsNeverPersisted() {
        val disk = MemoryPersistence()
        val museum = owner(disk = disk)
        museum.select("gascan", Surface.PRIMARY)
        museum.keep(Surface.PRIMARY)
        museum.setReduceTransparency(true, Surface.PRIMARY)
        val before = museum.state
        val saves = disk.saves
        museum.setSection(Section.DOMESTIC_SYSTEMS, Surface.PRIMARY)
        museum.setSavedOnly(true, Surface.PRIMARY)
        museum.setQuery("gas", Surface.PRIMARY)
        assertEquals(before.copy(filter = museum.state.filter), museum.state)
        assertEquals(saves, disk.saves)
        assertEquals(CatalogueFilter(), owner(disk = disk).state.filter) // a new process starts unfiltered
        // Selecting while filtered leaves the filter as it is.
        museum.select("coffeepod", Surface.PRIMARY)
        assertEquals(CatalogueFilter(Section.DOMESTIC_SYSTEMS, savedOnly = true, query = "gas"), museum.state.filter)
    }

    @Test
    fun clearingTheCollectionInTheSavedViewShowsTheEmptyStateAndKeepsSelectionAndPreferences() {
        val museum = owner()
        museum.select("smartphone", Surface.PRIMARY)
        museum.keep(Surface.PRIMARY)
        museum.select("remote", Surface.PRIMARY)
        museum.keep(Surface.PRIMARY)
        museum.setIncreaseContrast(true, Surface.PRIMARY)
        museum.setSavedOnly(true, Surface.PRIMARY)
        assertEquals(listOf("smartphone", "remote"), CatalogueQuery.visible(Catalogue.artifacts, museum.state.filter, museum.state.savedIds).map { it.id })

        museum.collectionClearAsked(Surface.PRIMARY)
        museum.clearCollection(Surface.PRIMARY)
        assertEquals(EmptyReason.NOTHING_SAVED, CatalogueQuery.emptyReason(Catalogue.artifacts, museum.state.filter, museum.state.savedIds))
        assertEquals("remote", museum.state.selectedId)
        assertTrue(museum.state.options.increaseContrast)
        assertTrue(museum.state.filter.savedOnly) // still in the Saved view, which now says it is empty
        museum.showAll(Surface.PRIMARY) // the empty state's Show all
        assertEquals(18, CatalogueQuery.visible(Catalogue.artifacts, museum.state.filter, museum.state.savedIds).size)
    }

    @Test
    fun theSearchTextIsSanitisedByTheReducer() {
        val museum = owner()
        museum.setQuery("coffee\npod" + "x".repeat(200), Surface.PRIMARY)
        assertEquals(CatalogueQuery.MAX_QUERY_LENGTH, museum.state.filter.query.length)
        assertFalse(museum.state.filter.query.contains('\n'))
        assertEquals("filter section=all saved=0 query=64", FilterReducer.marker(museum.state.filter))
    }
}
