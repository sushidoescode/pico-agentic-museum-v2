package dev.local.everydayfutures.museum.stage

import dev.local.everydayfutures.museum.content.FadeIn

/**
 * The arrival ring, free of SDK types so it is host-tested: after a move (still an instant cut) one thin warm-brass
 * floor ring round the visitor's feet widens a little and fades, then is gone. It waits for the room's fade-in to end
 * ([delaySeconds]): it is part of the room, so while the room is still part-transparent it would play at a fraction of
 * its strength. It is never shown under Reduce motion and has no collider, so it is never a tap target. Its radii stay
 * between the room's own brass arrival inlays (rings of radius 0.50 m and 1.50 m, 25 and 40 mm wide, 3 mm proud:
 * art/room/build_room.py floor inlays), and its band clears their top. Below a level forward view at a 1.7 m eye (its
 * nearest point is 55-65° down), so it reads as a cue when glancing down; a frame looking straight ahead is not
 * expected to show it.
 */
object ArrivalCue {
    const val DURATION_SECONDS = 0.6f
    const val START_RADIUS = 0.8f
    const val END_RADIUS = 1.2f
    const val START_OPACITY = 0.6f

    /** The torus mesh's outer and inner radii (metres) at scale 1: a 1.5 cm band, so 7.5 mm half-height. */
    const val MESH_OUTER = 1.0f
    const val MESH_INNER = 0.985f

    /** Height of the band's centre above the floor: its bottom is 4.5 mm up, clear of the 3 mm inlays. */
    const val LIFT = 0.012f

    /** One frame of the ring: its outer [radius] (metres) and its [opacity]. */
    data class Frame(val radius: Float, val opacity: Float)

    /** Only a move plays the ring, and never under Reduce motion. */
    fun shouldPlay(reduceMotion: Boolean, moved: Boolean): Boolean = moved && !reduceMotion

    /** How long the ring waits after a move: until the room's fade-in is over ([FadeIn.ROOM_MILLIS]), or not at all. */
    fun delaySeconds(roomFades: Boolean): Float = if (roomFades) FadeIn.ROOM_MILLIS / 1000f else 0f

    /** The ring [elapsedSeconds] after it started (eased out), or null once it is over (or before it began). */
    fun at(elapsedSeconds: Float): Frame? {
        if (!elapsedSeconds.isFinite() || elapsedSeconds < 0f || elapsedSeconds >= DURATION_SECONDS) return null
        val t = elapsedSeconds / DURATION_SECONDS
        val ease = 1f - (1f - t) * (1f - t) * (1f - t)
        return Frame(START_RADIUS + (END_RADIUS - START_RADIUS) * ease, START_OPACITY * (1f - ease))
    }
}
