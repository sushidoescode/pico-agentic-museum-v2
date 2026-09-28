package dev.local.everydayfutures.museum.state

/** Where the controller keeps the current state. The app backs it with Compose snapshot state so every window
 *  root that reads it recomposes; host tests use [PlainCell]. */
interface StateCell<T> {
    var value: T
}

/** A plain field, for host tests. */
class PlainCell<T>(initial: T) : StateCell<T> {
    override var value: T = initial
}

/** Persistence port for the parts of [MuseumState] that outlive a launch (saved objects and comfort preferences). */
interface MuseumPersistence {
    fun load(): MuseumState

    fun save(state: MuseumState)
}

/** The platform window navigator, reduced to the two calls the app makes. */
interface WindowNavigator {
    fun open(id: String, tag: String)

    fun close(id: String, tag: String)
}

/**
 * The one app-owned owner of [MuseumState]. The application creates a single instance per process and hands the
 * same instance to every window root, so selection, orientation, saved objects and preferences cannot disagree
 * between windows. Every action goes through one reducer and is logged with a stable marker naming the window it came
 * from, so emulator evidence can attribute a counted input and correlate the other window's agreement. Logical state
 * (selection, view, orientation, windows, story) lives as long as the process; only saved objects and comfort
 * preferences are persisted beyond it. Plain Kotlin; all calls happen on the main thread.
 */
class MuseumController(
    private val persistence: MuseumPersistence,
    cellFactory: (MuseumState) -> StateCell<MuseumState> = { PlainCell(it) },
    private val log: (String) -> Unit = {},
) {
    private val cell: StateCell<MuseumState> = cellFactory(persistence.load())
    private var actionCount = 0
    private var companionRequests = 0

    val state: MuseumState get() = cell.value

    fun select(id: String, source: Surface) = commit("select id=$id", source) { MuseumReducer.select(it, id) }

    fun turn(direction: Turn, source: Surface) =
        commit("turn direction=${direction.name.lowercase()}", source) { MuseumReducer.turn(it, direction) }

    fun toggleInspect(source: Surface) = commit("inspect-toggle", source) { MuseumReducer.toggleInspect(it) }

    fun reset(source: Surface) = commit("reset", source) { MuseumReducer.reset(it) }

    fun keep(source: Surface) = commit("keep id=${state.selectedId}", source, persist = true) { MuseumReducer.keep(it) }

    /** The confirmation for clearing the saved collection is shown (also when a recreated window shows it again); nothing changes until they confirm. */
    fun collectionClearAsked(source: Surface) = log("collection-clear-ask source=${source.label} ${summary(state)}")

    /** The visitor kept their collection at the confirmation; nothing changed. */
    fun collectionClearCancelled(source: Surface) = log("collection-clear-cancel source=${source.label} ${summary(state)}")

    /**
     * Confirmed collection reset (design-doc "Reset saved collection" → "Start fresh"): clears the saved objects and
     * persists that, and nothing else — selection, the study's orientation and view, an open story and its chapter, the
     * windows and the comfort preferences all stay as they are.
     */
    fun clearCollection(source: Surface) =
        commit("collection-clear cleared=${state.savedIds.size}", source, persist = true) { MuseumReducer.clearSaved(it) }

    fun setReduceMotion(on: Boolean, source: Surface) = setOptions(state.options.copy(reduceMotion = on), "reduceMotion=$on", source)

    fun setReduceTransparency(on: Boolean, source: Surface) =
        setOptions(state.options.copy(reduceTransparency = on), "reduceTransparency=$on", source)

    fun setIncreaseContrast(on: Boolean, source: Surface) =
        setOptions(state.options.copy(increaseContrast = on), "increaseContrast=$on", source)

    /**
     * Opens the companion, or asks for the already-open instance again. The request always carries the fixed id and
     * tag, so the platform reuses the open window instead of creating a second one; no bundle is sent, because the
     * companion renders from this owner rather than from launch data.
     */
    fun openCompanion(navigator: WindowNavigator, source: Surface) {
        val request = WorkspaceReducer.openRequest(state)
        companionRequests += 1
        log(
            "companion-open request=#$companionRequests mode=${request.kind.name.lowercase()} tag=${request.tag} " +
                "instances=${state.workspace.companionInstances} source=${source.label}",
        )
        navigator.open(request.id, request.tag)
    }

    /**
     * Opens the selected object's miniature story in the companion (L3): starts it at chapter 1, or keeps the chapter if
     * that story is already open, then opens the companion or asks for the same instance again.
     */
    fun revealStory(navigator: WindowNavigator, source: Surface) {
        commit("story-reveal id=${state.selectedId}", source) { StoryReducer.reveal(it) }
        openCompanion(navigator, source)
    }

    fun nextChapter(source: Surface) = commit("story-next", source) { StoryReducer.next(it) }

    fun previousChapter(source: Surface) = commit("story-back", source) { StoryReducer.back(it) }

    /** Story reset: chapter 1; nothing else changes. */
    fun restartStory(source: Surface) = commit("story-restart", source) { StoryReducer.restart(it) }

    /** Leaves the story; the marker records the chapter left and the entry context, so a changed study is visible. */
    fun returnFromStory(source: Surface) {
        val leaving = state.story
        val detail = leaving?.let { " from=${it.chapterNumber} entry=${it.entry.view.name.lowercase()}:${it.entry.turnSteps}" }.orEmpty()
        commit("story-return$detail", source) { StoryReducer.returnFromStory(it) }
    }

    /** The companion's miniature has arrived at a chapter: how it got there (`animated`, `still` or `arrive`) and how long it took. */
    fun storySceneSettled(objectId: String, chapter: Int, motion: String, millis: Long) {
        log("story-scene id=$objectId chapter=${chapter + 1} motion=$motion ms=$millis reduceMotion=${state.options.reduceMotion}")
    }

    /** The companion started creating its story scene (attempt n): the miniature region shows its preparing state. */
    fun storyScenePreparing(objectId: String, chapter: Int, attempt: Int) {
        log("story-scene-preparing id=$objectId chapter=${chapter + 1} attempt=$attempt")
    }

    /** The story's own miniature now exists in the companion's scene; [millis] since the preparing state was first composed. */
    fun storySceneReady(objectId: String, chapter: Int, attempt: Int, millis: Long) {
        log("story-scene-ready id=$objectId chapter=${chapter + 1} attempt=$attempt ms=$millis")
    }

    /** The preparing (or failed) card left the story region after [millis] in the composition. */
    fun storySceneCardRemoved(objectId: String, chapter: Int, attempt: Int, millis: Long) {
        log("story-scene-card-removed id=$objectId chapter=${chapter + 1} attempt=$attempt shownMs=$millis")
    }

    /** The story's miniature could not be created (or not completely); the companion offers Try again and Return instead. */
    fun storySceneFailed(objectId: String, chapter: Int, attempt: Int, reason: String) {
        log("story-scene-failed id=$objectId chapter=${chapter + 1} attempt=$attempt reason=$reason")
    }

    /** Closes the companion wherever the request comes from; the logical state is untouched. */
    fun closeCompanion(navigator: WindowNavigator, source: Surface) {
        val request = WorkspaceReducer.closeRequest()
        companionRequests += 1
        log("companion-close request=#$companionRequests tag=${request.tag} instances=${state.workspace.companionInstances} source=${source.label}")
        navigator.close(request.id, request.tag)
    }

    /** A window root's composition started. Called once per composition; a second live companion is counted, not hidden. */
    fun windowAttached(surface: Surface) {
        cell.value = WorkspaceReducer.attached(state, surface)
        log("window-attached surface=${surface.label} instances=${state.workspace.instances(surface)} ${summary(state)}")
    }

    /** A window root's composition was disposed (closed by the app or by the system). Shared logical state survives. */
    fun windowDetached(surface: Surface) {
        cell.value = WorkspaceReducer.detached(state, surface)
        log("window-detached surface=${surface.label} instances=${state.workspace.instances(surface)} ${summary(state)}")
    }

    /** A window root recomposed onto new logical state: the marker that correlates the other window's agreement. */
    fun windowShows(surface: Surface) {
        log("window-shows surface=${surface.label} ${summary(state)}")
    }

    private fun setOptions(options: ComfortOptions, marker: String, source: Surface) =
        commit("option $marker", source, persist = true) { MuseumReducer.setOptions(it, options) }

    private fun commit(marker: String, source: Surface, persist: Boolean = false, reduce: (MuseumState) -> MuseumState) {
        val next = reduce(state)
        actionCount += 1
        cell.value = next
        if (persist) persistence.save(next)
        log("${marker.withChapter(next)} source=${source.label} ${summary(next)} action=#$actionCount")
    }

    /** Story markers carry the chapter they arrived at (1-based), e.g. `story-next chapter=2`. */
    private fun String.withChapter(next: MuseumState): String =
        if (startsWith("story-") && next.story != null) "$this chapter=${next.story.chapterNumber}" else this

    companion object {
        /** Logcat tag; acceptance cases grep `select id=…`, `turn …`, `reset`, `collection-…`, `companion-…`, `window-…` and `story-…` markers. */
        const val TAG = "Museum"

        /** Compact state summary shared by every marker: L1's fields first, then saved count, comfort flags and story. */
        fun summary(s: MuseumState): String =
            "steps=${s.turnSteps} selected=${s.selectedId} view=${s.view.name.lowercase()} saved=${s.savedIds.size} " +
                "opts=m${s.options.reduceMotion.bit}t${s.options.reduceTransparency.bit}c${s.options.increaseContrast.bit} " +
                "story=${s.story?.let { "${it.objectId}:${it.chapterNumber}" } ?: "none"}"

        private val Boolean.bit: Int get() = if (this) 1 else 0
    }
}

/** Lower-case name used in markers. */
val Surface.label: String get() = name.lowercase()
