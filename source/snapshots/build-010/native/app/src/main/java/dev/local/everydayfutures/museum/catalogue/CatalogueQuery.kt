package dev.local.everydayfutures.museum.catalogue

/**
 * What the catalogue grid shows: one section or all ([section] null), everything or only the saved collection, and a
 * search [query]. App state, never persisted (a new process starts unfiltered).
 */
data class CatalogueFilter(
    val section: Section? = null,
    val savedOnly: Boolean = false,
    val query: String = "",
) {
    /** True when the grid may be showing fewer than every object. */
    val isActive: Boolean get() = section != null || savedOnly || query.isNotBlank()
}

/** The grid's data-derived counts: [visible] of [total] objects shown, [saved] in the collection. */
data class CatalogueCount(val visible: Int, val total: Int, val saved: Int, val filtered: Boolean)

/** Why the grid is empty, so the empty state can say so and offer the one action that helps. */
enum class EmptyReason {
    /** The search matches nothing (the state names the query and offers Clear search). */
    NO_MATCH,

    /** Saved view with an empty collection (offers Show all). */
    NOTHING_SAVED,

    /** Saved objects exist, but none in this section (offers Show all). */
    NONE_IN_VIEW,
}

/** What a grid cell says in text beside its number, so selection and saving are never shown by colour alone. */
enum class CellMark { NONE, SELECTED, SAVED }

/** Pure catalogue queries: filtering, counts, empty states, the hidden-selection note and the cell marks. */
object CatalogueQuery {
    /** Longest query kept; a search field is a few words, and markers log only the length. */
    const val MAX_QUERY_LENGTH = 64

    /**
     * The objects [filter] shows, in accession order. The query matches the name, the number, the short name and the
     * section label, case-insensitively, after trimming; a blank query matches everything.
     */
    fun visible(artifacts: List<Artifact>, filter: CatalogueFilter, savedIds: List<String>): List<Artifact> {
        val needle = filter.query.trim().lowercase()
        return artifacts.filter { a ->
            (filter.section == null || a.section == filter.section) &&
                (!filter.savedOnly || a.id in savedIds) &&
                (needle.isEmpty() || matches(a, needle))
        }
    }

    private fun matches(a: Artifact, needle: String): Boolean =
        listOf(a.name, a.number, a.shortName, a.section.label).any { it.lowercase().contains(needle) }

    fun count(artifacts: List<Artifact>, filter: CatalogueFilter, savedIds: List<String>): CatalogueCount =
        CatalogueCount(
            visible = visible(artifacts, filter, savedIds).size,
            total = artifacts.size,
            saved = savedIds.count { id -> artifacts.any { it.id == id } },
            filtered = filter.isActive,
        )

    /**
     * Null when the grid shows something; otherwise why it is empty. The search is named when it is what empties the
     * grid (without it something would show); otherwise an empty Saved view says whether anything is saved at all.
     */
    fun emptyReason(artifacts: List<Artifact>, filter: CatalogueFilter, savedIds: List<String>): EmptyReason? {
        if (visible(artifacts, filter, savedIds).isNotEmpty()) return null
        val searchEmptiesIt = filter.query.isNotBlank() && visible(artifacts, filter.copy(query = ""), savedIds).isNotEmpty()
        return when {
            searchEmptiesIt -> EmptyReason.NO_MATCH
            filter.savedOnly && savedIds.none { id -> artifacts.any { it.id == id } } -> EmptyReason.NOTHING_SAVED
            filter.savedOnly -> EmptyReason.NONE_IN_VIEW
            else -> EmptyReason.NO_MATCH
        }
    }

    /** True when the selected object exists but the current filter hides it (it stays selected; the grid says so). */
    fun selectionHidden(artifacts: List<Artifact>, filter: CatalogueFilter, savedIds: List<String>, selectedId: String): Boolean =
        artifacts.any { it.id == selectedId } && visible(artifacts, filter, savedIds).none { it.id == selectedId }

    /** The selection first (a selected cell that is also saved says Selected), then whether the object is saved. */
    fun cellMark(id: String, selectedId: String, savedIds: List<String>): CellMark = when {
        id == selectedId -> CellMark.SELECTED
        id in savedIds -> CellMark.SAVED
        else -> CellMark.NONE
    }

    /**
     * Enter in the one-line search field arrives as a line break: it means "done typing". On the emulator the system
     * input method keeps every touch while it is open (build-003: taps on Clear search were routed to
     * com.bytedance.pico.inputmethod), so Enter must also put the keyboard away.
     */
    fun isSubmit(input: String): Boolean = input.contains('\n') || input.contains('\r')

    /** The typed text without the line break that submitted it. */
    fun withoutLineBreaks(input: String): String = input.replace("\r", "").replace("\n", "")

    /** What the search field stores: one line (newlines become spaces), at most [MAX_QUERY_LENGTH] characters. */
    fun sanitize(input: String): String = input.replace('\n', ' ').replace('\r', ' ').take(MAX_QUERY_LENGTH)
}

/** Locale-free size text for the details and the inspector (the UI adds "tall"/"wide" from resources). */
object SizeText {
    /** 0.03 → "3 cm", 0.012 → "1.2 cm", 0.55 → "55 cm", 1.15 → "1.15 m". Non-finite or non-positive → "—". */
    fun length(metres: Float): String {
        if (!metres.isFinite() || metres <= 0f) return "—"
        return if (metres >= 1f) {
            "${trim(round(metres, 100))} m"
        } else {
            val cm = metres * 100f
            if (cm < 10f) "${trim(round(cm, 10))} cm" else "${Math.round(cm)} cm"
        }
    }

    /** Which way an object's largest real dimension runs, for "15 cm long" rather than "1.8 cm tall" on a flat remote. */
    enum class Extent { TALL, WIDE, LONG }

    /** The largest of height, width and depth (ties prefer height, then width), with its direction. */
    fun largest(size: Size3): Pair<Extent, Float> = listOf(
        Extent.TALL to size.height,
        Extent.WIDE to size.width,
        Extent.LONG to size.depth,
    ).maxByOrNull { it.second } ?: (Extent.TALL to size.height)

    private fun round(value: Float, scale: Int): Double = Math.round(value.toDouble() * scale).toDouble() / scale

    /** 3.0 → "3", 1.20 → "1.2", 1.15 → "1.15". */
    private fun trim(value: Double): String {
        val text = value.toString()
        return if (text.contains('.')) text.trimEnd('0').trimEnd('.') else text
    }
}
