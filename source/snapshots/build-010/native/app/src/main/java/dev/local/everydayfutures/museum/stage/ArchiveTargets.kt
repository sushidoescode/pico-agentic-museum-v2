package dev.local.everydayfutures.museum.stage

/** An axis-aligned box in some local frame (metres): its [centre] and its full [size] along x, y and z. */
data class TargetBox(val centre: Point3, val size: Point3) {
    val min: Point3 get() = Point3(centre.x - size.x / 2f, centre.y - size.y / 2f, centre.z - size.z / 2f)
    val max: Point3 get() = Point3(centre.x + size.x / 2f, centre.y + size.y / 2f, centre.z + size.z / 2f)
}

/**
 * Invisible tap targets for what the room's model bakes in without colliders: each exhibit's support (bench riser,
 * plinth, pedestal, lectern, platform slot) with its brass plaque, and the lit ring on the entrance door, which is an
 * exit. Support boxes are in the anchor's local frame (origin on the plinth top, +Y up, front +Z toward the bay
 * viewpoint, the same frame as [Anchor] and the room build's `box_rr`); the exit is in the room frame. Every dimension
 * is transcribed from the room's authoring source (art/room/room_layout.py and build_room.py), not exported data, so a
 * rebuilt room must re-check them; the host test cross-checks them against the plaques exported in room-layout.json.
 */
object ArchiveTargets {
    /** Bench half depth, radially: BENCH_DEPTH 0.70 (room_layout.py:25). */
    private const val BENCH_HALF_DEPTH = 0.35f

    /** Riser side by bench bay (room_layout.py:48, :54); each riser stands on the bench under its anchor (build_room.py:585). */
    private val RISER = mapOf("daily" to 0.42f, "connected" to 0.36f)

    /** Plinth side (PLINTH_SIZE, room_layout.py:31), from the platform up to the plinth top (build_room.py:614-616). */
    private val PLINTH = mapOf("rocket" to 0.36f, "router" to 0.46f, "gascan" to 0.42f, "doorbell" to 0.40f)

    /** The doorbell's anchor is 0.12 m further out than its plinth (RADIAL_OFFSET, room_layout.py:66), so the plinth sits in front. */
    private val PLINTH_FORWARD = mapOf("doorbell" to 0.12f)

    /** Platform half depth: PLATFORM_DEPTH 0.90 (room_layout.py:27). */
    private const val PLATFORM_HALF_DEPTH = 0.45f

    /** The vacuum's round pedestal, radius 0.26 (build_room.py:597-599), boxed. */
    private const val PEDESTAL_RADIUS = 0.26f

    /** Lecterns (vacuum, scooter): centred 0.33 m in front, 0.06 x 0.03 m half size, up to LECTERN_TOP_Y + 0.012 (build_room.py:623-627). */
    private const val LECTERN_INSET = 0.33f
    private const val LECTERN_HALF_DEPTH = 0.03f
    private const val LECTERN_TOP = 0.492f

    /** The scooter's slot is 1.50 m (room_layout.py:61); a 0.75 m half slot would reach the doorbell plinth's corner. */
    private const val SCOOTER_HALF_SLOT = 0.65f

    /** Plaques: 0.20 m wide plates hanging 0.72 m down to about 0.65 m (PLAQUE, room_layout.py:28), boxed with a margin. */
    private const val PLAQUE_HALF_WIDTH = 0.11f
    private const val PLAQUE_BOTTOM = 0.62f
    private const val PLAQUE_TOP = 0.73f

    /** How far a target reaches in front of a support face, so the tilted plate (about 0.04 m deep) is inside it. */
    private const val PLAQUE_REACH = 0.05f

    /** The doorbell's wall-mount backboard: 0.22 x 0.40 m, 0.03 m thick, just behind the anchor (room_layout.py:67; build_room.py:618-621). */
    private const val BOARD_HALF_WIDTH = 0.11f
    private const val BOARD_HEIGHT = 0.40f

    /** The exit target's side: the lit ring is 0.14 m across (r 0.07, build_room.py:471-474); the patch adds a margin. */
    const val RING_PATCH = 0.24f

    /** How far below the ring the exit caption is centred, on the same door leaf. */
    const val LABEL_DROP = 0.24f

    /** The exit caption's clean area on the door leaf (metres). */
    const val LABEL_WIDTH = 0.6f
    const val LABEL_HEIGHT = 0.15f

    private fun box(x0: Float, x1: Float, y0: Float, y1: Float, z0: Float, z1: Float) =
        TargetBox(Point3((x0 + x1) / 2f, (y0 + y1) / 2f, (z0 + z1) / 2f), Point3(x1 - x0, y1 - y0, z1 - z0))

    /**
     * The support under [anchor] and its plaque, in the anchor's local frame, from the floor (y = −plinth top) up to the
     * plinth top (y = 0); empty for an exhibit the room build gives no support.
     */
    fun supportBoxes(anchor: Anchor): List<TargetBox> {
        val top = anchor.position.y
        // Bench bays: the riser and the bench section below it, out to the plaque on the bench's front face.
        RISER[anchor.bay]?.let { side ->
            return listOf(box(-side / 2f, side / 2f, -top, 0f, -BENCH_HALF_DEPTH, BENCH_HALF_DEPTH + PLAQUE_REACH))
        }
        // Plinths: the block, a thin box for the plaque on its front face (a deeper block would touch its neighbours),
        // and the doorbell's backboard above its plinth.
        PLINTH[anchor.id]?.let { side ->
            val c = PLINTH_FORWARD[anchor.id] ?: 0f
            val front = c + side / 2f
            val out = mutableListOf(
                box(-side / 2f, side / 2f, -top, 0f, c - side / 2f, front),
                box(-PLAQUE_HALF_WIDTH, PLAQUE_HALF_WIDTH, PLAQUE_BOTTOM - top, PLAQUE_TOP - top, front - 0.01f, front + PLAQUE_REACH),
            )
            if (anchor.id == "doorbell") out += box(-BOARD_HALF_WIDTH, BOARD_HALF_WIDTH, -0.01f, BOARD_HEIGHT, -0.05f, -0.01f)
            return out
        }
        // Low supports: the lectern with its plaque at the platform's front edge.
        val lectern = box(
            -PLAQUE_HALF_WIDTH,
            PLAQUE_HALF_WIDTH,
            -top,
            LECTERN_TOP - top,
            LECTERN_INSET - LECTERN_HALF_DEPTH,
            LECTERN_INSET + LECTERN_HALF_DEPTH + PLAQUE_REACH,
        )
        return when (anchor.id) {
            "vacuum" -> listOf(box(-PEDESTAL_RADIUS, PEDESTAL_RADIUS, -top, 0f, -PEDESTAL_RADIUS, PEDESTAL_RADIUS), lectern)
            "scooter" -> listOf(box(-SCOOTER_HALF_SLOT, SCOOTER_HALF_SLOT, -top, 0f, -PLATFORM_HALF_DEPTH, PLATFORM_HALF_DEPTH), lectern)
            else -> emptyList()
        }
    }

    /**
     * How far in front of [anchor] (along its front, local +Z) its furniture reaches on the floor: the bench's front face
     * (the toe kick under it is recessed) or the platform's front edge, 0.12 m further for the doorbell, whose anchor sits
     * that much further out than the platform's centreline. A floor marker in front of an exhibit must stay beyond it.
     */
    fun floorFront(anchor: Anchor): Float =
        if (RISER.containsKey(anchor.bay)) BENCH_HALF_DEPTH else PLATFORM_HALF_DEPTH + (PLINTH_FORWARD[anchor.id] ?: 0f)

    /**
     * The exit target in the room frame: a [RING_PATCH] square over the lit ring at [marker] (room.entrance.marker), from
     * the ring's face back into the door, so it sits behind the caption's plane.
     */
    fun exitMarkerBox(marker: Point3): TargetBox =
        TargetBox(Point3(marker.x, marker.y, marker.z + 0.02f), Point3(RING_PATCH, RING_PATCH, 0.04f))

    /** The exit caption: [LABEL_DROP] below the ring on the same leaf, 2 cm in front of it, facing into the room (−Z). */
    fun exitLabelPlacard(marker: Point3): Placard = Placard(
        Point3(marker.x, marker.y - LABEL_DROP, marker.z - 0.02f),
        yawDegrees = 180f,
        widthMetres = LABEL_WIDTH,
        heightMetres = LABEL_HEIGHT,
    )
}
