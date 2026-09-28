package dev.local.everydayfutures.museum.stage

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

/** A point in the archive's runtime frame: metres, +Y up, the visitor faces −Z at arrival (Stage floor origin). */
data class Point3(val x: Float, val y: Float, val z: Float)

/**
 * Where a visitor stands and which way they face: [yawDegrees] about +Y, 0 = facing −Z, +90 = facing −X (a quarter
 * turn to the left). The same convention as the room layout manifest written by the Blender export.
 */
data class StandPoint(val position: Point3, val yawDegrees: Float)

/** Where the room's root must go so that a visitor fixed at the Stage origin, facing −Z, sees the room from a [StandPoint]. */
data class WorldPose(val position: Point3, val yawDegrees: Float)

/**
 * A flat app label placed in the room (a plaque face or a bay sign): its centre, the way its front faces ([yawDegrees],
 * same convention as a viewpoint's facing but for the label's front), its tilt up from vertical ([pitchDegrees]) and,
 * when the room gives it, the clean area the label may fill ([widthMetres] x [heightMetres]).
 */
data class Placard(
    val position: Point3,
    val yawDegrees: Float,
    val pitchDegrees: Float = 0f,
    val widthMetres: Float? = null,
    val heightMetres: Float? = null,
)

/**
 * One exhibit position in the room: the plinth-top point the object stands on, the way its front faces, its bay, its
 * display scale (1 = real size) and, when the room provides one, the plaque where its readable label goes.
 */
data class Anchor(
    val id: String,
    val position: Point3,
    val yawDegrees: Float,
    val bay: String,
    val displayScale: Float = 1f,
    val plaque: Placard? = null,
)

/**
 * Viewpoint arithmetic for the archive, free of SDK types so it is host-tested. The Stage's visitor cannot be moved,
 * so a viewpoint moves the room instead: a room point q is shown at R(−yaw) · (q − p), where p and yaw are the
 * viewpoint's. A change of viewpoint is always an instant cut, never an animated camera ride.
 */
object ArchiveGeometry {
    /**
     * Nearest a close look stands in front of an exhibit's anchor (metres, on the floor). Build-002's frame at 0.85 m put
     * a waist-height exhibit about 41 degrees below eye level, outside the emulator's ~36 degree half view; at 1.4 m it
     * sits about 27 degrees below.
     */
    const val CLOSE_LOOK_DISTANCE = 1.4f

    /**
     * Eye height the close-look distance is planned for (metres). The Stage keeps the visitor's own height; build-004's
     * frames put the emulator's eye at about 1.70 m, and the room's renders use 1.75 m. A planning value, not a measurement
     * of any visitor.
     */
    const val NOMINAL_EYE_HEIGHT = 1.7f

    /**
     * Largest angle below eye level at which an exhibit's base is planned in a close look, inside the emulator's ~36 degree
     * half view with a margin. Build-004's close look at the floor-standing scooter from 1.4 m showed only its handlebar.
     */
    const val CLOSE_LOOK_MAX_DROP_DEGREES = 30f

    /** How far in front of [anchor] its close look stands: [CLOSE_LOOK_DISTANCE], or further for a low exhibit. */
    fun closeLookDistance(anchor: Anchor): Float {
        val drop = NOMINAL_EYE_HEIGHT - anchor.position.y
        val needed = drop / tan(Math.toRadians(CLOSE_LOOK_MAX_DROP_DEGREES.toDouble())).toFloat()
        return maxOf(CLOSE_LOOK_DISTANCE, needed)
    }

    /** The root pose that shows the room from [stand]. */
    fun worldPoseFor(stand: StandPoint): WorldPose {
        val p = stand.position
        val moved = rotateY(Point3(-p.x, -p.y, -p.z), -stand.yawDegrees)
        return WorldPose(moved, -stand.yawDegrees)
    }

    /** Where a room point appears for a visitor at [stand] (the same transform as [worldPoseFor], applied to one point). */
    fun apparent(point: Point3, stand: StandPoint): Point3 {
        val p = stand.position
        return rotateY(Point3(point.x - p.x, point.y - p.y, point.z - p.z), -stand.yawDegrees)
    }

    /**
     * A close look at one exhibit: on the floor [closeLookDistance] in front of its anchor (along the direction its front
     * faces), facing it. An anchor facing +X (yaw +90°) is looked at from its +X side by a visitor facing −X (yaw +90°).
     */
    fun closeLook(anchor: Anchor): StandPoint {
        val rad = Math.toRadians(anchor.yawDegrees.toDouble())
        val fx = sin(rad).toFloat()
        val fz = cos(rad).toFloat()
        val d = closeLookDistance(anchor)
        return StandPoint(
            Point3(anchor.position.x + fx * d, 0f, anchor.position.z + fz * d),
            normaliseDegrees(anchor.yawDegrees),
        )
    }

    /**
     * Metres per dp of an attachment panel at entity scale 1, measured on the emulator (build-004): a 300 dp plaque label
     * at scale 0.5 came out 0.12 m wide against its 0.20 m brass plate. A measurement, not an SDK constant.
     */
    const val METRES_PER_DP = 0.0008f

    /** Share of a placard's clean width a label fills, leaving a margin for the measurement's error. */
    const val LABEL_FILL = 0.9f

    /** How far a label sits in front of its surface, along the surface's normal, so it never z-fights the plate or wall. */
    const val LABEL_STANDOFF = 0.004f

    /**
     * Entity scale for a label [contentWidthDp] wide on [placard]: [LABEL_FILL] of the placard's width, and never taller
     * than its height when [contentHeightDp] is known. [fallback] when the room gives no size.
     */
    fun labelScale(placard: Placard, contentWidthDp: Float, contentHeightDp: Float? = null, fallback: Float): Float {
        val width = placard.widthMetres ?: return fallback
        val byWidth = LABEL_FILL * width / (contentWidthDp * METRES_PER_DP)
        val height = placard.heightMetres ?: return byWidth
        val h = contentHeightDp ?: return byWidth
        return minOf(byWidth, LABEL_FILL * height / (h * METRES_PER_DP))
    }

    /** A label's centre: [LABEL_STANDOFF] in front of the placard's face, along its (yawed, tilted) normal. */
    fun labelPosition(placard: Placard): Point3 {
        val yaw = Math.toRadians(placard.yawDegrees.toDouble())
        val pitch = Math.toRadians(placard.pitchDegrees.toDouble())
        val horizontal = cos(pitch).toFloat() * LABEL_STANDOFF
        return Point3(
            placard.position.x + sin(yaw).toFloat() * horizontal,
            placard.position.y + sin(pitch).toFloat() * LABEL_STANDOFF,
            placard.position.z + cos(yaw).toFloat() * horizontal,
        )
    }

    /** Rotation about +Y by [degrees], right-handed: +90° takes −Z to −X and +Z to +X. */
    fun rotateY(v: Point3, degrees: Float): Point3 {
        val rad = Math.toRadians(degrees.toDouble())
        val c = cos(rad).toFloat()
        val s = sin(rad).toFloat()
        return Point3(v.x * c + v.z * s, v.y, -v.x * s + v.z * c)
    }

    /** Degrees folded into (−180, 180]. */
    fun normaliseDegrees(degrees: Float): Float {
        var d = degrees % 360f
        if (d <= -180f) d += 360f
        if (d > 180f) d -= 360f
        return d
    }
}
