package dev.local.lunamuseum.reproduction.content

import org.junit.Assert.assertEquals
import org.junit.Test

class MuseumSelectionTest {
    @Test
    fun selectingAnObjectUpdatesTheDescriptionAndResetsTurn() {
        val turned = MuseumSelection().turn().turn()
        val selected = turned.select(MuseumObject.SMART_SPEAKER)

        assertEquals(MuseumObject.SMART_SPEAKER, selected.selected)
        assertEquals("Smart speaker", selected.selected.title)
        assertEquals(0, selected.turnQuarterTurns)
    }

    @Test
    fun resetReturnsTheSelectedObjectToItsOriginalRotation() {
        val reset = MuseumSelection(
            selected = MuseumObject.ROBOT_VACUUM,
            turnQuarterTurns = 3,
        ).reset()

        assertEquals(MuseumObject.ROBOT_VACUUM, reset.selected)
        assertEquals(0, reset.turnQuarterTurns)
    }

    @Test
    fun turningWrapsAfterFourQuarterTurns() {
        val result = MuseumSelection()
            .turn()
            .turn()
            .turn()
            .turn()

        assertEquals(0, result.turnQuarterTurns)
    }
}
