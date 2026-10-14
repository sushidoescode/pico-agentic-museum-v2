package dev.local.everydayfutures.museum.stage

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/** A point in the archive's runtime frame: metres, +Y up, the visitor faces −Z at arrival (Stage floor origin). */
data class Point3(val x: Float, val y: Float, val z: Float)

/**
 * Where a visitor stands and which way they face: [yawDegrees] about +Y, 0 = facing −Z, +90 = facing −X (a quarter
 * turn to the left). The same convention as the room layout manifest written by the Blender export.
 */
data class StandPoint(val position: Point3, val yawDegrees: Float)

/**
 * Where the room's root (or the panel) must go so that a visitor fixed at the Stage origin, facing −Z, or at a
 * [HeadFrame], sees the room from a [StandPoint].
 */
data class WorldPose(val position: Point3, val yawDegrees: Float)

/**
 * The visitor's head when an in-room tap moved them, in the Stage frame: the floor point under it, its heading (the
 * [StandPoint] yaw convention) and its height. Read from head tracking once per such tap; never measured otherwise.
 */
data class HeadFrame(val floor: Point3, val yawDegrees: Float, val eyeHeight: Float)

/**
 * How an arrival chose its [HeadFrame]: the one read at its tap ([HEAD]), the last one for any other move ([KEPT]), or
 * the last one because none could be read at its tap ([KEPT_NO_HEAD]).
 */
enum class ArrivalFrameSource {
    HEAD,
    KEPT,
    KEPT_NO_HEAD,
    ;

    /** The word in `archive-stand frame=<word>`. */
    val word: String get() = name.lowercase().replace('_', '-')
}

/**
 * Which [HeadFrame] the room and the panel are shown in, across one visit. An in-room tap that focuses another exhibit
 * [offer]s the frame read at the tap; the next placement adopts it only when it arrives at that exhibit's close look.
 * Every other move (the panel's bays, its shortlist, Back to the bay) keeps the last frame, so the panel the visitor
 * just pressed stays where it is. Placing the same stand again changes nothing, so the presenter may place as often as
 * it recomposes. Pure; main thread.
 */
class ArrivalFrames {
    /** The frame the room and the panel are shown in; null until a tap supplies one (the Stage origin, facing −Z). */
    var frame: HeadFrame? = null
        private set

    private var stand: StandPoint? = null
    private var offeredFor: String? = null
    private var offered: HeadFrame? = null

    /** A tap is about to focus [focusId]; [frame] is the head read at that tap, or null when none could be read. */
    fun offer(focusId: String, frame: HeadFrame?) {
        offeredFor = focusId
        offered = frame
    }

    /** The offered move was refused: the offer is never adopted by a later move. */
    fun drop() {
        offeredFor = null
        offered = null
    }

    /**
     * The room is placed at [stand] with [focusedId] in focus; any offer is used up here. Returns how the frame was
     * chosen when this is a move, or null for the visit's first placement and for the same stand again (no change).
     */
    fun arrive(stand: StandPoint, focusedId: String?): ArrivalFrameSource? {
        val forId = offeredFor
        val head = offered
        drop()
        if (stand == this.stand) return null
        val first = this.stand == null
        this.stand = stand
        if (first) return null
        if (forId == null || forId != focusedId) return ArrivalFrameSource.KEPT
        head ?: return ArrivalFrameSource.KEPT_NO_HEAD
        frame = head
        return ArrivalFrameSource.HEAD
    }
}

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
 * viewpoint's, for a visitor at the Stage origin facing −Z. After an in-room tap that read the visitor's head
 * ([HeadFrame]: floor point F, heading ψ), it is shown at F + R(ψ − yaw) · (q − p) instead, so the destination is ahead
 * of where they now face; with no frame every function gives exactly the origin-facing-−Z result. A change of viewpoint
 * is always an instant cut, never an animated camera ride.
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

    /** The root pose that shows the room from [stand] to a visitor at [frame]; [worldPoseFor] when there is none. */
    fun worldPoseFor(stand: StandPoint, frame: HeadFrame?): WorldPose {
        frame ?: return worldPoseFor(stand)
        val yaw = normaliseDegrees(frame.yawDegrees - stand.yawDegrees)
        val r = rotateY(stand.position, yaw)
        return WorldPose(Point3(frame.floor.x - r.x, -stand.position.y, frame.floor.z - r.z), yaw)
    }

    /** Where a room point appears from [stand] to a visitor at [frame] (one point of [worldPoseFor]); else [apparent]. */
    fun apparent(point: Point3, stand: StandPoint, frame: HeadFrame?): Point3 {
        frame ?: return apparent(point, stand)
        val p = stand.position
        val r = rotateY(Point3(point.x - p.x, point.y - p.y, point.z - p.z), frame.yawDegrees - stand.yawDegrees)
        return Point3(frame.floor.x + r.x, r.y, frame.floor.z + r.z)
    }

    /**
     * Shortest horizontal length (of a unit vector) a head axis needs to give a heading: below it, about 75° of pitch or
     * roll, the axis points too nearly straight up or down to say which way the visitor faces.
     */
    const val MIN_HEADING_LENGTH = 0.25f

    /**
     * The visitor's heading from their head's forward (−Z) axis, or from its right (+X) axis when they look nearly
     * straight up or down; null when neither is usable (or a reading is not a number).
     */
    fun headingDegrees(forward: Point3, right: Point3): Float? {
        val fh = sqrt(forward.x * forward.x + forward.z * forward.z)
        if (fh.isFinite() && fh >= MIN_HEADING_LENGTH) return degrees(atan2(-forward.x, -forward.z))
        val rh = sqrt(right.x * right.x + right.z * right.z)
        if (rh.isFinite() && rh >= MIN_HEADING_LENGTH) return degrees(atan2(-right.z, right.x))
        return null
    }

    /** A [HeadFrame] from one head reading (position, forward and right axes, Stage frame); null for an unusable one. */
    fun headFrame(position: Point3, forward: Point3, right: Point3): HeadFrame? {
        if (!position.x.isFinite() || !position.y.isFinite() || !position.z.isFinite()) return null
        val yaw = headingDegrees(forward, right) ?: return null
        return HeadFrame(Point3(position.x, 0f, position.z), yaw, position.y)
    }

    private fun degrees(radians: Float): Float = normaliseDegrees(Math.toDegrees(radians.toDouble()).toFloat())

    /**
     * The persistent panel hangs from its top edge at [PANEL_TOP] (Stage frame: the visitor at the origin facing −Z),
     * turned [PANEL_YAW] toward them, at [PANEL_SCALE] and at least [PANEL_MIN_HEIGHT_DP] tall, so its controls never
     * move as its content grows. Build-001's unscaled panel at (-0.55, 1.15, -1.0) was cut off by the emulator view's
     * lower edge; build-002's 0.6x was in view but its text small, so 0.75x; build-004's full room showed a centred
     * panel covering the leftmost exhibit of the Daily and Connected bays, so it hung from (-0.62, 1.84, -1.3) at 25°,
     * its lower edge about 1.59 m high. Review (2026-10) found that too far and too small to read (body text about 20
     * arcmin at 1.44 m). Build-009 stood it 1.10 m away on a 26° bearing at 0.85x; its emulator frames (2026-10-13)
     * cut its left third off: the view is about 34° each side (focal length about 1586 px on the 2160 px capture,
     * fitted from the panel's own right edge), and that panel reached 39° left. It now stands 1.18 m away on a 20°
     * bearing at 0.85x, spanning about 7.6°-32.4° left: inside that view, clear of the bay title (about 6° either side
     * of ahead), 1.38x larger in angle than build-008, its lower edge (about 1.61 m) about 4.2° below a 1.7 m eye.
     * Frame-tuned values, not a comfort claim.
     */
    val PANEL_TOP = Point3(-0.40f, 1.90f, -1.11f)
    const val PANEL_YAW = 20f
    const val PANEL_SCALE = 0.85f
    const val PANEL_MIN_HEIGHT_DP = 420f

    /** Bounds of the panel's top when it follows a measured eye height ([panelPose]): a seated or a tall visitor. */
    const val PANEL_TOP_MIN_Y = 1.25f
    const val PANEL_TOP_MAX_Y = 2.10f

    /**
     * The panel's top-centre pose: [PANEL_TOP] and [PANEL_YAW] without a frame; with one, the same place front-left of
     * the visitor's heading, its top as far above their eye as [PANEL_TOP] is above [NOMINAL_EYE_HEIGHT] (clamped).
     */
    fun panelPose(frame: HeadFrame?): WorldPose {
        frame ?: return WorldPose(PANEL_TOP, PANEL_YAW)
        val off = rotateY(Point3(PANEL_TOP.x, 0f, PANEL_TOP.z), frame.yawDegrees)
        val top = (frame.eyeHeight + PANEL_TOP.y - NOMINAL_EYE_HEIGHT).coerceIn(PANEL_TOP_MIN_Y, PANEL_TOP_MAX_Y)
        return WorldPose(Point3(frame.floor.x + off.x, top, frame.floor.z + off.z), normaliseDegrees(frame.yawDegrees + PANEL_YAW))
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
