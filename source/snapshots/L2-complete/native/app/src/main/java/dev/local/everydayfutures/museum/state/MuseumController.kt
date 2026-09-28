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
 * from, so emulator evidence can attribute a counted input and correlate the other window's agreement. Only the parts
 * that outlive a launch are persisted. Plain Kotlin; all calls happen on the main thread.
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
        log("$marker source=${source.label} ${summary(next)} action=#$actionCount")
    }

    companion object {
        /** Logcat tag; acceptance cases grep `Museum: select id=…`, `Museum: turn …`, `Museum: reset`, `Museum: companion-…`, `Museum: window-…`. */
        const val TAG = "Museum"

        /** Compact state summary shared by every marker: L1's fields first, then saved count and comfort flags. */
        fun summary(s: MuseumState): String =
            "steps=${s.turnSteps} selected=${s.selectedId} view=${s.view.name.lowercase()} saved=${s.savedIds.size} " +
                "opts=m${s.options.reduceMotion.bit}t${s.options.reduceTransparency.bit}c${s.options.increaseContrast.bit}"

        private val Boolean.bit: Int get() = if (this) 1 else 0
    }
}

/** Lower-case name used in markers. */
val Surface.label: String get() = name.lowercase()
