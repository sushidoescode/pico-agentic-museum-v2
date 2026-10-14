package dev.local.lunamuseum.reproduction.content

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class MuseumSelectionStoreTest {
    @Before
    fun resetStore() {
        MuseumSelectionStore.reset()
    }

    @Test
    fun homeSelectionIsImmediatelyReadableByCompanion() {
        val homeSelection = MuseumSelection()
            .select(MuseumObject.ROBOT_VACUUM)
            .turn()

        MuseumSelectionStore.update(homeSelection)

        assertEquals(homeSelection, MuseumSelectionStore.current)
    }

    @Test
    fun companionSelectionIsImmediatelyReadableByHome() {
        val companionSelection = MuseumSelection()
            .select(MuseumObject.SMART_SPEAKER)
            .turn()
            .turn()

        MuseumSelectionStore.update(companionSelection)

        assertEquals(companionSelection, MuseumSelectionStore.current)
    }

    @Test
    fun storeResetRestoresTheDefaultSelection() {
        MuseumSelectionStore.update(
            MuseumSelection(
                selected = MuseumObject.SMART_SPEAKER,
                turnQuarterTurns = 2,
            ),
        )

        MuseumSelectionStore.reset()

        assertEquals(MuseumSelection(), MuseumSelectionStore.current)
    }
}
