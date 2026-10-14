package dev.local.lunamuseum.reproduction.content

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MuseumStoryTest {
    @Before
    fun resetStore() {
        MuseumStoryStore.reset()
    }

    @Test
    fun eachObjectHasThreeStorySteps() {
        MuseumObject.values().forEach { objectType ->
            assertEquals(3, MuseumStories.forObject(objectType).steps.size)
        }
    }

    @Test
    fun restartReturnsToTheFirstStepAndStartsPlaying() {
        val restarted = MuseumStoryState(
            selected = MuseumObject.ROBOT_VACUUM,
            stepIndex = 2,
        ).restart()

        assertEquals(0, restarted.stepIndex)
        assertTrue(restarted.isPlaying)
        assertEquals("The robot vacuum starts at home.", restarted.stepText)
    }

    @Test
    fun playbackAdvancesThroughAllStepsAndStops() {
        val finished = MuseumStoryState().play().advance().advance().advance()

        assertEquals(2, finished.stepIndex)
        assertFalse(finished.isPlaying)
    }

    @Test
    fun reducedMotionPreservesStoryStateWhileTurningOn() {
        val state = MuseumStoryState(stepIndex = 1, isPlaying = true)
        val reduced = state.toggleReducedMotion()

        assertTrue(reduced.reducedMotion)
        assertEquals(state.stepIndex, reduced.stepIndex)
        assertEquals(state.stepText, reduced.stepText)
        assertTrue(reduced.isPlaying)
    }

    @Test
    fun resetRestoresTheCurrentExhibitsFirstStepAndPreservesReducedMotion() {
        MuseumStoryStore.update(
            MuseumStoryState(
                selected = MuseumObject.ROBOT_VACUUM,
                stepIndex = 2,
                isPlaying = true,
                reducedMotion = true,
            ),
        )

        MuseumStoryStore.reset(MuseumObject.ROBOT_VACUUM)

        assertEquals(MuseumObject.ROBOT_VACUUM, MuseumStoryStore.current.selected)
        assertEquals(0, MuseumStoryStore.current.stepIndex)
        assertEquals("The robot vacuum starts at home.", MuseumStoryStore.current.stepText)
        assertFalse(MuseumStoryStore.current.isPlaying)
        assertTrue(MuseumStoryStore.current.reducedMotion)
    }

    @Test
    fun reducedMotionProducesZeroAutomaticMotion() {
        val motion = museumStoryMotion(
            objectType = MuseumObject.SMART_SPEAKER,
            stepIndex = 2,
            reducedMotion = true,
        )

        assertEquals(MuseumStoryMotionValues(), motion)
    }
}
