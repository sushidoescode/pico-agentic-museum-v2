package dev.local.everydayfutures.museum.state

/**
 * The app's composition roots. [PRIMARY] is the launcher window; [COMPANION] and [INSPECTOR] are the two optional
 * auxiliary windows, of which at most one is shown at a time; [ARCHIVE] is the immersive archive's Stage root, which
 * is not a window but is counted the same way so a duplicate or a leak is visible.
 */
enum class Surface { PRIMARY, COMPANION, INSPECTOR, ARCHIVE }

/**
 * The app's record of its roots. The platform owns the windows; this counts which roots currently have a live
 * composition, so a duplicate shows up as a count of two instead of passing unnoticed. [pendingAux] is the auxiliary
 * window the visitor asked for while another one occupied the slot: it opens only once that one's detach has been
 * observed. A window whose open or close has been requested but whose attach or detach has not arrived yet is in
 * flight ([openingAux], [closingAux], with the time of the request): the review of 2026-09-27 showed that counting only
 * attached roots let a quick second press (inside the ~120 ms before an attach, or the ~340 ms before a detach) request
 * a third window or lose both. [closeOnAttach] is an opening window that has already been displaced; it is closed as
 * soon as it attaches. Never persisted: a new process starts with no window attached and nothing pending.
 */
data class Workspace(
    val primaryInstances: Int = 0,
    val companionInstances: Int = 0,
    val inspectorInstances: Int = 0,
    val archiveInstances: Int = 0,
    val pendingAux: Surface? = null,
    val openingAux: Surface? = null,
    val openingSinceMillis: Long = 0L,
    val closingAux: Surface? = null,
    val closingSinceMillis: Long = 0L,
    val closeOnAttach: Surface? = null,
) {
    val companionOpen: Boolean get() = companionInstances > 0

    val inspectorOpen: Boolean get() = inspectorInstances > 0

    val archiveOpen: Boolean get() = archiveInstances > 0

    fun instances(surface: Surface): Int = when (surface) {
        Surface.PRIMARY -> primaryInstances
        Surface.COMPANION -> companionInstances
        Surface.INSPECTOR -> inspectorInstances
        Surface.ARCHIVE -> archiveInstances
    }

    /** The auxiliary windows that are attached now (normally none or one). */
    val attachedAux: List<Surface> get() = WorkspaceReducer.AUXILIARY.filter { instances(it) > 0 }

    /** The auxiliary window whose open is in flight at [now]: requested, not attached, and not older than the bound. */
    fun opening(now: Long): Surface? = openingAux?.takeIf { instances(it) == 0 && now - openingSinceMillis < WorkspaceReducer.IN_FLIGHT_MS }

    /** The auxiliary window whose close is in flight at [now]: requested, still attached, and not older than the bound. */
    fun closing(now: Long): Surface? = closingAux?.takeIf { instances(it) > 0 && now - closingSinceMillis < WorkspaceReducer.IN_FLIGHT_MS }
}

/** One call the controller hands to the platform navigator. */
data class WindowRequest(val id: String, val tag: String, val kind: Kind) {
    enum class Kind { OPEN, FOCUS, CLOSE }
}

/** What a request to show an auxiliary window leads to under the one-auxiliary-window policy. */
sealed class AuxPlan {
    /** Open the target (nothing else is attached) or focus it (it is attached already). Nothing stays pending. */
    data class Show(val request: WindowRequest) : AuxPlan()

    /**
     * Another auxiliary window ([other]) is attached: close it first and remember the target as pending. [repeated] is
     * true when the same target was already pending (the close is simply issued again; nothing else is opened).
     */
    data class CloseFirst(val other: Surface, val target: Surface, val close: WindowRequest, val repeated: Boolean) : AuxPlan()

    /** [other] is still opening: nothing is requested now; [other] is closed once it attaches, then the target opens. */
    data class AfterOpening(val other: Surface, val target: Surface) : AuxPlan()

    /** [target] is attached but its close is in flight: it opens again once its detach has been observed. */
    data class ReopenAfterClose(val target: Surface) : AuxPlan()

    /** [target]'s own open is in flight: nothing more is requested (a second open could create a second instance). */
    data class AlreadyOpening(val target: Surface) : AuxPlan()
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

    /** The Open in 3D inspector's container id (DSL only, volumetric, material background off). */
    const val INSPECTOR_ID = "EverydayFuturesInspector"

    /** Fixed, like the companion's, so a repeated Open in 3D reuses the open inspector. */
    const val INSPECTOR_TAG = "inspector"

    /** The immersive archive's Stage id (DSL only; opened with the Full style). */
    const val ARCHIVE_ID = "EverydayFuturesArchive"

    /** The two auxiliary windows; at most one of them is shown beside the primary. */
    val AUXILIARY: List<Surface> = listOf(Surface.COMPANION, Surface.INSPECTOR)

    /**
     * How long a requested open or close counts as in flight without its attach or detach. The emulator attaches in
     * 120–170 ms and detaches in about 340 ms; past this bound the request is treated as lost, so a missing platform
     * event can never wedge the policy.
     */
    const val IN_FLIGHT_MS = 3_000L

    fun attached(state: MuseumState, surface: Surface): MuseumState {
        val w = state.workspace.adjust(surface, +1)
        return state.copy(workspace = if (w.openingAux == surface) w.copy(openingAux = null) else w)
    }

    /** Clamped at zero, so an unmatched dispose can never produce a negative count. */
    fun detached(state: MuseumState, surface: Surface): MuseumState {
        val w = state.workspace.adjust(surface, -1)
        return state.copy(workspace = if (w.closingAux == surface && w.instances(surface) == 0) w.copy(closingAux = null) else w)
    }

    /** An open of [surface] has been requested at [now] (a focus of an attached window is not in flight). */
    fun opening(state: MuseumState, surface: Surface, now: Long): MuseumState =
        state.copy(workspace = state.workspace.copy(openingAux = surface, openingSinceMillis = now))

    /** A close of [surface] has been requested at [now]. */
    fun closing(state: MuseumState, surface: Surface, now: Long): MuseumState =
        state.copy(workspace = state.workspace.copy(closingAux = surface, closingSinceMillis = now))

    /** [surface] is to be closed as soon as it attaches (or no longer, with null). */
    fun withCloseOnAttach(state: MuseumState, surface: Surface?): MuseumState =
        if (state.workspace.closeOnAttach == surface) state else state.copy(workspace = state.workspace.copy(closeOnAttach = surface))

    /** Opening while the companion is attached asks for the same instance again (focus), never for a second one. */
    fun openRequest(state: MuseumState): WindowRequest = openRequest(state, Surface.COMPANION)

    fun closeRequest(): WindowRequest = closeRequest(Surface.COMPANION)

    /** Open or focus [surface] (an auxiliary window): focus while it is attached, so the platform reuses it. */
    fun openRequest(state: MuseumState, surface: Surface): WindowRequest =
        request(surface, if (state.workspace.instances(surface) > 0) WindowRequest.Kind.FOCUS else WindowRequest.Kind.OPEN)

    fun closeRequest(surface: Surface): WindowRequest = request(surface, WindowRequest.Kind.CLOSE)

    /**
     * The one-auxiliary-window policy for a request to show [target] at [now]: focus it if attached (or, if its close is
     * in flight, open it again after the detach); nothing more if its own open is in flight; if the other auxiliary
     * window is attached, close that one first (the target waits as pending); if the other one is still opening, wait
     * for it to attach and close it then; otherwise open the target. A third window is never requested, in flight or not.
     */
    fun planAux(state: MuseumState, target: Surface, now: Long = 0L): AuxPlan {
        require(target in AUXILIARY) { "not an auxiliary window: $target" }
        val workspace = state.workspace
        if (workspace.instances(target) > 0) {
            return if (workspace.closing(now) == target) AuxPlan.ReopenAfterClose(target) else AuxPlan.Show(openRequest(state, target))
        }
        val opening = workspace.opening(now)
        if (opening == target) return AuxPlan.AlreadyOpening(target)
        val other = AUXILIARY.firstOrNull { it != target && workspace.instances(it) > 0 }
        if (other != null) return AuxPlan.CloseFirst(other, target, closeRequest(other), repeated = workspace.pendingAux == target)
        if (opening != null) return AuxPlan.AfterOpening(opening, target)
        return AuxPlan.Show(openRequest(state, target))
    }

    /**
     * Applies a plan to the window record: a show clears anything pending (and records an open as in flight); a
     * close-first, an after-opening or a reopen leaves the target pending; asking for the window that is already opening
     * cancels anything pending and any close queued for it.
     */
    fun applyPlan(state: MuseumState, plan: AuxPlan, now: Long = 0L): MuseumState = when (plan) {
        is AuxPlan.Show -> withPending(state, null).let {
            if (plan.request.kind == WindowRequest.Kind.OPEN) opening(it, surfaceOf(plan.request), now) else it
        }
        is AuxPlan.CloseFirst -> closing(withPending(state, plan.target), plan.other, now)
        is AuxPlan.AfterOpening -> withCloseOnAttach(withPending(state, plan.target), plan.other)
        is AuxPlan.ReopenAfterClose -> withPending(state, plan.target)
        is AuxPlan.AlreadyOpening -> withPending(state, null).let {
            if (it.workspace.closeOnAttach == plan.target) withCloseOnAttach(it, null) else it
        }
    }

    /**
     * After a detach: the pending target may open now that no auxiliary window is attached or opening. Returns the open
     * request, or null when nothing is pending or the slot is still taken.
     */
    fun pendingOpen(state: MuseumState, now: Long = 0L): WindowRequest? {
        val target = state.workspace.pendingAux ?: return null
        if (state.workspace.attachedAux.isNotEmpty()) return null
        if (state.workspace.opening(now) != null) return null
        return request(target, WindowRequest.Kind.OPEN)
    }

    private fun surfaceOf(request: WindowRequest): Surface = when (request.id) {
        COMPANION_ID -> Surface.COMPANION
        INSPECTOR_ID -> Surface.INSPECTOR
        else -> error("not an auxiliary request: ${request.id}")
    }

    fun withPending(state: MuseumState, pending: Surface?): MuseumState =
        if (state.workspace.pendingAux == pending) state else state.copy(workspace = state.workspace.copy(pendingAux = pending))

    private fun request(surface: Surface, kind: WindowRequest.Kind): WindowRequest = when (surface) {
        Surface.COMPANION -> WindowRequest(COMPANION_ID, COMPANION_TAG, kind)
        Surface.INSPECTOR -> WindowRequest(INSPECTOR_ID, INSPECTOR_TAG, kind)
        else -> error("no window request for $surface")
    }

    private fun Workspace.adjust(surface: Surface, delta: Int): Workspace = when (surface) {
        Surface.PRIMARY -> copy(primaryInstances = (primaryInstances + delta).coerceAtLeast(0))
        Surface.COMPANION -> copy(companionInstances = (companionInstances + delta).coerceAtLeast(0))
        Surface.INSPECTOR -> copy(inspectorInstances = (inspectorInstances + delta).coerceAtLeast(0))
        Surface.ARCHIVE -> copy(archiveInstances = (archiveInstances + delta).coerceAtLeast(0))
    }
}
