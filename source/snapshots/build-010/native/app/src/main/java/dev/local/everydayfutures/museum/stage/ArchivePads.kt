package dev.local.everydayfutures.museum.stage

import kotlin.math.cos
import kotlin.math.sin

/**
 * The floor circles, free of SDK types so they are host-tested: one per exhibit, a thin unlit ring round a faint disc,
 * flat on the floor, which steps the visitor up to that exhibit's close look ([ArchiveGeometry.closeLook]) when pinched
 * or clicked, exactly as a first tap on the exhibit does. They are not drawn at the close-look stand points: those are
 * too crowded for circles of this size (0.30 m apart in the Connected bay, 0.31 m for the router and the doorbell) and
 * the scooter's lies on the 1.5 m arrival inlay. So each circle lies on the floor directly in front of its support, on
 * the exhibit's front axis, [CLEARANCE] beyond the bench face or the platform edge ([ArchiveTargets.floorFront]); the
 * tap still moves to the close look, 1.4 m or more out from the anchor. They never move; the one of the exhibit in focus
 * is hidden ([shown]). All of them are 2.6 m or more from the arrival point, well clear of its brass inlays (r 0.50 and
 * 1.50 m), and much smaller and paler than the brass arrival ring ([ArrivalCue], 0.8-1.2 m round the visitor).
 */
object ArchivePads {
    /**
     * The ring's outer radius (metres): the largest that keeps the closest pair, the domestic bay's router and doorbell
     * (0.38 m apart), 6 cm apart at the edge, with a square collider each that still does not touch its neighbour's.
     */
    const val RADIUS = 0.16f

    /** The ring's inner radius: a 1.2 cm band (a torus with a 6 mm tube, so 12 mm tall). */
    const val RING_INNER = 0.148f

    /** Height of the ring's centre: its bottom is 2 mm above the floor. */
    const val RING_LIFT = 0.008f

    /** The faint disc inside the ring: radius, thickness and the height of its centre (from 2 to 4 mm up). */
    const val DISC_RADIUS = RING_INNER
    const val DISC_HEIGHT = 0.002f
    const val DISC_LIFT = 0.003f

    /** Opacities: a restrained ring that lets the floor through a little, and a disc that only tints it. */
    const val RING_OPACITY = 0.8f
    const val DISC_OPACITY = 0.18f

    /** Floor left between a support's front and a circle's edge. */
    const val CLEARANCE = 0.06f

    /**
     * The tap target, in the circle's frame (origin on the floor at its centre, turned like its anchor): a 3 cm slab just
     * above the floor, as wide as the ring.
     */
    val COLLIDER = TargetBox(Point3(0f, 0.02f, 0f), Point3(2f * RADIUS, 0.03f, 2f * RADIUS))

    /** How far in front of [anchor] its circle's centre lies, along its front. */
    fun distance(anchor: Anchor): Float = ArchiveTargets.floorFront(anchor) + CLEARANCE + RADIUS

    /** The circle's centre on the floor (y = 0), in the room frame; its own frame is turned by the anchor's yaw. */
    fun centre(anchor: Anchor): Point3 {
        val rad = Math.toRadians(anchor.yawDegrees.toDouble())
        val d = distance(anchor)
        return Point3(anchor.position.x + sin(rad).toFloat() * d, 0f, anchor.position.z + cos(rad).toFloat() * d)
    }

    /** Whether [id]'s circle shows: every one but the focused exhibit's, whose close look is where the visitor stands. */
    fun shown(id: String, focusedId: String?): Boolean = id != focusedId
}
