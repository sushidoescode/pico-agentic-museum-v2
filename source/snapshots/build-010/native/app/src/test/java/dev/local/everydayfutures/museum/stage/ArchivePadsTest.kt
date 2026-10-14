package dev.local.everydayfutures.museum.stage

import kotlin.math.abs
import kotlin.math.hypot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Host checks of the floor circles against the exported layout. The anchors and bay arcs below are copied from
 * assets/room/room-layout.json (generated 2026-09-27); the furniture depths come from the authoring source through
 * [ArchiveTargets]. What a circle looks like, and whether a gaze or hand-ray pinch reaches it, is the native lane's
 * business.
 */
class ArchivePadsTest {
    private val eps = 1e-4f

    private fun anchor(id: String, bay: String, x: Float, y: Float, z: Float, yaw: Float) = Anchor(id, Point3(x, y, z), yaw, bay)

    private val room = listOf(
        anchor("quencher", "daily", -3.6384f, 0.85f, 1.4776f, 117.5f),
        anchor("coffeepod", "daily", -3.8682f, 1.04f, 0.9088f, 106.5f),
        anchor("deliverybox", "daily", -3.9853f, 0.88f, 0.3067f, 95.5f),
        anchor("qrcode", "daily", -3.9853f, 0.95f, -0.3067f, 84.5f),
        anchor("fidget", "daily", -3.8682f, 1.02f, -0.9088f, 73.5f),
        anchor("facemask", "daily", -3.6384f, 0.98f, -1.4776f, 62.5f),
        anchor("smartspeaker", "connected", -1.3101f, 0.9f, -3.0096f, 25f),
        anchor("smartphone", "connected", -0.8023f, 0.95f, -3.1944f, 15f),
        anchor("earbuds", "connected", -0.2702f, 1.02f, -3.2882f, 5f),
        anchor("smartwatch", "connected", 0.2702f, 1.0f, -3.2882f, -5f),
        anchor("cryptowallet", "connected", 0.8023f, 1.04f, -3.1944f, -15f),
        anchor("remote", "connected", 1.3101f, 1.03f, -3.0096f, -25f),
        anchor("vacuum", "domestic", 3.7717f, 0.3f, -1.6519f, -60.931f),
        anchor("router", "domestic", 3.999f, 0.9f, -1.1518f, -70.199f),
        anchor("doorbell", "domestic", 4.2457f, 0.95f, -0.7196f, -78.204f),
        anchor("scooter", "domestic", 4.1889f, 0.1f, 0.2747f, -94.634f),
        anchor("gascan", "domestic", 3.9637f, 0.75f, 1.2453f, -111.486f),
        anchor("rocket", "domestic", 3.7471f, 0.9f, 1.6955f, -119.912f),
    )

    /** bays[].arcCentre (x, z) and arcRadiusM: the furniture's centreline, round each bay's viewpoint. */
    private val arcs = mapOf(
        "daily" to Triple(-0.8f, 0f, 3.2f),
        "connected" to Triple(0f, -0.2f, 3.1f),
        "domestic" to Triple(0.8f, 0f, 3.4f),
    )

    /** Half the furniture's depth across the arc: BENCH_DEPTH 0.70 and PLATFORM_DEPTH 0.90 (room_layout.py:25, :27). */
    private val halfDepth = mapOf("daily" to 0.35f, "connected" to 0.35f, "domestic" to 0.45f)

    /** The floor corners of a box in some frame placed at [at] and turned by [yaw], in the room frame (x, z). */
    private fun footprint(box: TargetBox, at: Point3, yaw: Float): List<Pair<Float, Float>> =
        listOf(box.min.x to box.min.z, box.max.x to box.min.z, box.max.x to box.max.z, box.min.x to box.max.z).map { (x, z) ->
            val r = ArchiveGeometry.rotateY(Point3(x, 0f, z), yaw)
            (at.x + r.x) to (at.z + r.z)
        }

    private fun collider(a: Anchor) = footprint(ArchivePads.COLLIDER, ArchivePads.centre(a), a.yawDegrees)

    /** Separating-axis test for two convex quadrilaterals on the floor (as in ArchiveTargetsTest). */
    private fun separated(p: List<Pair<Float, Float>>, q: List<Pair<Float, Float>>): Boolean =
        listOf(p, q).any { poly ->
            poly.indices.any { i ->
                val (x1, z1) = poly[i]
                val (x2, z2) = poly[(i + 1) % poly.size]
                val nx = z2 - z1
                val nz = -(x2 - x1)
                val a = p.map { (x, z) -> nx * x + nz * z }
                val b = q.map { (x, z) -> nx * x + nz * z }
                a.max() < b.min() || b.max() < a.min()
            }
        }

    private fun gap(a: Point3, b: Point3): Float = hypot(a.x - b.x, a.z - b.z)

    @Test
    fun eachCircleLiesOnTheFloorStraightInFrontOfItsExhibit() {
        for (a in room) {
            val c = ArchivePads.centre(a)
            assertEquals(a.id, 0f, c.y, 0f)
            // In the anchor's own frame the centre is on its front axis (+Z), beyond the furniture by the clearance.
            val local = ArchiveGeometry.rotateY(Point3(c.x - a.position.x, 0f, c.z - a.position.z), -a.yawDegrees)
            assertEquals(a.id, 0f, local.x, eps)
            assertEquals(a.id, ArchiveTargets.floorFront(a) + ArchivePads.CLEARANCE + ArchivePads.RADIUS, local.z, eps)
            // Between the furniture and the close look it steps to, which stands further out.
            val stand = ArchiveGeometry.closeLook(a).position
            assertTrue(a.id, gap(c, stand) + ArchivePads.RADIUS < gap(stand, a.position))
        }
        assertEquals(0.35f, ArchiveTargets.floorFront(room.first { it.id == "earbuds" }), eps)
        assertEquals(0.45f, ArchiveTargets.floorFront(room.first { it.id == "scooter" }), eps)
        assertEquals(0.57f, ArchiveTargets.floorFront(room.first { it.id == "doorbell" }), eps)
    }

    @Test
    fun circlesStayInFrontOfTheBenchesAndThePlatform() {
        for (a in room) {
            val (cx, cz, radius) = arcs.getValue(a.bay)
            val c = ArchivePads.centre(a)
            // Across the whole bench or platform, not only under its own exhibit: its outer edge is nearer the viewpoint
            // than the furniture's front by the clearance.
            val front = radius - halfDepth.getValue(a.bay)
            assertTrue(a.id, hypot(c.x - cx, c.z - cz) + ArchivePads.RADIUS <= front - ArchivePads.CLEARANCE + eps)
            // And its collider touches no exhibit's support target anywhere in the room.
            for (other in room) {
                for (box in ArchiveTargets.supportBoxes(other)) {
                    assertTrue("${a.id} / ${other.id}", separated(collider(a), footprint(box, other.position, other.yawDegrees)))
                }
            }
        }
    }

    @Test
    fun neighbouringCirclesAndTheirCollidersKeepApart() {
        val minimum = 2f * ArchivePads.RADIUS + 0.05f
        var closest = Float.MAX_VALUE
        for (i in room.indices) for (j in i + 1 until room.size) {
            val a = room[i]
            val b = room[j]
            val d = gap(ArchivePads.centre(a), ArchivePads.centre(b))
            closest = minOf(closest, d)
            assertTrue("${a.id} / ${b.id} $d", d >= minimum)
            assertTrue("${a.id} / ${b.id}", separated(collider(a), collider(b)))
        }
        // The router and the doorbell are the closest pair (0.38 m): what limits the radius.
        assertEquals(gap(ArchivePads.centre(room[13]), ArchivePads.centre(room[14])), closest, eps)
    }

    @Test
    fun circlesClearTheArrivalInlaysAndTheWalls() {
        for (a in room) {
            val c = ArchivePads.centre(a)
            // The outer arrival inlay is a 40 mm band at r 1.50 round the origin (build_room.py floor inlays).
            assertTrue(a.id, hypot(c.x, c.z) - ArchivePads.RADIUS > 1.52f + 0.5f)
            // Inside the room's floor (x -5..5, z -4.5..4.5, room_layout.py ROOM).
            assertTrue(a.id, abs(c.x) + ArchivePads.RADIUS < 5f && abs(c.z) + ArchivePads.RADIUS < 4.5f)
        }
    }

    @Test
    fun theCloseLookStandPointsAreTooCrowdedForCircles() {
        // Why the circles are not drawn where the close look stands: there, neighbours in the Connected bay stand 0.30 m
        // apart (less than one circle's width) and the scooter's lies across the 1.5 m arrival inlay.
        val connected = room.filter { it.bay == "connected" }.map { ArchiveGeometry.closeLook(it).position }
        assertTrue(connected.zipWithNext().any { (p, q) -> gap(p, q) < 2f * ArchivePads.RADIUS })
        val scooter = ArchiveGeometry.closeLook(room.first { it.id == "scooter" }).position
        assertTrue(abs(hypot(scooter.x, scooter.z) - 1.5f) < ArchivePads.RADIUS)
    }

    @Test
    fun theCircleIsFlatThinAndJustAboveTheFloor() {
        val tube = (ArchivePads.RADIUS - ArchivePads.RING_INNER) / 2f
        assertTrue(ArchivePads.RING_LIFT - tube > 0f) // the ring's underside
        assertTrue(ArchivePads.RING_LIFT + tube < 0.02f) // and its top: under 2 cm
        assertTrue(ArchivePads.DISC_LIFT - ArchivePads.DISC_HEIGHT / 2f > 0f)
        assertTrue(ArchivePads.DISC_RADIUS <= ArchivePads.RING_INNER)
        val box = ArchivePads.COLLIDER
        assertTrue(box.min.y > 0f && box.max.y <= 0.05f)
        assertTrue(box.max.x >= ArchivePads.RADIUS - eps && box.max.z >= ArchivePads.RADIUS - eps)
        assertEquals(0f, box.centre.x, 0f)
        assertEquals(0f, box.centre.z, 0f)
        assertTrue(ArchivePads.DISC_OPACITY < ArchivePads.RING_OPACITY)
    }

    @Test
    fun onlyTheFocusedExhibitsCircleIsHidden() {
        assertTrue(ArchivePads.shown("rocket", null))
        assertFalse(ArchivePads.shown("rocket", "rocket"))
        assertTrue(ArchivePads.shown("rocket", "gascan"))
        assertEquals(17, room.count { ArchivePads.shown(it.id, "gascan") })
    }
}
