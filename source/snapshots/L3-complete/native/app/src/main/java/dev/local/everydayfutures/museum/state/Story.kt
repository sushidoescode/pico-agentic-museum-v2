package dev.local.everydayfutures.museum.state

import dev.local.everydayfutures.museum.catalogue.Catalogue

/**
 * Where the visitor was when the story was revealed. The story never changes the study, so Return leaves the visitor
 * exactly here unless they changed the study themselves during the story; it is kept for the return marker.
 */
data class EntryContext(val view: View, val turnSteps: Int)

/** An open miniature story: whose it is, which chapter (0-based) and the entry context. Never persisted. */
data class StoryState(val objectId: String, val chapter: Int, val entry: EntryContext) {
    /** 1-based, for labels and markers. */
    val chapterNumber: Int get() = chapter + 1
}

/**
 * Total reducers for the story. One path serves all three objects: the chapter count comes from the catalogue, and
 * no reducer reads the comfort options, so reduced motion cannot change the logical sequence.
 */
object StoryReducer {
    fun chapterCount(objectId: String): Int = Catalogue.byId(objectId)?.chapters?.size ?: 0

    /** Starts the selected object's story at chapter 1; revealing the story that is already open keeps its chapter. */
    fun reveal(state: MuseumState): MuseumState =
        if (state.story?.objectId == state.selectedId || chapterCount(state.selectedId) == 0) state
        else state.copy(story = StoryState(state.selectedId, 0, EntryContext(state.view, state.turnSteps)))

    fun next(state: MuseumState): MuseumState = moveBy(state, +1)

    fun back(state: MuseumState): MuseumState = moveBy(state, -1)

    /** Story reset: chapter 1. Selection, saved objects, preferences, view and orientation are untouched. */
    fun restart(state: MuseumState): MuseumState =
        state.story?.let { state.copy(story = it.copy(chapter = 0)) } ?: state

    /**
     * Leaves the story and nothing else. The story never touches selection, view or orientation, so the inspection
     * context is exactly as the visitor left it: the entry context, or whatever they chose in the study meanwhile.
     * (Revised after the L3 review: restoring the entry pose here could only ever undo the visitor's own later actions.)
     */
    fun returnFromStory(state: MuseumState): MuseumState =
        if (state.story == null) state else state.copy(story = null)

    /** After a selection: an open story switches to the newly selected object at chapter 1. */
    fun followSelection(state: MuseumState): MuseumState {
        val story = state.story ?: return state
        return if (story.objectId == state.selectedId) state
        else state.copy(story = StoryState(state.selectedId, 0, EntryContext(state.view, state.turnSteps)))
    }

    private fun moveBy(state: MuseumState, delta: Int): MuseumState {
        val story = state.story ?: return state
        val last = chapterCount(story.objectId) - 1
        return state.copy(story = story.copy(chapter = (story.chapter + delta).coerceIn(0, last.coerceAtLeast(0))))
    }
}
