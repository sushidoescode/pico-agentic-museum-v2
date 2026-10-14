package dev.local.everydayfutures.museum.state

import dev.local.everydayfutures.museum.catalogue.Catalogue
import dev.local.everydayfutures.museum.catalogue.Section

/** Where the immersive archive (a Full Space Stage) is in its life. */
enum class ArchivePhase { CLOSED, OPENING, OPEN, CLOSING }

/**
 * The archive's named viewpoints; [key] matches the viewpoint names in the room layout (`arrival`, `daily`,
 * `connected`, `domestic`).
 */
enum class ArchiveViewpoint {
    ARRIVAL,
    DAILY,
    CONNECTED,
    DOMESTIC,
    ;

    val key: String get() = name.lowercase()

    companion object {
        /** The bay that holds a section's six exhibits. */
        fun forSection(section: Section): ArchiveViewpoint = when (section) {
            Section.DAILY_RITUALS -> DAILY
            Section.CONNECTED_LIVES -> CONNECTED
            Section.DOMESTIC_SYSTEMS -> DOMESTIC
        }
    }
}

/**
 * The archive's app state: its [phase], the visitor's [viewpoint], the exhibit in close focus and how far it is turned,
 * and why the last open was refused (shown beside Enter archive). Never persisted.
 */
data class ArchiveState(
    val phase: ArchivePhase = ArchivePhase.CLOSED,
    val viewpoint: ArchiveViewpoint = ArchiveViewpoint.ARRIVAL,
    val focusedId: String? = null,
    val lastRefusal: String? = null,
    /**
     * The focused exhibit's turn on its plinth, in [TURN_STEP_DEGREES] steps (unbounded). The room's own, independent of
     * the study's turn; a new focus, a viewpoint move and leaving bring it back to the front.
     */
    val turnSteps: Int = 0,
) {
    /** The focused exhibit's yaw on its plinth, in degrees (+ turns its front toward the visitor's right). */
    val turnDegrees: Float get() = turnSteps * TURN_STEP_DEGREES

    companion object {
        /** Eight steps return exactly to the front. */
        const val TURN_STEP_DEGREES = 45f
        const val STEPS_PER_TURN = 8
    }
}

/**
 * Total reducers for the archive. Selection, saved items, preferences, the catalogue filter and the story are left
 * untouched by all of them, with one deliberate exception: [focus] selects its object through the ordinary select
 * reducer, exactly as a click in the catalogue would (so an open story follows the selection as usual).
 */
object ArchiveReducer {
    /** Enter archive: CLOSED → OPENING at the arrival viewpoint with nothing focused; ignored in any other phase. */
    fun enterRequested(state: MuseumState): MuseumState =
        if (state.archive.phase != ArchivePhase.CLOSED) state
        else state.copy(archive = ArchiveState(phase = ArchivePhase.OPENING))

    /** The Stage open finished: OPENING → OPEN, or → CLOSED recording [reason]. Ignored unless OPENING. */
    fun openResult(state: MuseumState, allowed: Boolean, reason: String): MuseumState {
        if (state.archive.phase != ArchivePhase.OPENING) return state
        return state.copy(
            archive = if (allowed) state.archive.copy(phase = ArchivePhase.OPEN, lastRefusal = null)
            else ArchiveState(phase = ArchivePhase.CLOSED, lastRefusal = reason.ifBlank { "refused" }),
        )
    }

    /** Move to a named viewpoint (OPEN only); clears the close focus and its turn. */
    fun goTo(state: MuseumState, viewpoint: ArchiveViewpoint): MuseumState =
        if (state.archive.phase != ArchivePhase.OPEN) state
        else state.copy(archive = state.archive.copy(viewpoint = viewpoint, focusedId = null, turnSteps = 0))

    /**
     * Focus one exhibit (OPEN only, known ids only): selects it and moves to its section's bay. A different exhibit starts
     * at its front; focusing the one already in focus again keeps its turn.
     */
    fun focus(state: MuseumState, id: String): MuseumState {
        if (state.archive.phase != ArchivePhase.OPEN) return state
        val artifact = Catalogue.byId(id) ?: return state
        val selected = MuseumReducer.select(state, id)
        return selected.copy(
            archive = selected.archive.copy(
                viewpoint = ArchiveViewpoint.forSection(artifact.section),
                focusedId = id,
                turnSteps = if (id == state.archive.focusedId) state.archive.turnSteps else 0,
            ),
        )
    }

    /**
     * True when [focus] would move the visitor to [id]'s close look from somewhere else: the archive is OPEN, [id] is
     * known and it is not the exhibit already in focus (a tap on that one turns it, or focuses it again where it is).
     * The room reads the visitor's head only for such a tap (on the exhibit, its support or its floor circle), and a floor
     * circle acts only then.
     */
    fun wouldFocusAnew(state: MuseumState, id: String): Boolean =
        state.archive.phase == ArchivePhase.OPEN && Catalogue.byId(id) != null && state.archive.focusedId != id

    /** True when [turn] would act: the archive is OPEN and [id] is the exhibit in focus. */
    fun wouldTurn(state: MuseumState, id: String): Boolean =
        state.archive.phase == ArchivePhase.OPEN && state.archive.focusedId == id

    /**
     * Turns the focused exhibit one step (+[ArchiveState.TURN_STEP_DEGREES]) on its plinth; ignored unless [wouldTurn].
     * Nothing else changes: not the selection, the study's own turn, saved objects, preferences, filter or story.
     */
    fun turn(state: MuseumState, id: String): MuseumState =
        if (!wouldTurn(state, id)) state
        else state.copy(archive = state.archive.copy(turnSteps = state.archive.turnSteps + 1))

    /** Return to catalogue: OPEN or OPENING → CLOSING. */
    fun returnRequested(state: MuseumState): MuseumState =
        if (state.archive.phase == ArchivePhase.OPEN || state.archive.phase == ArchivePhase.OPENING) {
            state.copy(archive = state.archive.copy(phase = ArchivePhase.CLOSING))
        } else state

    /** The Stage is gone (closed by Return, or by the system): → CLOSED at the arrival viewpoint; a refusal note survives. */
    fun closed(state: MuseumState): MuseumState =
        if (state.archive.phase == ArchivePhase.CLOSED) state
        else state.copy(archive = ArchiveState(phase = ArchivePhase.CLOSED, lastRefusal = state.archive.lastRefusal))
}

/** What opening the Stage returned (mirrors the SDK's `OpenStageResult` without depending on it). */
sealed class StageOpenResult {
    object Allowed : StageOpenResult()

    object NotAllowed : StageOpenResult()

    data class Error(val code: Int, val reason: String) : StageOpenResult()

    /** The word in `archive-open result=<word>`. */
    val resultWord: String
        get() = when (this) {
            Allowed -> "allowed"
            NotAllowed -> "not-allowed"
            is Error -> "error"
        }

    /** The word in `archive-open … reason=<word>`: `ok`, `not-allowed`, or `code-<n>[-<sdk reason>]`. */
    val reasonWord: String
        get() = when (this) {
            Allowed -> "ok"
            NotAllowed -> "not-allowed"
            is Error -> "code-$code" + reason.trim().takeIf { it.isNotEmpty() }?.let { "-" + it.replace(' ', '-') }.orEmpty()
        }
}

/**
 * The platform Stage calls the app makes, reduced to a port like [WindowNavigator] so the flow is host-tested. The SDK
 * adapter (content/MuseumRoot.kt) opens with `StageStyle.Full`; minimize/restore work only while a Stage is open.
 */
interface StageNavigator {
    suspend fun open(id: String): StageOpenResult

    suspend fun close()

    fun minimize(id: String, tag: String?): Boolean

    fun restore(id: String, tag: String?): Boolean
}
