package dev.local.everydayfutures.museum.state

import dev.local.everydayfutures.museum.catalogue.Catalogue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/** Host-only reducer checks. They establish state transitions, not rendering, input or any device behaviour. */
class MuseumStateTest {
    @Test
    fun initialStateShowsTheQuencherAtTheFront() {
        val s = MuseumState()
        assertEquals(Catalogue.QUENCHER, s.selectedId)
        assertEquals(View.CATALOGUE, s.view)
        assertTrue(s.isFrontView)
        assertEquals(0f, s.turnDegrees, 0f)
        assertTrue(s.savedIds.isEmpty())
    }

    @Test
    fun catalogueHasExactlyTheThreeObjectsInOrder() {
        assertEquals(listOf(Catalogue.QUENCHER, Catalogue.VACUUM, Catalogue.SMART_SPEAKER), Catalogue.artifacts.map { it.id })
        Catalogue.artifacts.forEach { a ->
            assertEquals(3, a.chapters.size)
            assertTrue(a.description.isNotBlank() && a.title.isNotBlank() && a.selectLabel.isNotBlank())
        }
    }

    @Test
    fun selectingAnotherObjectResetsOrientationAndView() {
        val turned = MuseumReducer.turn(MuseumState(), Turn.RIGHT)
        val s = MuseumReducer.select(turned, Catalogue.VACUUM)
        assertEquals(Catalogue.VACUUM, s.selectedId)
        assertEquals(View.CATALOGUE, s.view)
        assertEquals(0, s.turnSteps)
    }

    @Test
    fun selectingAnUnknownIdIsIgnored() {
        val s = MuseumState()
        assertSame(s, MuseumReducer.select(s, "astronaut"))
    }

    @Test
    fun turningEntersInspectionAndTwelveStepsReturnToTheFront() {
        var s = MuseumState()
        s = MuseumReducer.turn(s, Turn.RIGHT)
        assertEquals(View.INSPECT, s.view)
        assertEquals(30f, s.turnDegrees, 0f)
        assertFalse(s.isFrontView)
        repeat(11) { s = MuseumReducer.turn(s, Turn.RIGHT) }
        assertEquals(12, s.turnSteps)
        assertTrue(s.isFrontView)
        s = MuseumReducer.turn(s, Turn.LEFT)
        assertEquals(11, s.turnSteps)
        assertEquals(330f, s.turnDegrees, 0f)
        assertFalse(s.isFrontView)
    }

    @Test
    fun displayedDegreesStayWithinOneTurn() {
        var s = MuseumState()
        repeat(13) { s = MuseumReducer.turn(s, Turn.RIGHT) }
        assertEquals(390f, s.turnDegrees, 0f)
        assertEquals(30, s.displayDegrees)
        s = MuseumReducer.turn(MuseumState(), Turn.LEFT)
        assertEquals(-30f, s.turnDegrees, 0f)
        assertEquals(330, s.displayDegrees)
        assertEquals(0, MuseumState().displayDegrees)
    }

    @Test
    fun negativeStepsStillReportTheFrontCorrectly() {
        var s = MuseumState()
        repeat(12) { s = MuseumReducer.turn(s, Turn.LEFT) }
        assertEquals(-12, s.turnSteps)
        assertTrue(s.isFrontView)
    }

    @Test
    fun resetReturnsToFrontAndCataloguePreservingSelectionSavedAndOptions() {
        var s = MuseumReducer.select(MuseumState(), Catalogue.SMART_SPEAKER)
        s = MuseumReducer.keep(s)
        s = MuseumReducer.setOptions(s, ComfortOptions(reduceMotion = true))
        s = MuseumReducer.turn(s, Turn.LEFT)
        val r = MuseumReducer.reset(s)
        assertEquals(Catalogue.SMART_SPEAKER, r.selectedId)
        assertEquals(View.CATALOGUE, r.view)
        assertTrue(r.isFrontView)
        assertEquals(listOf(Catalogue.SMART_SPEAKER), r.savedIds)
        assertTrue(r.options.reduceMotion)
    }

    @Test
    fun keepIsIdempotentAndOrdered() {
        var s = MuseumReducer.keep(MuseumState())
        s = MuseumReducer.keep(s)
        assertEquals(listOf(Catalogue.QUENCHER), s.savedIds)
        s = MuseumReducer.keep(MuseumReducer.select(s, Catalogue.VACUUM))
        assertEquals(listOf(Catalogue.QUENCHER, Catalogue.VACUUM), s.savedIds)
        assertTrue(s.isSaved(Catalogue.QUENCHER))
        assertTrue(MuseumReducer.clearSaved(s).savedIds.isEmpty())
    }

    @Test
    fun resetIsOnlyOfferedWhenThereIsSomethingToReset() {
        val fresh = MuseumState()
        assertFalse(fresh.canReset)
        assertTrue(MuseumReducer.turn(fresh, Turn.RIGHT).canReset)
        assertTrue(MuseumReducer.toggleInspect(fresh).canReset)
        assertFalse(MuseumReducer.reset(MuseumReducer.turn(fresh, Turn.LEFT)).canReset)
    }

    @Test
    fun toggleInspectDoesNotChangeOrientation() {
        val turned = MuseumReducer.turn(MuseumState(), Turn.RIGHT)
        val back = MuseumReducer.toggleInspect(turned)
        assertEquals(View.CATALOGUE, back.view)
        assertEquals(1, back.turnSteps)
        assertEquals(View.INSPECT, MuseumReducer.toggleInspect(back).view)
    }
}
