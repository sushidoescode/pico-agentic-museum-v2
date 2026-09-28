package dev.local.everydayfutures.museum.content

/**
 * The inspector's live rotation between commits. App state holds the committed yaw ([committed] below); spin and
 * drag accumulate here, frame by frame, without touching app state or recomposing, and the accumulated turn is folded
 * into app state only when the visitor pauses, turns, resets, closes or ends a drag ([commit]). Pure Kotlin, so the
 * arithmetic is host-tested; the presenter calls it from the scene's frame events on the main thread.
 */
class InspectorPose(
    private val spinDegreesPerSecond: Float = SPIN_DEGREES_PER_SECOND,
    private val dragDegreesPerDp: Float = DRAG_DEGREES_PER_DP,
) {
    /** Degrees turned since the last commit (spin plus drag). */
    var pending: Float = 0f
        private set

    /** True while a drag is in progress: spin holds still under the visitor's hand. */
    var dragging: Boolean = false
        private set

    /** The yaw to show now: the committed yaw plus what spin and drag added since, in [0, 360). */
    fun live(committed: Float): Float = normalise(committed + pending)

    /**
     * One frame of spin: advances only while [spinning] (app state, already false under Reduce motion) and no drag is in
     * progress. [deltaSeconds] is clamped, so a stalled frame never jumps the model round. Returns true if it moved.
     */
    fun advance(deltaSeconds: Float, spinning: Boolean): Boolean {
        if (!spinning || dragging || !deltaSeconds.isFinite() || deltaSeconds <= 0f) return false
        pending = normalise(pending + deltaSeconds.coerceAtMost(MAX_FRAME_SECONDS) * spinDegreesPerSecond)
        return true
    }

    fun startDrag() {
        dragging = true
    }

    /** A drag step of [dp] horizontally (right is positive: the front turns toward the visitor's right). */
    fun dragBy(dp: Float) {
        if (!dp.isFinite()) return
        pending = normalise(pending + dp * dragDegreesPerDp)
    }

    /** Ends a drag (or a cancelled one) and returns the yaw to commit. */
    fun endDrag(committed: Float): Float {
        dragging = false
        return commit(committed)
    }

    /** The live yaw, folded out of this accumulator: the caller writes it to app state as the new committed yaw. */
    fun commit(committed: Float): Float {
        val yaw = live(committed)
        pending = 0f
        return yaw
    }

    /** Forgets the accumulated turn (Reset view, or a new object at its front view). */
    fun clear() {
        pending = 0f
        dragging = false
    }

    companion object {
        /** A slow turntable: one revolution in 18 s. */
        const val SPIN_DEGREES_PER_SECOND = 20f

        /** Drag sensitivity: a 180 dp sweep turns the model 90°. An app choice, to be tuned on a headset. */
        const val DRAG_DEGREES_PER_DP = 0.5f

        /** Longest frame step spin will take (seconds). */
        const val MAX_FRAME_SECONDS = 0.1f

        /** Degrees folded into [0, 360). */
        fun normalise(degrees: Float): Float {
            if (!degrees.isFinite()) return 0f
            val d = degrees % 360f
            val folded = (if (d < 0f) d + 360f else d) + 0f // + 0f turns -0.0 into 0.0
            return if (folded >= 360f) 0f else folded
        }
    }
}
