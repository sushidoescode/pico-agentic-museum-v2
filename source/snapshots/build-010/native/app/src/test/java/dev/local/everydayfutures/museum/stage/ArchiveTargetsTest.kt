package dev.local.everydayfutures.museum.stage

import kotlin.math.cos
import kotlin.math.sin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Host checks of the room's invisible tap targets against the exported layout. The anchors and plaques below are copied
 * from assets/room/room-layout.json (generated 2026-09-27); the support dimensions themselves come from the authoring
 * source, so these tests are the cross-check that the two still agree. What the targets do on the device (taps, hover)
 * is the native lane's business.
 */
class ArchiveTargetsTest {
    private val eps = 1e-4f

    private class Exhibit(val anchor: Anchor, val plaque: Point3)

    private fun exhibit(id: String, bay: String, x: Float, y: Float, z: Float, yaw: Float, px: Float, py: Float, pz: Float) =
        Exhibit(Anchor(id, Point3(x, y, z), yaw, bay), Point3(px, py, pz))

    private val room = listOf(
        exhibit("quencher", "daily", -3.6384f, 0.85f, 1.4776f, 117.5f, -3.3091f, 0.6885f, 1.3062f),
        exhibit("coffeepod", "daily", -3.8682f, 1.04f, 0.9088f, 106.5f, -3.5122f, 0.6885f, 0.8034f),
        exhibit("deliverybox", "daily", -3.9853f, 0.88f, 0.3067f, 95.5f, -3.6157f, 0.6885f, 0.2711f),
        exhibit("qrcode", "daily", -3.9853f, 0.95f, -0.3067f, 84.5f, -3.6157f, 0.6885f, -0.2711f),
        exhibit("fidget", "daily", -3.8682f, 1.02f, -0.9088f, 73.5f, -3.5122f, 0.6885f, -0.8034f),
        exhibit("facemask", "daily", -3.6384f, 0.98f, -1.4776f, 62.5f, -3.3091f, 0.6885f, -1.3062f),
        exhibit("smartspeaker", "connected", -1.3101f, 0.9f, -3.0096f, 25f, -1.1532f, 0.6885f, -2.6731f),
        exhibit("smartphone", "connected", -0.8023f, 0.95f, -3.1944f, 15f, -0.7062f, 0.6885f, -2.8357f),
        exhibit("earbuds", "connected", -0.2702f, 1.02f, -3.2882f, 5f, -0.2378f, 0.6885f, -2.9183f),
        exhibit("smartwatch", "connected", 0.2702f, 1.0f, -3.2882f, -5f, 0.2378f, 0.6885f, -2.9183f),
        exhibit("cryptowallet", "connected", 0.8023f, 1.04f, -3.1944f, -15f, 0.7062f, 0.6885f, -2.8357f),
        exhibit("remote", "connected", 1.3101f, 1.03f, -3.0096f, -25f, 1.1532f, 0.6885f, -2.6731f),
        exhibit("vacuum", "domestic", 3.7717f, 0.3f, -1.6519f, -60.931f, 3.4385f, 0.4485f, -1.4667f),
        exhibit("router", "domestic", 3.999f, 0.9f, -1.1518f, -70.199f, 3.7626f, 0.6885f, -1.0666f),
        exhibit("doorbell", "domestic", 4.2457f, 0.95f, -0.7196f, -78.204f, 3.9116f, 0.6885f, -0.6498f),
        exhibit("scooter", "domestic", 4.1889f, 0.1f, 0.2747f, -94.634f, 3.8088f, 0.4485f, 0.2439f),
        exhibit("gascan", "domestic", 3.9637f, 0.75f, 1.2453f, -111.486f, 3.7485f, 0.6885f, 1.1606f),
        exhibit("rocket", "domestic", 3.7471f, 0.9f, 1.6955f, -119.912f, 3.5726f, 0.6885f, 1.5951f),
    )

    /** room.entrance.marker in room-layout.json. */
    private val marker = Point3(0.55f, 1.45f, 5.68f)

    /** A room point in an anchor's local frame (the inverse of the build's rotation by the anchor's yaw). */
    private fun local(p: Point3, a: Anchor): Point3 =
        ArchiveGeometry.rotateY(Point3(p.x - a.position.x, p.y - a.position.y, p.z - a.position.z), -a.yawDegrees)

    private fun TargetBox.contains(p: Point3): Boolean =
        p.x in min.x..max.x && p.y in min.y..max.y && p.z in min.z..max.z

    /** A box's four floor corners in the room frame (x, z), in order round the rectangle. */
    private fun footprint(box: TargetBox, a: Anchor): List<Pair<Float, Float>> =
        listOf(box.min.x to box.min.z, box.max.x to box.min.z, box.max.x to box.max.z, box.min.x to box.max.z).map { (x, z) ->
            val r = ArchiveGeometry.rotateY(Point3(x, 0f, z), a.yawDegrees)
            (a.position.x + r.x) to (a.position.z + r.z)
        }

    /** Separating-axis test for two convex quadrilaterals on the floor: true when some edge normal separates them. */
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

    @Test
    fun everyPlaqueSitsInsideItsExhibitsTarget() {
        // The plate: 0.20 x 0.075 m, 6 mm thick, its face tilted 25 degrees up (room_layout.py:28); the exported point is
        // the centre of its front face. Every corner of both faces must be inside one of the exhibit's boxes.
        val p = Math.toRadians(25.0)
        val upY = (cos(p) * 0.0375).toFloat()
        val upZ = -(sin(p) * 0.0375).toFloat()
        for (e in room) {
            val c = local(e.plaque, e.anchor)
            val corners = listOf(-1f, 1f).flatMap { sx ->
                listOf(-1f, 1f).flatMap { sy ->
                    listOf(0f, 0.006f).map { t ->
                        Point3(c.x + sx * 0.1f, c.y + sy * upY - (sin(p) * t).toFloat(), c.z + sy * upZ - (cos(p) * t).toFloat())
                    }
                }
            }
            val boxes = ArchiveTargets.supportBoxes(e.anchor)
            assertTrue(e.anchor.id, boxes.any { box -> corners.all { box.contains(it) } })
        }
    }

    @Test
    fun theDoorbellBackboardIsATargetAboveItsPlinth() {
        val doorbell = room.first { it.anchor.id == "doorbell" }.anchor
        val board = ArchiveTargets.supportBoxes(doorbell).single { it.max.z < 0f && it.max.y > 0.1f }
        // The authored board: 0.22 m wide, centred 0.029 m behind the anchor, 0.03 m thick, from 1 cm under the plinth top
        // to 0.40 m above it (build_room.py:618-621).
        assertTrue(board.min.x <= -0.11f + eps && board.max.x >= 0.11f - eps)
        assertTrue(board.min.y <= -0.01f + eps && board.max.y >= 0.40f - eps)
        assertTrue(board.min.z <= -0.044f && board.max.z >= -0.014f)
        // And the plinth block is in front of the anchor, where RADIAL_OFFSET put it.
        val plinth = ArchiveTargets.supportBoxes(doorbell).first()
        assertEquals(0.12f, plinth.centre.z, eps)
        assertEquals(0.40f, plinth.size.x, eps)
    }

    @Test
    fun targetsNeverGoBelowTheFloor() {
        for (e in room) {
            val top = e.anchor.position.y
            val boxes = ArchiveTargets.supportBoxes(e.anchor)
            assertTrue("${e.anchor.id} has a support", boxes.isNotEmpty())
            boxes.forEach { box -> assertTrue(e.anchor.id, box.min.y >= -top - eps) }
            // Something reaches down to the floor, so the support is a target over its full height.
            assertEquals(e.anchor.id, -top, boxes.minOf { it.min.y }, eps)
        }
    }

    @Test
    fun neighbouringTargetsDoNotOverlap() {
        // The domestic bay's plinths stand 1-2 cm apart, and a scooter slot box of the full 0.75 m half width would reach
        // the doorbell plinth's corner; the benches' risers are well apart. No two exhibits may share a target volume.
        for (bay in listOf("daily", "connected", "domestic")) {
            val inBay = room.filter { it.anchor.bay == bay }.map { it.anchor }
            for (i in inBay.indices) for (j in i + 1 until inBay.size) {
                val a = inBay[i]
                val b = inBay[j]
                for (boxA in ArchiveTargets.supportBoxes(a)) for (boxB in ArchiveTargets.supportBoxes(b)) {
                    assertTrue("${a.id} / ${b.id}", separated(footprint(boxA, a), footprint(boxB, b)))
                }
            }
        }
    }

    @Test
    fun aFullWidthScooterSlotWouldOverlapItsNeighbours() {
        // Guards the separating-axis check itself: the slot at its authored 1.50 m does touch the doorbell plinth.
        val scooter = room.first { it.anchor.id == "scooter" }.anchor
        val doorbell = room.first { it.anchor.id == "doorbell" }.anchor
        val fullSlot = TargetBox(Point3(0f, -0.05f, 0f), Point3(1.50f, 0.1f, 0.90f))
        val plinth = ArchiveTargets.supportBoxes(doorbell).first()
        assertFalse(separated(footprint(fullSlot, scooter), footprint(plinth, doorbell)))
    }

    @Test
    fun theExitPatchCoversTheLitRingAndNotItsCaption() {
        val patch = ArchiveTargets.exitMarkerBox(marker)
        // The ring (outer radius 0.07, build_room.py:471-474) on the door, facing into the room.
        for (k in 0 until 16) {
            val a = 2.0 * Math.PI * k / 16
            val x = marker.x + (0.07 * cos(a)).toFloat()
            val y = marker.y + (0.07 * sin(a)).toFloat()
            assertTrue(x in patch.min.x..patch.max.x && y in patch.min.y..patch.max.y)
        }
        assertEquals(marker.z, patch.min.z, eps) // starts at the ring's face, reaching back into the door
        // On the left leaf as seen from the room (x 0.03..0.98, build_room.py:466-470), clear of the seam.
        assertTrue(patch.min.x > 0.03f && patch.max.x < 0.98f)

        val caption = ArchiveTargets.exitLabelPlacard(marker)
        val at = ArchiveGeometry.labelPosition(caption)
        assertEquals(180f, caption.yawDegrees, eps) // its front faces −Z, into the room
        // labelScale never fills more than the placard, so the placard bounds the caption.
        val width = checkNotNull(caption.widthMetres)
        val height = checkNotNull(caption.heightMetres)
        val scale = ArchiveGeometry.labelScale(caption, 360f, 90f, 1f)
        assertTrue(scale * 360f * ArchiveGeometry.METRES_PER_DP <= width + eps)
        assertTrue(scale * 90f * ArchiveGeometry.METRES_PER_DP <= height + eps)
        assertTrue("caption below the patch", at.y + height / 2f < patch.min.y)
        assertTrue("caption in front of the patch", at.z < patch.min.z)
        assertTrue("caption on the same leaf", at.x - width / 2f > 0.03f && at.x + width / 2f < 0.98f)
    }

    @Test
    fun theLayoutKeepsAnExitMarkerOnlyWhenGiven() {
        val plain = RoomLayout(listOf("room/earth_archive.glb"), emptyList(), mapOf(RoomLayout.ARRIVAL to StandPoint(Point3(0f, 0f, 0f), 0f)))
        assertNull(plain.exitMarker)
        assertEquals(marker, plain.copy(exitMarker = marker).exitMarker)
    }
}
