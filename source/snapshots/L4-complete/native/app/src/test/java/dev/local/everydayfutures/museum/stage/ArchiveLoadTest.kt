package dev.local.everydayfutures.museum.stage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ArchiveLoadTest {
    private val layout = RoomLayout(
        roomModels = listOf("room/earth_archive.glb"),
        anchors = listOf(
            Anchor("quencher", Point3(-4f, 1f, 2.2f), 90f, bay = "daily"),
            Anchor("coffeepod", Point3(-4f, 1f, 1.3f), 90f, bay = "daily"),
        ),
        viewpoints = mapOf(RoomLayout.ARRIVAL to StandPoint(Point3(0f, 0f, 0f), 0f)),
    )

    @Test
    fun onlyAnchoredExhibitsAreLoaded() {
        val load = ArchiveLoad.start(listOf("quencher", "coffeepod", "gascan"), layout, now = 100)
        assertEquals(2, load.total)
        assertEquals(ExhibitLoad.NO_ANCHOR, load.exhibits["gascan"])
        assertFalse(load.settled)
    }

    @Test
    fun loadingSettlesWhenRoomAndEveryAnchoredExhibitHaveFinished() {
        var load = ArchiveLoad.start(listOf("quencher", "coffeepod"), layout, now = 100)
        load = load.roomDone(ok = true, now = 400)
        assertEquals(300L, load.roomMillis)
        load = load.exhibitDone("quencher", ok = true, now = 500)
        assertFalse(load.settled)
        assertNull(load.allMillis)
        load = load.exhibitDone("coffeepod", ok = false, now = 900)
        assertTrue(load.settled)
        assertEquals(1, load.ready)
        assertEquals(1, load.failed)
        assertEquals(800L, load.allMillis)
    }

    @Test
    fun aLateOrRepeatedResultChangesNothing() {
        var load = ArchiveLoad.start(listOf("quencher"), layout, now = 0)
        load = load.exhibitDone("quencher", ok = true, now = 10)
        assertEquals(load, load.exhibitDone("quencher", ok = false, now = 20))
        assertEquals(load, load.exhibitDone("not-in-catalogue", ok = true, now = 20))
    }

    @Test
    fun aMissingLayoutFailsTheRoomAndPlacesNothing() {
        val load = ArchiveLoad.start(listOf("quencher", "coffeepod"), layout = null, now = 0)
        assertEquals(RoomLoad.FAILED, load.room)
        assertEquals(0, load.total)
        assertTrue(load.settled)
    }

    @Test
    fun layoutLooksUpAnchorsAndBays() {
        assertEquals("coffeepod", layout.anchor("coffeepod")?.id)
        assertNull(layout.anchor("rocket"))
        assertEquals(2, layout.anchorsInBay("daily").size)
    }
}
