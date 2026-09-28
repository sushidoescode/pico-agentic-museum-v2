package dev.local.everydayfutures.museum.content

/** Which model a surface shows for one exhibit; [marker] is the word in `exhibit-model … model=<marker>`. */
enum class ModelKind {
    /** The exhibit's own packaged model (GLB), loaded and fitted. */
    GENERATED,

    /** One of the three original objects' procedural stand-ins, after its model could not be loaded. */
    PROCEDURAL,

    /** Nothing: the surface shows an explicit error naming the object instead. */
    NONE,
    ;

    val marker: String get() = name.lowercase()
}

/** What one load attempt produced. [cause] words: `missing`, `timeout`, `load-<code>`, `bounds`, `fit-<error>`, `attach-<error>`. */
sealed class ModelOutcome {
    abstract val kind: ModelKind

    /** The value of `reason=` in the `exhibit-model` marker. */
    abstract val reason: String

    data class Generated(val authoredBounds: Boolean) : ModelOutcome() {
        override val kind: ModelKind get() = ModelKind.GENERATED
        override val reason: String get() = if (authoredBounds) "ok-authored-bounds" else "ok"
    }

    data class Procedural(val cause: String) : ModelOutcome() {
        override val kind: ModelKind get() = ModelKind.PROCEDURAL
        override val reason: String get() = "fallback-procedural-$cause"
    }

    data class Failed(val cause: String) : ModelOutcome() {
        override val kind: ModelKind get() = ModelKind.NONE
        override val reason: String get() = cause
    }
}

/**
 * The load state of one surface (the study or the inspector) for its current object. Only the selected object is
 * ever loaded; a new request carries a new [Loading.token], and a result for any other token is stale and discarded.
 */
sealed class ModelLoadState {
    /** Nothing requested yet (the scene is still being created). */
    object Idle : ModelLoadState()

    /** Attempt [attempt] for [id] is in flight; the surface shows a loading card naming the object. */
    data class Loading(val id: String, val attempt: Int, val token: Long) : ModelLoadState()

    /** A model is shown for [id]: its own, or (for the three originals) the procedural stand-in. */
    data class Ready(val id: String, val attempt: Int, val outcome: ModelOutcome) : ModelLoadState()

    /** Nothing could be shown for [id] after [attempts] attempts; the surface names the object and may offer Try again. */
    data class Failed(val id: String, val reason: String, val attempts: Int) : ModelLoadState() {
        val canRetry: Boolean get() = attempts < ModelLoadMachine.MAX_ATTEMPTS
    }

    /** The object this state is about, or null while idle. */
    val objectId: String?
        get() = when (this) {
            Idle -> null
            is Loading -> id
            is Ready -> id
            is Failed -> id
        }
}

/**
 * Pure decisions for lazy, per-selection model loading, shared by the study and the inspector. The presenter owns the
 * SDK calls and the coroutine; this decides what the surface shows and which results still count.
 */
object ModelLoadMachine {
    /** Try again is offered twice per selection: the first attempt plus two retries. */
    const val MAX_RETRIES = 2
    const val MAX_ATTEMPTS = 1 + MAX_RETRIES

    /**
     * True when [cause] means the model file is simply not there (the app's asset pre-check, or the SDK's NO_ENTRY): an
     * expected state while the collection is being filled, logged as the one `exhibit-model … reason=` line with no
     * stack trace. Every other failure is unexpected and keeps its stack trace in the log.
     */
    fun isMissing(cause: String): Boolean = cause == "missing" || cause == "load-NO_ENTRY"

    /** A new selection (or the first) starts at attempt 1; a retry passes its own attempt number, capped. */
    fun start(id: String, attempt: Int, token: Long): ModelLoadState.Loading =
        ModelLoadState.Loading(id, attempt.coerceIn(1, MAX_ATTEMPTS), token)

    /** The next attempt after a failure, or null when the retries for this selection are spent. */
    fun retryAttempt(state: ModelLoadState): Int? =
        (state as? ModelLoadState.Failed)?.takeIf { it.canRetry }?.let { it.attempts + 1 }

    /**
     * Applies a finished attempt. Null when the result is stale: the surface has moved on to another request (another
     * object, or a retry) since [token] was issued, so the caller must discard what it built.
     */
    fun complete(state: ModelLoadState, token: Long, id: String, outcome: ModelOutcome): ModelLoadState? {
        if (state !is ModelLoadState.Loading || state.token != token || state.id != id) return null
        return when (outcome) {
            is ModelOutcome.Failed -> ModelLoadState.Failed(id, outcome.reason, state.attempt)
            else -> ModelLoadState.Ready(id, state.attempt, outcome)
        }
    }

    /**
     * The outcome for an attempt whose own model failed with [cause]: the procedural stand-in when the object has one
     * and it was built ([proceduralBuilt]), otherwise an explicit failure. Never another object's model.
     */
    fun afterModelFailure(cause: String, proceduralFallback: Boolean, proceduralBuilt: Boolean): ModelOutcome = when {
        proceduralFallback && proceduralBuilt -> ModelOutcome.Procedural(cause)
        proceduralFallback -> ModelOutcome.Failed("$cause+procedural-failed")
        else -> ModelOutcome.Failed(cause)
    }
}

/** The fit boxes of the two surfaces that show one exhibit at a time (metres, in each SpatialView's frame). */
object ExhibitBoxes {
    /**
     * The study: the accepted vessel footprint's height and base (0.28 m tall, base at y = -0.075 m, on x/z = 0), and a
     * 0.26 m square footprint — the study region is about 450 dp wide in the eighteen-object layout, about 0.26 m at the
     * ~1,750 dp per scene metre observed in Lesson 1. Build-003's eighteen-object sweep showed the widest models (the
     * robot vacuum) just reaching the details column at 0.26 m, so the width is 0.23 m for a margin.
     */
    val STUDY = FitBox(width = 0.23f, height = 0.28f, depth = 0.26f, baseY = -0.075f)

    /**
     * The inspector: a 0.30 m box in the 720 x 640 x 560 dp volume, standing low (base at y = -0.17 m) so the title
     * chip at the top stays clear of the model. To be tuned from opened frames.
     */
    val INSPECTOR = FitBox(width = 0.30f, height = 0.30f, depth = 0.30f, baseY = -0.17f)
}
