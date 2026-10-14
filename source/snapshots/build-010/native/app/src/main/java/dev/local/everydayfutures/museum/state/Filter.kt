package dev.local.everydayfutures.museum.state

import dev.local.everydayfutures.museum.catalogue.CatalogueFilter
import dev.local.everydayfutures.museum.catalogue.CatalogueQuery
import dev.local.everydayfutures.museum.catalogue.Section

/**
 * Total reducers for the catalogue filter. The filter only changes what the grid shows: a selection it hides stays
 * selected (the study and the details keep showing it), and nothing here touches the saved collection, the
 * preferences, the study, the story or the windows. Never persisted.
 */
object FilterReducer {
    /** One section, or every section when [section] is null. */
    fun section(state: MuseumState, section: Section?): MuseumState = change(state) { it.copy(section = section) }

    /** The All / Saved view. */
    fun savedOnly(state: MuseumState, savedOnly: Boolean): MuseumState = change(state) { it.copy(savedOnly = savedOnly) }

    /** The search text, kept to one line and a bounded length. */
    fun query(state: MuseumState, query: String): MuseumState = change(state) { it.copy(query = CatalogueQuery.sanitize(query)) }

    /** Clear search: only the text; the section and the view stay. */
    fun clearQuery(state: MuseumState): MuseumState = change(state) { it.copy(query = "") }

    /** Show / Show all: every object again (section, view and search all reset). */
    fun showAll(state: MuseumState): MuseumState = change(state) { CatalogueFilter() }

    /** The `filter …` marker for [filter]: section key or `all`, saved 0/1 and the query's length (never its text). */
    fun marker(filter: CatalogueFilter): String =
        "filter section=${filter.section?.key ?: "all"} saved=${if (filter.savedOnly) 1 else 0} query=${filter.query.length}"

    private inline fun change(state: MuseumState, f: (CatalogueFilter) -> CatalogueFilter): MuseumState {
        val next = f(state.filter)
        return if (next == state.filter) state else state.copy(filter = next)
    }
}
