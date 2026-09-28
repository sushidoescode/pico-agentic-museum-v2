package dev.local.everydayfutures.museum.state

/** The app's two window roots. The primary is the launcher window; the companion is optional. */
enum class Surface { PRIMARY, COMPANION }

/**
 * The app's record of its window roots. The platform owns the windows; this counts which roots currently have a live
 * composition, so a duplicate companion shows up as a count of two instead of passing unnoticed. Never persisted:
 * a new process starts with no window attached.
 */
data class Workspace(
    val primaryInstances: Int = 0,
    val companionInstances: Int = 0,
) {
    val companionOpen: Boolean get() = companionInstances > 0

    fun instances(surface: Surface): Int = when (surface) {
        Surface.PRIMARY -> primaryInstances
        Surface.COMPANION -> companionInstances
    }
}

/** One call the controller hands to the platform navigator. */
data class WindowRequest(val id: String, val tag: String, val kind: Kind) {
    enum class Kind { OPEN, FOCUS, CLOSE }
}

/** Total reducers and fixed identities for the window roots. */
object WorkspaceReducer {
    /** The launcher window's container id (manifest `pico.spatial.windowcontainer.id`). */
    const val MAIN_ID = "EverydayFuturesMain"

    /** The companion's container id, registered in the DSL only. */
    const val COMPANION_ID = "EverydayFuturesCompanion"

    /**
     * Stable and non-null on every request: SDK 6.1.9 reuses an open container with the same id and tag, while a
     * null tag always creates a new instance (`SpatialNavigator.openWindowContainer`).
     */
    const val COMPANION_TAG = "companion"

    fun attached(state: MuseumState, surface: Surface): MuseumState =
        state.copy(workspace = state.workspace.adjust(surface, +1))

    /** Clamped at zero, so an unmatched dispose can never produce a negative count. */
    fun detached(state: MuseumState, surface: Surface): MuseumState =
        state.copy(workspace = state.workspace.adjust(surface, -1))

    /** Opening while the companion is attached asks for the same instance again (focus), never for a second one. */
    fun openRequest(state: MuseumState): WindowRequest =
        WindowRequest(COMPANION_ID, COMPANION_TAG, if (state.workspace.companionOpen) WindowRequest.Kind.FOCUS else WindowRequest.Kind.OPEN)

    fun closeRequest(): WindowRequest = WindowRequest(COMPANION_ID, COMPANION_TAG, WindowRequest.Kind.CLOSE)

    private fun Workspace.adjust(surface: Surface, delta: Int): Workspace = when (surface) {
        Surface.PRIMARY -> copy(primaryInstances = (primaryInstances + delta).coerceAtLeast(0))
        Surface.COMPANION -> copy(companionInstances = (companionInstances + delta).coerceAtLeast(0))
    }
}
