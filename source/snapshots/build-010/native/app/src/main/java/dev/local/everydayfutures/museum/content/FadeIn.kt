package dev.local.everydayfutures.museum.content

/**
 * The short fade-ins, free of SDK types so they are host-tested: a newly shown model in the study and the Open in 3D
 * view, and the archive's room after a move to a new stand. Each eases an opacity up from 0 to 1, quickly at first and
 * settling at the end. Nothing fades out (an outgoing model is hidden at once, and the room's move is still an instant
 * cut), and nothing fades under Reduce motion: the content then shows at full opacity from its first frame. A clock
 * that gives no usable time always ends a fade at full opacity, so content is never left transparent.
 */
object FadeIn {
    /** A new model: the platform's state-change duration (the polish kit's DurationStateMillis, 180 ms). */
    const val MODEL_MILLIS = 180L

    /** The room after a move: a little longer, as the whole view changes. An app choice, to tune natively. */
    const val ROOM_MILLIS = 250L

    /** Why a fade does not play: `reduce-motion`, or `hidden` for content not on screen; null when it plays. */
    fun skipReason(reduceMotion: Boolean, visible: Boolean = true): String? = when {
        reduceMotion -> "reduce-motion"
        !visible -> "hidden"
        else -> null
    }

    fun shouldPlay(reduceMotion: Boolean, visible: Boolean = true): Boolean = skipReason(reduceMotion, visible) == null

    /** True once [elapsedMillis] reaches [durationMillis]; a time that is not a finite number counts as over. */
    fun isOver(elapsedMillis: Float, durationMillis: Long): Boolean =
        !elapsedMillis.isFinite() || durationMillis <= 0L || elapsedMillis >= durationMillis

    /**
     * The opacity [elapsedMillis] into a fade lasting [durationMillis]: 0 at its start (and before it), eased out to 1,
     * and exactly 1 once it is over ([isOver]).
     */
    fun opacityAt(elapsedMillis: Float, durationMillis: Long): Float {
        if (isOver(elapsedMillis, durationMillis)) return 1f
        if (elapsedMillis <= 0f) return 0f
        val t = elapsedMillis / durationMillis
        return 1f - (1f - t) * (1f - t) * (1f - t)
    }
}
