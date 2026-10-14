package dev.local.everydayfutures.museum.stage

import kotlin.math.atan2
import kotlin.math.hypot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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

    @Test
    fun headingFollowsForwardAndFallsBackToRight() {
        val right = Point3(1f, 0f, 0f)
        assertEquals(0f, ArchiveGeometry.headingDegrees(Point3(0f, 0f, -1f), right)!!, eps)
        assertEquals(90f, ArchiveGeometry.headingDegrees(Point3(-1f, 0f, 0f), Point3(0f, 0f, -1f))!!, eps)
        assertEquals(-90f, ArchiveGeometry.headingDegrees(Point3(1f, 0f, 0f), Point3(0f, 0f, 1f))!!, eps)
        assertEquals(180f, ArchiveGeometry.headingDegrees(Point3(0f, 0f, 1f), Point3(-1f, 0f, 0f))!!, eps)
        // Pitched well down, still facing ahead.
        assertEquals(0f, ArchiveGeometry.headingDegrees(Point3(0f, -0.9f, -0.436f), right)!!, eps)
        // Looking straight down while turned a quarter left: the right axis gives the heading.
        assertEquals(90f, ArchiveGeometry.headingDegrees(Point3(0f, -1f, 0f), Point3(0f, 0f, -1f))!!, eps)
        // Both axes vertical, or not numbers: no heading.
        assertNull(ArchiveGeometry.headingDegrees(Point3(0f, -1f, 0f), Point3(0f, 1f, 0f)))
        assertNull(ArchiveGeometry.headingDegrees(Point3(Float.NaN, 0f, -1f), Point3(Float.NaN, 0f, 0f)))
    }

    @Test
    fun headFrameDropsBadReadings() {
        val forward = Point3(0f, 0f, -1f)
        val right = Point3(1f, 0f, 0f)
        assertNull(ArchiveGeometry.headFrame(Point3(Float.NaN, 1.6f, 0f), forward, right))
        assertNull(ArchiveGeometry.headFrame(Point3(0f, Float.POSITIVE_INFINITY, 0f), forward, right))
        assertNull(ArchiveGeometry.headFrame(Point3(0f, 1.6f, 0f), Point3(0f, 1f, 0f), Point3(0f, -1f, 0f)))
        val frame = ArchiveGeometry.headFrame(Point3(0.4f, 1.65f, -0.3f), forward, right)!!
        assertPoint(Point3(0.4f, 0f, -0.3f), frame.floor)
        assertEquals(1.65f, frame.eyeHeight, eps)
        assertEquals(0f, frame.yawDegrees, eps)
    }

    @Test
    fun withoutAHeadFrameTheRoomPoseIsUnchanged() {
        val q = Point3(3.1f, 0.95f, 1.2f)
        for (stand in listOf(
            StandPoint(Point3(0f, 0f, 0f), 0f),
            StandPoint(Point3(-2.2f, 0f, 0f), 90f),
            StandPoint(Point3(1.3f, 0f, -0.7f), -65f),
            ArchiveGeometry.closeLook(Anchor("remote", Point3(1.31f, 1.03f, -3.01f), -25f, "connected")),
        )) {
            assertEquals(ArchiveGeometry.worldPoseFor(stand), ArchiveGeometry.worldPoseFor(stand, null))
            assertEquals(ArchiveGeometry.apparent(q, stand), ArchiveGeometry.apparent(q, stand, null))
        }
        // A frame at the Stage origin facing −Z is the same pose.
        val stand = StandPoint(Point3(1.3f, 0f, -0.7f), -65f)
        val origin = HeadFrame(Point3(0f, 0f, 0f), 0f, ArchiveGeometry.NOMINAL_EYE_HEIGHT)
        val pose = ArchiveGeometry.worldPoseFor(stand, origin)
        assertPoint(ArchiveGeometry.worldPoseFor(stand).position, pose.position)
        assertEquals(ArchiveGeometry.worldPoseFor(stand).yawDegrees, pose.yawDegrees, eps)
    }

    @Test
    fun aTurnedVisitorArrivesFacingTheExhibit() {
        val remote = Anchor("remote", Point3(1.31f, 1.03f, -3.01f), -25f, "connected")
        val stand = ArchiveGeometry.closeLook(remote)
        val d = ArchiveGeometry.closeLookDistance(remote)
        for (yaw in listOf(-90f, -30f, 0f, 45f, 90f, 180f)) {
            val frame = HeadFrame(Point3(0.4f, 0f, -0.3f), yaw, 1.7f)
            val seen = ArchiveGeometry.apparent(remote.position, stand, frame)
            // In the head's own frame (its floor point, its heading): straight ahead at the close-look distance.
            val ahead = ArchiveGeometry.rotateY(Point3(seen.x - frame.floor.x, seen.y, seen.z - frame.floor.z), -yaw)
            assertEquals("x at $yaw", 0f, ahead.x, 1e-3f)
            assertEquals("z at $yaw", -d, ahead.z, 1e-3f)
            assertEquals("y at $yaw", remote.position.y, ahead.y, 1e-3f)
        }
    }

    @Test
    fun worldPoseAndApparentAgreeWithAHeadFrame() {
        val stand = StandPoint(Point3(1.3f, 0f, -0.7f), -65f)
        val q = Point3(3.1f, 0.95f, 1.2f)
        for (frame in listOf(HeadFrame(Point3(0.4f, 0f, -0.3f), 72f, 1.6f), HeadFrame(Point3(-0.2f, 0f, 0.5f), -140f, 1.8f))) {
            val pose = ArchiveGeometry.worldPoseFor(stand, frame)
            val viaPose = ArchiveGeometry.rotateY(q, pose.yawDegrees).let { Point3(it.x + pose.position.x, it.y + pose.position.y, it.z + pose.position.z) }
            assertPoint(ArchiveGeometry.apparent(q, stand, frame), viaPose)
            // The stand itself lands under the visitor's head.
            val feet = ArchiveGeometry.rotateY(stand.position, pose.yawDegrees).let { Point3(it.x + pose.position.x, it.y + pose.position.y, it.z + pose.position.z) }
            assertPoint(frame.floor, feet)
        }
    }

    @Test
    fun thePanelKeepsItsPlaceBesideTheVisitorsHeading() {
        val still = ArchiveGeometry.panelPose(null)
        assertEquals(WorldPose(ArchiveGeometry.PANEL_TOP, ArchiveGeometry.PANEL_YAW), still)
        val frame = HeadFrame(Point3(1f, 0f, 2f), 90f, 1.2f)
        val pose = ArchiveGeometry.panelPose(frame)
        val off = ArchiveGeometry.rotateY(Point3(ArchiveGeometry.PANEL_TOP.x, 0f, ArchiveGeometry.PANEL_TOP.z), 90f)
        assertPoint(Point3(1f + off.x, 1.40f, 2f + off.z), pose.position)
        assertEquals(90f + ArchiveGeometry.PANEL_YAW, pose.yawDegrees, eps)
        // Its top follows the eye, within bounds.
        assertEquals(ArchiveGeometry.PANEL_TOP_MIN_Y, ArchiveGeometry.panelPose(frame.copy(eyeHeight = 0f)).position.y, eps)
        assertEquals(ArchiveGeometry.PANEL_TOP_MAX_Y, ArchiveGeometry.panelPose(frame.copy(eyeHeight = 3f)).position.y, eps)
        // At the Stage origin facing −Z with the nominal eye, it is exactly where it is without a frame.
        val origin = ArchiveGeometry.panelPose(HeadFrame(Point3(0f, 0f, 0f), 0f, ArchiveGeometry.NOMINAL_EYE_HEIGHT))
        assertPoint(still.position, origin.position)
        assertEquals(still.yawDegrees, origin.yawDegrees, eps)
    }

    @Test
    fun thePanelIsCloserAndStaysAboveTheExhibitLine() {
        val top = ArchiveGeometry.PANEL_TOP
        val distance = hypot(top.x, top.z)
        assertTrue("distance $distance", distance in 1.0f..1.25f)
        // It faces the visitor: its bearing from straight ahead is its own yaw.
        val bearing = Math.toDegrees(atan2(-top.x, -top.z).toDouble()).toFloat()
        assertEquals(ArchiveGeometry.PANEL_YAW, bearing, 1f)
        // Its lower edge at its minimum height stays 4-8 degrees below a nominal eye, as before (above the exhibits).
        val bottom = top.y - ArchiveGeometry.PANEL_MIN_HEIGHT_DP * ArchiveGeometry.METRES_PER_DP * ArchiveGeometry.PANEL_SCALE
        val below = Math.toDegrees(atan2(ArchiveGeometry.NOMINAL_EYE_HEIGHT - bottom, distance).toDouble())
        assertTrue("lower edge $below degrees below the eye", below in 4.0..8.0)
        // At least 1.3x larger in angle than build-008's panel (0.75x at 1.44 m).
        val before = 0.75f / hypot(-0.62f, -1.3f)
        assertTrue(ArchiveGeometry.PANEL_SCALE / distance >= 1.3f * before)
        // It fits the emulator view measured on build-009 (about 34 degrees each side), with both edges inside 33
        // degrees left, and its right edge clears the bay title (about 6 degrees either side of ahead).
        val half = 760f * ArchiveGeometry.METRES_PER_DP * ArchiveGeometry.PANEL_SCALE / 2f
        val yaw = Math.toRadians(ArchiveGeometry.PANEL_YAW.toDouble())
        val dx = (half * Math.cos(yaw)).toFloat()
        val dz = (half * Math.sin(yaw)).toFloat()
        fun bearingOf(x: Float, z: Float) = Math.toDegrees(atan2(-x, -z).toDouble())
        val left = bearingOf(top.x - dx, top.z + dz)
        val right = bearingOf(top.x + dx, top.z - dz)
        assertTrue("left edge $left degrees", left <= 33.0)
        assertTrue("right edge $right degrees", right >= 6.5)
    }

    @Test
    fun aTapArrivalAdoptsItsHeadFrameAndEveryOtherMoveKeepsIt() {
        val arrival = StandPoint(Point3(0f, 0f, 0f), 0f)
        val bay = StandPoint(Point3(-2.2f, 0f, 0f), 90f)
        val coffeepod = StandPoint(Point3(-2.5f, 0f, 0.5f), 106.5f)
        val fidget = StandPoint(Point3(-2.5f, 0f, -0.5f), 73.5f)
        val turned = HeadFrame(Point3(0.3f, 0f, -0.2f), -90f, 1.7f)
        val frames = ArrivalFrames()
        assertNull(frames.arrive(arrival, null)) // the visit's first placement is not a move
        assertNull(frames.frame)
        assertEquals(ArrivalFrameSource.KEPT, frames.arrive(bay, null)) // a panel bay button
        assertNull(frames.frame)
        frames.offer("coffeepod", turned)
        assertEquals(ArrivalFrameSource.HEAD, frames.arrive(coffeepod, "coffeepod")) // an in-room tap
        assertEquals(turned, frames.frame)
        assertEquals(ArrivalFrameSource.KEPT, frames.arrive(bay, null)) // Back to the bay keeps the frame (and the panel)
        assertEquals(ArrivalFrameSource.KEPT, frames.arrive(fidget, "fidget")) // the panel's object buttons too
        assertEquals(turned, frames.frame)
        // A tap with no head reading moves the visitor in the frame they had.
        frames.offer("coffeepod", null)
        assertEquals(ArrivalFrameSource.KEPT_NO_HEAD, frames.arrive(coffeepod, "coffeepod"))
        assertEquals(turned, frames.frame)
        assertEquals("head", ArrivalFrameSource.HEAD.word)
        assertEquals("kept-no-head", ArrivalFrameSource.KEPT_NO_HEAD.word)
    }

    @Test
    fun placingTheSameStandAgainChangesNothing() {
        val bay = StandPoint(Point3(-2.2f, 0f, 0f), 90f)
        val coffeepod = StandPoint(Point3(-2.5f, 0f, 0.5f), 106.5f)
        val turned = HeadFrame(Point3(0.3f, 0f, -0.2f), -90f, 1.7f)
        val frames = ArrivalFrames()
        frames.arrive(bay, null)
        frames.offer("coffeepod", turned)
        assertEquals(ArrivalFrameSource.HEAD, frames.arrive(coffeepod, "coffeepod"))
        // The effect, `update` and every recomposition place it again: no new arrival, no new frame.
        repeat(3) { assertNull(frames.arrive(coffeepod, "coffeepod")) }
        assertEquals(turned, frames.frame)
    }

    @Test
    fun anOfferIsOnlyAdoptedByTheArrivalItWasReadFor() {
        val bay = StandPoint(Point3(-2.2f, 0f, 0f), 90f)
        val coffeepod = StandPoint(Point3(-2.5f, 0f, 0.5f), 106.5f)
        val turned = HeadFrame(Point3(0.3f, 0f, -0.2f), -90f, 1.7f)
        val frames = ArrivalFrames()
        frames.arrive(StandPoint(Point3(0f, 0f, 0f), 0f), null)
        // Refused (the archive was not OPEN): dropped, so a later panel move to the same exhibit keeps the frame.
        frames.offer("coffeepod", turned)
        frames.drop()
        assertEquals(ArrivalFrameSource.KEPT, frames.arrive(coffeepod, "coffeepod"))
        assertNull(frames.frame)
        // Overtaken by another move before the room was placed: that move keeps the frame, and the offer is used up.
        frames.offer("fidget", turned)
        assertEquals(ArrivalFrameSource.KEPT, frames.arrive(bay, null))
        assertEquals(ArrivalFrameSource.KEPT, frames.arrive(StandPoint(Point3(-2.5f, 0f, -0.5f), 73.5f), "fidget"))
        assertNull(frames.frame)
    }
}
