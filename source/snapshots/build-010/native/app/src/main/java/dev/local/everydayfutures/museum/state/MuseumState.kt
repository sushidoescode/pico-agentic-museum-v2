package dev.local.everydayfutures.museum.state

import dev.local.everydayfutures.museum.catalogue.Catalogue
import dev.local.everydayfutures.museum.catalogue.CatalogueFilter

/**
 * The study's mode: INSPECT once the object has been turned, CATALOGUE after a selection or Reset. It has no visible
 * effect of its own; it is kept for the `view=` markers and the story's entry context. It never changes the selection.
 */
enum class View { CATALOGUE, INSPECT }

/** Direction of one inspection turn step. */
enum class Turn { LEFT, RIGHT }

/** Comfort preferences owned by the app and mirrored into the polish kit's options at the theme root. */
data class ComfortOptions(
    val reduceMotion: Boolean = false,
    val reduceTransparency: Boolean = false,
    val increaseContrast: Boolean = false,
)

/**
 * Single source of truth, shared by every window root through one app-owned [MuseumController]. Pure Kotlin: no
 * Android or Compose import, so every transition is host-testable.
 */
data class MuseumState(
    val selectedId: String = Catalogue.QUENCHER,
    val view: View = View.CATALOGUE,
    val turnSteps: Int = 0,
    val savedIds: List<String> = emptyList(),
    val options: ComfortOptions = ComfortOptions(),
    /** Which window roots are live (L2). Logical state above is shared by every root; this is never persisted. */
    val workspace: Workspace = Workspace(),
    /** The open miniature story (L3), shown in the companion; never persisted. */
    val story: StoryState? = null,
    /** What the catalogue grid shows (section, All/Saved, search); never persisted. */
    val filter: CatalogueFilter = CatalogueFilter(),
    /** The Open in 3D inspector's own presentation while it is requested or attached; never persisted. */
    val inspector: InspectorState? = null,
    /** The immersive archive (L4 Stage); never persisted. */
    val archive: ArchiveState = ArchiveState(),
) {
    /** Rotation of the study object around the vertical axis, in degrees, derived from the step count (unbounded, absolute pose). */
    val turnDegrees: Float get() = turnSteps * TURN_STEP_DEGREES

    /** The same rotation normalised to 0..330 for display, so the status never reads 390° or -30°. */
    val displayDegrees: Int get() = Math.floorMod(turnSteps, STEPS_PER_TURN) * TURN_STEP_DEGREES.toInt()

    /** True when the object is at its front pose; the reset control is disabled in this state. */
    val isFrontView: Boolean get() = Math.floorMod(turnSteps, STEPS_PER_TURN) == 0

    /** Reset has something to do only when the object is turned; at the front view the control is disabled. */
    val canReset: Boolean get() = !isFrontView

    fun isSaved(id: String = selectedId): Boolean = id in savedIds

    /**
     * True while the Open in 3D inspector is attached and shows the study's own object, and no switch away from it is
     * pending: the study then hides its model (not destroyed, same pose) so two copies of the same object never overlap
     * when the platform does not fade the primary (build-003). False while the inspector is being closed for the
     * companion, and after it has detached.
     */
    val studyShownInInspector: Boolean
        get() = workspace.inspectorOpen && workspace.pendingAux == null && inspector?.objectId == selectedId

    companion object {
        /** Twelve equal steps return exactly to the front; the browser draft's 25° step did not. */
        const val TURN_STEP_DEGREES = 30f
        const val STEPS_PER_TURN = 12
    }
}

/** Total reducers. Every function returns a new state and never throws on unexpected input. */
object MuseumReducer {
    /**
     * Selecting an object resets its orientation and returns to the catalogue view; an open story follows the selection
     * to the new object's chapter 1, or ends when the new object has no story; an open inspector shows the new object at
     * its front view. The catalogue filter is untouched (a hidden selection stays selected). Unknown ids are ignored.
     */
    fun select(state: MuseumState, id: String): MuseumState =
        if (!Catalogue.contains(id)) state
        else InspectorReducer.followSelection(
            StoryReducer.followSelection(state.copy(selectedId = id, view = View.CATALOGUE, turnSteps = 0)),
        )

    /** A turn always enters INSPECT (logged as `view=inspect`); the study shows the turn itself as its angle. */
    fun turn(state: MuseumState, direction: Turn): MuseumState {
        val delta = if (direction == Turn.RIGHT) 1 else -1
        return state.copy(view = View.INSPECT, turnSteps = state.turnSteps + delta)
    }

    /** Reset returns to the front pose and the catalogue view; selection, saved objects and preferences survive. */
    fun reset(state: MuseumState): MuseumState = state.copy(view = View.CATALOGUE, turnSteps = 0)

    /** Keeping an object is idempotent: the count increments once and order is preserved. */
    fun keep(state: MuseumState): MuseumState =
        if (state.isSaved()) state else state.copy(savedIds = state.savedIds + state.selectedId)

    /** Removing the selected object from the shortlist is idempotent: the rest keep their order and nothing else changes. */
    fun unkeep(state: MuseumState): MuseumState =
        if (!state.isSaved()) state else state.copy(savedIds = state.savedIds.filterNot { it == state.selectedId })

    fun clearSaved(state: MuseumState): MuseumState = state.copy(savedIds = emptyList())

    /** Preferences apply at once; switching Reduce motion on also stops the inspector's spin. */
    fun setOptions(state: MuseumState, options: ComfortOptions): MuseumState =
        InspectorReducer.honourReduceMotion(state.copy(options = options))
}
