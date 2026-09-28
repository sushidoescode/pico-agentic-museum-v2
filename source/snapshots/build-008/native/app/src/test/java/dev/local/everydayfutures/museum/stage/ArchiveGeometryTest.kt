package dev.local.everydayfutures.museum.stage

import org.junit.Assert.assertEquals
import org.junit.Test

class ArchiveGeometryTest {
    private val eps = 1e-4f

    private fun assertPoint(expected: Point3, actual: Point3) {
        assertEquals("x", expected.x, actual.x, eps)
        assertEquals("y", expected.y, actual.y, eps)
        assertEquals("z", expected.z, actual.z, eps)
    }

    @Test
    fun aQuarterTurnLeftTakesMinusZToMinusX() {
        assertPoint(Point3(-1f, 0f, 0f), ArchiveGeometry.rotateY(Point3(0f, 0f, -1f), 90f))
        assertPoint(Point3(1f, 0f, 0f), ArchiveGeometry.rotateY(Point3(0f, 0f, 1f), 90f))
    }

    @Test
    fun arrivalIsTheIdentity() {
        val pose = ArchiveGeometry.worldPoseFor(StandPoint(Point3(0f, 0f, 0f), 0f))
        assertPoint(Point3(0f, 0f, 0f), pose.position)
        assertEquals(0f, pose.yawDegrees, eps)
    }

    @Test
    fun theLeftBayComesToTheFrontWhenStandingBeforeItFacingMinusX() {
        // Daily-rituals viewpoint: 2.2 m left of the arrival point, facing the left wall (−X).
        val stand = StandPoint(Point3(-2.2f, 0f, 0f), 90f)
        // A plinth-top point on the left bench, 1.8 m further left, appears straight ahead at the same height.
        assertPoint(Point3(0f, 1f, -1.8f), ArchiveGeometry.apparent(Point3(-4f, 1f, 0f), stand))
        // Facing −X, the entrance side (+Z) is on the visitor's left and the aperture side (−Z) on their right, so the
        // bench, laid out from the entrance toward the aperture, reads left to right in accession order.
        assertEquals(-1f, ArchiveGeometry.apparent(Point3(-4f, 1f, 1f), stand).x, eps)
        assertEquals(1f, ArchiveGeometry.apparent(Point3(-4f, 1f, -1f), stand).x, eps)
    }

    @Test
    fun worldPoseAndApparentAgree() {
        val stand = StandPoint(Point3(1.3f, 0f, -0.7f), -65f)
        val q = Point3(3.1f, 0.95f, 1.2f)
        val pose = ArchiveGeometry.worldPoseFor(stand)
        val viaPose = ArchiveGeometry.rotateY(q, pose.yawDegrees).let { Point3(it.x + pose.position.x, it.y + pose.position.y, it.z + pose.position.z) }
        assertPoint(ArchiveGeometry.apparent(q, stand), viaPose)
    }

    @Test
    fun aCloseLookStandsInFrontOfTheExhibitAndFacesIt() {
        // Left-bench anchor, front toward +X (yaw +90).
        val anchor = Anchor("coffeepod", Point3(-4f, 1f, 1.2f), 90f, bay = "daily")
        val stand = ArchiveGeometry.closeLook(anchor)
        assertPoint(Point3(-4f + ArchiveGeometry.CLOSE_LOOK_DISTANCE, 0f, 1.2f), stand.position)
        val seen = ArchiveGeometry.apparent(anchor.position, stand)
        assertEquals("straight ahead", 0f, seen.x, eps)
        assertEquals("in front", -ArchiveGeometry.CLOSE_LOOK_DISTANCE, seen.z, eps)
        // Rear-bench anchor facing +Z is looked at from +Z, facing −Z.
        val rear = ArchiveGeometry.closeLook(Anchor("smartphone", Point3(0.5f, 1f, -3.3f), 0f, bay = "connected"))
        assertPoint(Point3(0.5f, 0f, -3.3f + ArchiveGeometry.CLOSE_LOOK_DISTANCE), rear.position)
        assertEquals(0f, rear.yawDegrees, eps)
    }

    @Test
    fun degreesFoldIntoOneTurn() {
        assertEquals(180f, ArchiveGeometry.normaliseDegrees(-180f), eps)
        assertEquals(-90f, ArchiveGeometry.normaliseDegrees(270f), eps)
        assertEquals(10f, ArchiveGeometry.normaliseDegrees(730f), eps)
    }

    @Test
    fun aLabelFillsNineTenthsOfItsPlacardAndNeverOverflowsItsHeight() {
        val plaque = Placard(Point3(0f, 0.7f, 0f), 0f, 25f, widthMetres = 0.20f, heightMetres = 0.075f)
        // 300 dp at 0.0008 m/dp is 0.24 m at scale 1, so 0.18 m needs 0.75.
        assertEquals(0.75f, ArchiveGeometry.labelScale(plaque, contentWidthDp = 300f, fallback = 0.5f), eps)
        // A tall label is limited by the plate's height instead: 0.9 x 0.075 / (150 dp x 0.0008) = 0.5625.
        assertEquals(0.5625f, ArchiveGeometry.labelScale(plaque, 300f, contentHeightDp = 150f, fallback = 0.5f), eps)
        // No size from the room: the fallback.
        assertEquals(0.5f, ArchiveGeometry.labelScale(Placard(Point3(0f, 0f, 0f), 0f), 300f, fallback = 0.5f), eps)
    }

    @Test
    fun aLabelStandsOffAlongItsFacesNormal() {
        // A wall sign facing +X (yaw 90) moves 4 mm toward +X only.
        val sign = ArchiveGeometry.labelPosition(Placard(Point3(-4.96f, 2.05f, 0f), 90f))
        assertPoint(Point3(-4.956f, 2.05f, 0f), sign)
        // A plaque facing +Z tilted 25 degrees up moves toward +Z and up.
        val plaque = ArchiveGeometry.labelPosition(Placard(Point3(0f, 0.7f, 0f), 0f, 25f))
        assertEquals(0.004f * kotlin.math.cos(Math.toRadians(25.0)).toFloat(), plaque.z, 1e-6f)
        assertEquals(0.7f + 0.004f * kotlin.math.sin(Math.toRadians(25.0)).toFloat(), plaque.y, 1e-6f)
        assertEquals(0f, plaque.x, 1e-6f)
    }

    @Test
    fun aLowExhibitIsLookedAtFromFurtherAwaySoItsBaseStaysInView() {
        // A plinth-top exhibit keeps the nearest distance.
        assertEquals(1.4f, ArchiveGeometry.closeLookDistance(Anchor("coffeepod", Point3(0f, 1.04f, 0f), 0f, "daily")), eps)
        // A floor-platform exhibit (base 0.10 m) stands back until its base is 30 degrees below a 1.7 m eye: 1.6 / tan 30.
        val scooter = Anchor("scooter", Point3(3f, 0.10f, 0f), -90f, "domestic")
        val d = ArchiveGeometry.closeLookDistance(scooter)
        assertEquals(1.6f / kotlin.math.tan(Math.toRadians(30.0)).toFloat(), d, 1e-3f)
        val stand = ArchiveGeometry.closeLook(scooter)
        assertEquals(-d, ArchiveGeometry.apparent(scooter.position, stand).z, 1e-3f)
    }
}
