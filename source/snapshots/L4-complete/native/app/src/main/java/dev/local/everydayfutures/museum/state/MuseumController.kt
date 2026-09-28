package dev.local.everydayfutures.museum.state

import dev.local.everydayfutures.museum.catalogue.Section
import kotlin.coroutines.cancellation.CancellationException

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
 * (selection, view, orientation, filter, windows, story, inspector, archive) lives as long as the process; only saved
 * objects and comfort preferences are persisted beyond it. Plain Kotlin; all calls happen on the main thread.
 */
class MuseumController(
    private val persistence: MuseumPersistence,
    cellFactory: (MuseumState) -> StateCell<MuseumState> = { PlainCell(it) },
    private val log: (String) -> Unit = {},
    /** Monotonic milliseconds, for the in-flight bound of window requests; host tests pass their own. */
    private val clock: () -> Long = { System.nanoTime() / 1_000_000L },
) {
    private val cell: StateCell<MuseumState> = cellFactory(persistence.load())
    private var actionCount = 0
    private var companionRequests = 0
    private var inspectorRequests = 0

    /** The navigator and source of the request that left an auxiliary window pending (used when the detach arrives). */
    private var pendingNavigator: WindowNavigator? = null
    private var pendingSource: Surface = Surface.PRIMARY

    /** An auxiliary switch whose open has been issued: (closed window, window being opened), for the `done` marker. */
    private var switchInFlight: Pair<Surface, Surface>? = null

    val state: MuseumState get() = cell.value

    /**
     * Selects an object. When an open story's new object has no story, the story ends and a `story-return …
     * reason=selection-without-story` marker follows the selection's own marker.
     */
    fun select(id: String, source: Surface) {
        val leaving = state.story
        commit("select id=$id", source) { MuseumReducer.select(it, id) }
        if (leaving != null && state.story == null) {
            log("story-return${storyDetail(leaving)} reason=selection-without-story source=${source.label} ${summary(state)}")
        }
    }

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
     * catalogue filter, the windows and the comfort preferences all stay as they are.
     */
    fun clearCollection(source: Surface) =
        commit("collection-clear cleared=${state.savedIds.size}", source, persist = true) { MuseumReducer.clearSaved(it) }

    /**
     * Reduce motion. Switching it on stops the inspector's spin at once (`inspector-spin off reason=reduce-motion`); the
     * angle the model stopped at is known only to the inspector window, which folds it in right after with
     * [inspectorSettle] (`inspector-settle yaw=<deg> reason=reduce-motion`), so this marker carries no yaw.
     */
    fun setReduceMotion(on: Boolean, source: Surface) {
        val spinning = state.inspector?.spinning == true
        setOptions(state.options.copy(reduceMotion = on), "reduceMotion=$on", source)
        if (spinning && state.inspector?.spinning == false) {
            log("inspector-spin off reason=reduce-motion source=${source.label} ${summary(state)}")
        }
    }

    fun setReduceTransparency(on: Boolean, source: Surface) =
        setOptions(state.options.copy(reduceTransparency = on), "reduceTransparency=$on", source)

    fun setIncreaseContrast(on: Boolean, source: Surface) =
        setOptions(state.options.copy(increaseContrast = on), "increaseContrast=$on", source)

    // ---- Catalogue filter (app state, not persisted; the marker logs the query's length, never its text) ----

    fun setSection(section: Section?, source: Surface) = filterAction(source) { FilterReducer.section(it, section) }

    fun setSavedOnly(savedOnly: Boolean, source: Surface) = filterAction(source) { FilterReducer.savedOnly(it, savedOnly) }

    fun setQuery(query: String, source: Surface) = filterAction(source) { FilterReducer.query(it, query) }

    fun clearQuery(source: Surface) = filterAction(source) { FilterReducer.clearQuery(it) }

    /** Show / Show all: the whole collection again; the selection is untouched. */
    fun showAll(source: Surface) = filterAction(source) { FilterReducer.showAll(it) }

    private fun filterAction(source: Surface, reduce: (MuseumState) -> MuseumState) {
        val next = reduce(state)
        if (next == state) return
        commit(FilterReducer.marker(next.filter), source) { next }
    }

    // ---- Auxiliary windows: the companion and the inspector, at most one at a time ----

    /**
     * Opens the companion, or asks for the already-open instance again. The request always carries the fixed id and
     * tag, so the platform reuses the open window instead of creating a second one; no bundle is sent, because the
     * companion renders from this owner rather than from launch data. While the inspector is attached it is closed
     * first and the companion opens only once its detach is observed.
     */
    fun openCompanion(navigator: WindowNavigator, source: Surface) = showAux(Surface.COMPANION, navigator, source)

    /**
     * Opens the selected object's miniature story in the companion (L3): starts it at chapter 1, or keeps the chapter if
     * that story is already open, then opens the companion or asks for the same instance again. Only the three objects
     * with a story offer this; for any other object it is ignored (logged, nothing opens), so it is never a dead end.
     */
    fun revealStory(navigator: WindowNavigator, source: Surface) {
        if (StoryReducer.chapterCount(state.selectedId) == 0) {
            log("story-reveal id=${state.selectedId} ignored=no-story source=${source.label} ${summary(state)}")
            return
        }
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
        commit("story-return${leaving?.let { storyDetail(it) }.orEmpty()}", source) { StoryReducer.returnFromStory(it) }
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
    fun closeCompanion(navigator: WindowNavigator, source: Surface) = issueClose(Surface.COMPANION, navigator, source)

    /**
     * Open in 3D: the inspector opens at the defined initial view (the selected object at the study's current pose, not
     * spinning), or — while it is attached — is asked for again with the same id and tag (focus), keeping its
     * presentation. While the companion is attached it is closed first and the inspector opens only once its detach is
     * observed; a repeated press meanwhile re-issues that close and never opens a third window.
     */
    fun openInspector(navigator: WindowNavigator, source: Surface) {
        val next = InspectorReducer.open(state)
        if (next != state) cell.value = next
        showAux(Surface.INSPECTOR, navigator, source)
    }

    /**
     * Closes the inspector; [liveYaw] (the pose on screen) is committed first, and a running spin is logged as stopped
     * there. Study, story and saved data are untouched.
     */
    fun closeInspector(navigator: WindowNavigator, liveYaw: Float?, source: Surface) {
        if (liveYaw != null) {
            val spinning = state.inspector?.spinning == true
            val next = InspectorReducer.commitYaw(state, liveYaw)
            if (next != state) cell.value = next
            if (spinning) logSpinStopped(liveYaw, "close", source)
        }
        issueClose(Surface.INSPECTOR, navigator, source)
    }

    /** Inspector Turn left/right: 30° from the live yaw; stops spin. Marker `inspector-turn direction=… yaw=<deg>`. */
    fun inspectorTurn(direction: Turn, liveYaw: Float, source: Surface) {
        val spinning = state.inspector?.spinning == true
        val next = InspectorReducer.turn(state, direction, liveYaw)
        val inspector = next.inspector ?: return
        if (spinning) logSpinStopped(liveYaw, "turn", source)
        commit("inspector-turn direction=${direction.name.lowercase()} yaw=${deg(inspector.yawDegrees)}", source) { next }
    }

    /** A running spin stopped by another action: where it stopped (the live yaw that action committed) and why. */
    private fun logSpinStopped(liveYaw: Float, reason: String, source: Surface) {
        log("inspector-spin off yaw=${deg(liveYaw)} reason=$reason source=${source.label} ${summary(state)}")
    }

    /** Spin / Pause. Pause commits the live yaw; spin stays off while Reduce motion is on. Marker `inspector-spin on|off yaw=<deg>`. */
    fun inspectorToggleSpin(liveYaw: Float, source: Surface) {
        val before = state.inspector ?: return
        val next = InspectorReducer.toggleSpin(state, liveYaw)
        val inspector = next.inspector ?: return
        val refused = !before.spinning && !inspector.spinning
        val detail = if (refused) " reason=reduce-motion" else ""
        commit("inspector-spin ${if (inspector.spinning) "on" else "off"} yaw=${deg(inspector.yawDegrees)}$detail", source) { next }
    }

    /** A drag ended: its yaw is committed and spin stays off. Marker `inspector-drag yaw=<deg>`. */
    fun inspectorDragged(yaw: Float, source: Surface) {
        val spinning = state.inspector?.spinning == true
        val next = InspectorReducer.dragged(state, yaw)
        val inspector = next.inspector ?: return
        if (spinning) logSpinStopped(yaw, "drag", source)
        commit("inspector-drag yaw=${deg(inspector.yawDegrees)}", source) { next }
    }

    /**
     * The inspector window folds its live pose into state after spin stopped for a reason it did not initiate
     * ([reason], e.g. `reduce-motion`), so the committed yaw equals the angle on screen. Not a visitor action: no action
     * number; nothing but the inspector's yaw changes; ignored when there is no inspector or nothing to fold.
     */
    fun inspectorSettle(liveYaw: Float, reason: String, source: Surface) {
        val next = InspectorReducer.settle(state, liveYaw)
        if (next == state) return
        cell.value = next
        log("inspector-settle yaw=${deg(next.inspector?.yawDegrees)} reason=$reason source=${source.label} ${summary(state)}")
    }

    /** Reset view: the front view, spin off ([liveYaw], if given, is where a running spin stopped). */
    fun inspectorResetView(source: Surface, liveYaw: Float? = null) {
        val inspector = state.inspector ?: return
        if (inspector.spinning) logSpinStopped(liveYaw ?: inspector.yawDegrees, "reset", source)
        commit("inspector-reset yaw=0", source) { InspectorReducer.resetView(it) }
    }

    /**
     * The study's model was hidden (the inspector shows the same object) or shown again: `study-model hidden=<0|1>
     * reason=inspector id=<id>`. Logged by the study when its model's visibility actually changes.
     */
    fun studyModelHidden(hidden: Boolean, id: String) {
        log("study-model hidden=${if (hidden) 1 else 0} reason=inspector id=$id ${summary(state)}")
    }

    /** A surface (`study` or `inspector`) starts loading an object's model (attempt 1, or a Try again). */
    fun modelLoadStarted(id: String, surface: String, attempt: Int) {
        log("exhibit-load id=$id surface=$surface attempt=$attempt")
    }

    /**
     * A surface's load for [id] finished: which model it shows (`generated`, `procedural` or `none`), how long it took
     * and why. The study also logs `exhibits-ready` (now one lazily loaded object: `count` is how many roots its scene
     * holds, 0 or 1).
     */
    fun modelShown(id: String, surface: String, model: String, millis: Long, reason: String, lightReady: Boolean? = null) {
        log("exhibit-model id=$id surface=$surface model=$model ms=$millis reason=$reason")
        if (lightReady != null) {
            log("exhibits-ready count=${if (model == "none") 0 else 1} light=$lightReady model=$model id=$id surface=$surface")
        }
    }

    private fun showAux(target: Surface, navigator: WindowNavigator, source: Surface) {
        val now = clock()
        when (val plan = WorkspaceReducer.planAux(state, target, now)) {
            is AuxPlan.Show -> {
                cell.value = WorkspaceReducer.applyPlan(state, plan, now)
                pendingNavigator = null
                issueOpen(target, plan.request, navigator, source)
            }
            is AuxPlan.CloseFirst -> {
                cell.value = WorkspaceReducer.applyPlan(state, plan, now)
                pendingNavigator = navigator
                pendingSource = source
                val step = if (plan.repeated) "pending" else "close"
                log("aux-switch from=${plan.other.label} to=${target.label} step=$step source=${source.label} ${summary(state)}")
                issueClose(plan.other, navigator, source)
            }
            is AuxPlan.AfterOpening -> {
                cell.value = WorkspaceReducer.applyPlan(state, plan, now)
                pendingNavigator = navigator
                pendingSource = source
                log("aux-switch from=${plan.other.label} to=${target.label} step=after-open source=${source.label} ${summary(state)}")
            }
            is AuxPlan.ReopenAfterClose -> {
                cell.value = WorkspaceReducer.applyPlan(state, plan, now)
                pendingNavigator = navigator
                pendingSource = source
                log("aux-switch from=${target.label} to=${target.label} step=reopen source=${source.label} ${summary(state)}")
            }
            is AuxPlan.AlreadyOpening -> {
                cell.value = WorkspaceReducer.applyPlan(state, plan, now)
                pendingNavigator = null
                log("${target.label}-open ignored=opening source=${source.label} ${summary(state)}")
            }
        }
    }

    private fun issueOpen(target: Surface, request: WindowRequest, navigator: WindowNavigator, source: Surface) {
        if (request.kind == WindowRequest.Kind.OPEN) cell.value = WorkspaceReducer.opening(state, target, clock())
        val count = bumpRequests(target)
        log(
            "${target.label}-open request=#$count mode=${request.kind.name.lowercase()} tag=${request.tag} " +
                "instances=${state.workspace.instances(target)} source=${source.label}",
        )
        navigator.open(request.id, request.tag)
    }

    private fun issueClose(target: Surface, navigator: WindowNavigator, source: Surface) {
        cell.value = WorkspaceReducer.closing(state, target, clock())
        val request = WorkspaceReducer.closeRequest(target)
        val count = bumpRequests(target)
        val yaw = if (target == Surface.INSPECTOR) " yaw=${deg(state.inspector?.yawDegrees)}" else ""
        log("${target.label}-close request=#$count tag=${request.tag} instances=${state.workspace.instances(target)}$yaw source=${source.label}")
        navigator.close(request.id, request.tag)
    }

    private fun bumpRequests(target: Surface): Int = when (target) {
        Surface.COMPANION -> ++companionRequests
        else -> ++inspectorRequests
    }

    // ---- The immersive archive (a Full Space Stage; root presents it) ----

    /**
     * Enter archive: CLOSED → OPENING (marker `archive-enter request`). An attached auxiliary window is closed first (its
     * logical state is kept) and a pending switch is dropped. Returns false, changing nothing, when the archive is not
     * closed; the caller then opens the Stage and reports [archiveOpenResult].
     */
    fun enterArchive(navigator: WindowNavigator, source: Surface): Boolean {
        val phase = state.archive.phase
        if (phase != ArchivePhase.CLOSED) {
            log("archive-enter request ignored=${phase.name.lowercase()} source=${source.label} ${summary(state)}")
            return false
        }
        commit("archive-enter request", source) { WorkspaceReducer.withPending(ArchiveReducer.enterRequested(it), null) }
        pendingNavigator = null
        state.workspace.attachedAux.forEach { issueClose(it, navigator, source) }
        // A window still opening is closed as soon as it attaches, so it cannot appear over the Stage.
        state.workspace.opening(clock())?.let { opening ->
            cell.value = WorkspaceReducer.withCloseOnAttach(state, opening)
            pendingNavigator = navigator
            pendingSource = source
        }
        return true
    }

    /** The Stage open finished (marker `archive-open result=<allowed|not-allowed|error> reason=…`). */
    fun archiveOpenResult(result: StageOpenResult, source: Surface = Surface.PRIMARY) =
        commit("archive-open result=${result.resultWord} reason=${result.reasonWord}", source) {
            ArchiveReducer.openResult(it, result == StageOpenResult.Allowed, result.reasonWord)
        }

    /** The second half of Enter archive: opens the Stage through [stage] and records the result. Cancellation is recorded as an error, then rethrown. */
    suspend fun completeArchiveOpen(stage: StageNavigator, source: Surface) {
        val result = try {
            stage.open(WorkspaceReducer.ARCHIVE_ID)
        } catch (cancelled: CancellationException) {
            archiveOpenResult(StageOpenResult.Error(CODE_CANCELLED, "cancelled"), source)
            throw cancelled
        } catch (error: Throwable) {
            StageOpenResult.Error(CODE_EXCEPTION, error.javaClass.simpleName)
        }
        archiveOpenResult(result, source)
    }

    fun archiveGoTo(viewpoint: ArchiveViewpoint, source: Surface) =
        commit("archive-goto viewpoint=${viewpoint.key}", source) { ArchiveReducer.goTo(it, viewpoint) }

    fun archiveFocus(id: String, source: Surface) = commit("archive-focus id=$id", source) { ArchiveReducer.focus(it, id) }

    fun archiveReturn(source: Surface) = commit("archive-return", source) { ArchiveReducer.returnRequested(it) }

    /** The Stage is gone (after Return, or closed by the system). Idempotent. */
    fun archiveClosed(source: Surface) {
        if (state.archive.phase == ArchivePhase.CLOSED) return
        commit("archive-closed", source) { ArchiveReducer.closed(it) }
    }

    /** Return to catalogue: CLOSING, then `closeStage`, then CLOSED. The Stage root's detach also closes the record. */
    suspend fun leaveArchive(stage: StageNavigator, source: Surface) {
        archiveReturn(source)
        try {
            stage.close()
        } finally {
            archiveClosed(source)
        }
    }

    // ---- Window roots ----

    /** A window root's composition started. Called once per composition; a second live instance is counted, not hidden. */
    fun windowAttached(surface: Surface) {
        var next = WorkspaceReducer.attached(state, surface)
        if (surface == Surface.INSPECTOR) next = InspectorReducer.ensure(next)
        cell.value = next
        log("window-attached surface=${surface.label} instances=${state.workspace.instances(surface)} ${summary(state)}")
        val switching = switchInFlight
        if (switching != null && switching.second == surface) {
            switchInFlight = null
            log("aux-switch from=${switching.first.label} to=${surface.label} step=done ${summary(state)}")
        }
        if (state.workspace.closeOnAttach == surface) {
            cell.value = WorkspaceReducer.withCloseOnAttach(state, null)
            val navigator = pendingNavigator
            log("aux-switch from=${surface.label} to=${state.workspace.pendingAux?.label ?: "none"} step=close-after-open ${summary(state)}")
            if (navigator != null) issueClose(surface, navigator, pendingSource)
        }
    }

    /**
     * A window root's composition was disposed (closed by the app or by the system). Shared logical state survives; the
     * inspector's presentation is cleared when its last instance goes; the archive record closes when its root goes;
     * and a pending auxiliary window opens once no auxiliary window is attached.
     */
    fun windowDetached(surface: Surface) {
        var next = WorkspaceReducer.detached(state, surface)
        if (surface == Surface.INSPECTOR && !next.workspace.inspectorOpen) next = InspectorReducer.detached(next)
        cell.value = next
        log("window-detached surface=${surface.label} instances=${state.workspace.instances(surface)} ${summary(state)}")
        if (surface == Surface.ARCHIVE && !state.workspace.archiveOpen) archiveClosed(Surface.ARCHIVE)
        openPendingAfter(surface)
    }

    private fun openPendingAfter(detached: Surface) {
        val request = WorkspaceReducer.pendingOpen(state, clock()) ?: return
        val target = state.workspace.pendingAux ?: return
        val navigator = pendingNavigator ?: return
        cell.value = WorkspaceReducer.withPending(state, null)
        pendingNavigator = null
        switchInFlight = detached to target
        log("aux-switch from=${detached.label} to=${target.label} step=open source=${pendingSource.label} ${summary(state)}")
        issueOpen(target, request, navigator, pendingSource)
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

    private fun storyDetail(story: StoryState): String =
        " from=${story.chapterNumber} entry=${story.entry.view.name.lowercase()}:${story.entry.turnSteps}"

    companion object {
        /**
         * Logcat tag; acceptance cases grep `select id=…`, `turn …`, `reset`, `collection-…`, `filter …`, `companion-…`,
         * `inspector-…`, `aux-switch …`, `archive-…`, `exhibit-…`, `window-…` and `story-…` markers.
         */
        const val TAG = "Museum"

        /** [StageOpenResult.Error] codes the app assigns (the SDK's own codes are -2…2). */
        const val CODE_CANCELLED = -2
        const val CODE_EXCEPTION = -100

        /**
         * Compact state summary shared by every marker: L1's fields first, then saved count, comfort flags and story (the
         * accepted prefix), then the catalogue filter, the inspector presentation and the archive phase.
         */
        fun summary(s: MuseumState): String =
            "steps=${s.turnSteps} selected=${s.selectedId} view=${s.view.name.lowercase()} saved=${s.savedIds.size} " +
                "opts=m${s.options.reduceMotion.bit}t${s.options.reduceTransparency.bit}c${s.options.increaseContrast.bit} " +
                "story=${s.story?.let { "${it.objectId}:${it.chapterNumber}" } ?: "none"} " +
                "filter=${s.filter.section?.key ?: "all"}:${s.filter.savedOnly.bit}:${s.filter.query.length} " +
                "inspector=${s.inspector?.let { "${it.objectId}:${deg(it.yawDegrees)}:${if (it.spinning) "spin" else "still"}" } ?: "none"} " +
                "archive=${s.archive.phase.name.lowercase()}"

        /** Degrees for markers: whole degrees, 0..359 (null → 0). */
        fun deg(yaw: Float?): Int = Math.floorMod(Math.round(yaw ?: 0f), 360)

        private val Boolean.bit: Int get() = if (this) 1 else 0
    }
}

/** Lower-case name used in markers. */
val Surface.label: String get() = name.lowercase()
