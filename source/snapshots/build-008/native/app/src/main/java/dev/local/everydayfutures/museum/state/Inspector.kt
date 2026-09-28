package dev.local.everydayfutures.museum.state

import dev.local.everydayfutures.museum.content.InspectorPose

/**
 * The Open in 3D inspector's own presentation: which object it shows, its committed yaw (degrees, [0, 360)) and whether
 * it is spinning. Separate from the study's `turnSteps` and from any story's entry context, and never persisted: it
 * exists only while the inspector is requested or attached.
 */
data class InspectorState(val objectId: String, val yawDegrees: Float, val spinning: Boolean)

/**
 * Total reducers for the inspector presentation. No reducer here touches the selection, the study's orientation or
 * view, the story, the saved collection or the preferences; the one that reads a preference (Reduce motion) only
 * stops spin.
 */
object InspectorReducer {
    /** One Turn left / Turn right step, the same 30° as the study. */
    const val TURN_STEP_DEGREES = MuseumState.TURN_STEP_DEGREES

    /** The defined initial view: the selected object at the study's current pose, not spinning. */
    fun initial(state: MuseumState): InspectorState =
        InspectorState(state.selectedId, InspectorPose.normalise(state.turnDegrees), spinning = false)

    /**
     * Open in 3D. While the inspector is attached this is a focus and keeps its presentation; otherwise (first open, a
     * reopen after close, or a repeated press while the request is still pending) it starts from [initial].
     */
    fun open(state: MuseumState): MuseumState =
        if (state.workspace.inspectorOpen && state.inspector != null) state else state.copy(inspector = initial(state))

    /** A window that attached with no presentation (for example one the system restored) starts from [initial]. */
    fun ensure(state: MuseumState): MuseumState = if (state.inspector != null) state else state.copy(inspector = initial(state))

    /** Turn left/right by 30° from the live yaw; stops spin. */
    fun turn(state: MuseumState, direction: Turn, liveYaw: Float): MuseumState = update(state) {
        val delta = if (direction == Turn.RIGHT) TURN_STEP_DEGREES else -TURN_STEP_DEGREES
        it.copy(yawDegrees = InspectorPose.normalise(liveYaw + delta), spinning = false)
    }

    /** A drag ended at [yaw]; drag always stops spin. */
    fun dragged(state: MuseumState, yaw: Float): MuseumState = update(state) {
        it.copy(yawDegrees = InspectorPose.normalise(yaw), spinning = false)
    }

    /**
     * Spin / Pause. Pausing commits the live yaw. Starting spin is refused (stays off) while Reduce motion is on, so
     * spin never runs under Reduce motion.
     */
    fun toggleSpin(state: MuseumState, liveYaw: Float): MuseumState = update(state) {
        val yaw = InspectorPose.normalise(liveYaw)
        when {
            it.spinning -> it.copy(yawDegrees = yaw, spinning = false)
            state.options.reduceMotion -> it.copy(yawDegrees = yaw, spinning = false)
            else -> it.copy(yawDegrees = yaw, spinning = true)
        }
    }

    /** Reset view: the front view (yaw 0), spin off. */
    fun resetView(state: MuseumState): MuseumState = update(state) { it.copy(yawDegrees = 0f, spinning = false) }

    /**
     * The live pose settled after spin stopped for a reason the inspector did not initiate (Reduce motion switched on
     * in another window): the yaw on screen becomes the committed yaw, so state equals what is shown. Spin is untouched
     * (it is already off); nothing else changes.
     */
    fun settle(state: MuseumState, liveYaw: Float): MuseumState = commitYaw(state, liveYaw)

    /** Folds the live yaw into state without changing spin (used on Close). */
    fun commitYaw(state: MuseumState, liveYaw: Float): MuseumState = update(state) {
        it.copy(yawDegrees = InspectorPose.normalise(liveYaw))
    }

    /** A new selection while the inspector is open: the new object, at its front view, not spinning. */
    fun followSelection(state: MuseumState): MuseumState {
        val inspector = state.inspector ?: return state
        return if (inspector.objectId == state.selectedId) state
        else state.copy(inspector = InspectorState(state.selectedId, 0f, spinning = false))
    }

    /** Reduce motion on: spin stops at once (where the model is; the presenter keeps its live turn until the next commit). */
    fun honourReduceMotion(state: MuseumState): MuseumState {
        val inspector = state.inspector ?: return state
        return if (state.options.reduceMotion && inspector.spinning) state.copy(inspector = inspector.copy(spinning = false)) else state
    }

    /** The inspector window went away: its presentation is cleared, so a reopen reads the current selection. */
    fun detached(state: MuseumState): MuseumState = if (state.inspector == null) state else state.copy(inspector = null)

    private inline fun update(state: MuseumState, change: (InspectorState) -> InspectorState): MuseumState {
        val inspector = state.inspector ?: return state
        return state.copy(inspector = change(inspector))
    }
}
