package dev.local.everydayfutures.museum.state

import dev.local.everydayfutures.museum.catalogue.Catalogue
import dev.local.everydayfutures.museum.story.Pose
import dev.local.everydayfutures.museum.story.StoryMotion
import dev.local.everydayfutures.museum.story.StoryScenes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Host checks of the L3 story: the one reducer path and the miniature compositions for all three objects, driven
 * through the shared controller. They establish state, data and interpolation, not rendering or device behaviour.
 */
class StoryTest {
    private class MemoryPersistence : MuseumPersistence {
        var stored = MuseumState()
        var saves = 0

        override fun load(): MuseumState = MuseumState(savedIds = stored.savedIds, options = stored.options)

        override fun save(state: MuseumState) {
            saves += 1
            stored = state
        }
    }

    private class RecordingNavigator : WindowNavigator {
        val calls = mutableListOf<String>()

        override fun open(id: String, tag: String) {
            calls += "open $id $tag"
        }

        override fun close(id: String, tag: String) {
            calls += "close $id $tag"
        }
    }

    private fun owner(log: MutableList<String> = mutableListOf(), disk: MemoryPersistence = MemoryPersistence()) =
        MuseumController(persistence = disk, log = { log += it })

    @Test
    fun everyObjectHasThreeChaptersAndAMiniatureThatChangesBetweenEachPair() {
        Catalogue.artifacts.forEach { artifact ->
            assertEquals(3, StoryReducer.chapterCount(artifact.id))
            val diorama = StoryScenes.forObject(artifact.id)!!
            assertEquals(3, diorama.chapters)
            diorama.props.forEach { prop -> assertEquals("${artifact.id}/${prop.id}", 3, prop.poses.size) }
            for (c in 0 until 2) {
                assertNotEquals("${artifact.id} chapter ${c + 1} to ${c + 2}", diorama.composition(c), diorama.composition(c + 1))
            }
            assertTrue("${artifact.id} has its own model", diorama.props.any { it.shape == dev.local.everydayfutures.museum.story.Shape.HERO })
            // Bounded: every present prop's horizontal extent (position plus its own half-size, hero models by a generous
            // 0.12 m model radius times their scale) stays inside the footprint the scaled miniature was sized for.
            diorama.props.forEach { prop ->
                prop.poses.filter { it.present }.forEach { p ->
                    val (hx, hz) = halfExtent(prop.shape, prop.w, prop.d)
                    assertTrue("${prop.id} x ${p.x}", kotlin.math.abs(p.x) + hx * p.scale <= StoryScenes.HALF_FOOTPRINT)
                    assertTrue("${prop.id} z ${p.z}", kotlin.math.abs(p.z) + hz * p.scale <= StoryScenes.HALF_FOOTPRINT)
                    assertTrue("${prop.id} y ${p.y}", kotlin.math.abs(p.y) <= 0.09f)
                }
            }
            assertTrue(StoryScenes.HALF_FOOTPRINT * StoryScenes.ROOT_SCALE <= 0.134f)
        }
    }

    @Test
    fun chaptersAreBoundedAndNavigateForwardAndBack() {
        val museum = owner()
        museum.select(Catalogue.VACUUM, Surface.PRIMARY)
        museum.revealStory(RecordingNavigator(), Surface.PRIMARY)
        museum.previousChapter(Surface.COMPANION)
        assertEquals(0, museum.state.story!!.chapter) // no wrap below chapter 1
        museum.nextChapter(Surface.COMPANION)
        museum.nextChapter(Surface.COMPANION)
        museum.nextChapter(Surface.COMPANION)
        assertEquals(2, museum.state.story!!.chapter) // no wrap past chapter 3
        museum.previousChapter(Surface.PRIMARY)
        assertEquals(1, museum.state.story!!.chapter)
    }

    @Test
    fun revealOpensOrFocusesTheOneCompanionAndRepeatedRevealKeepsTheChapter() {
        val log = mutableListOf<String>()
        val museum = owner(log)
        val nav = RecordingNavigator()
        museum.select(Catalogue.VACUUM, Surface.PRIMARY)
        museum.revealStory(nav, Surface.PRIMARY)
        museum.windowAttached(Surface.COMPANION)
        museum.nextChapter(Surface.COMPANION)
        museum.revealStory(nav, Surface.PRIMARY)
        assertEquals(1, museum.state.story!!.chapter)
        val request = "open ${WorkspaceReducer.COMPANION_ID} ${WorkspaceReducer.COMPANION_TAG}"
        assertEquals(listOf(request, request), nav.calls)
        assertEquals(1, museum.state.workspace.companionInstances)
        assertTrue(log.any { it.startsWith("story-reveal id=vacuum chapter=1") })
        assertEquals(listOf("open", "focus"), log.filter { it.startsWith("companion-open") }.map { it.substringAfter("mode=").substringBefore(' ') })
    }

    @Test
    fun restartReturnsToChapterOneAndTouchesNothingElse() {
        val disk = MemoryPersistence()
        val museum = owner(disk = disk)
        museum.select(Catalogue.VACUUM, Surface.PRIMARY)
        museum.keep(Surface.PRIMARY)
        museum.setIncreaseContrast(true, Surface.PRIMARY)
        museum.turn(Turn.RIGHT, Surface.PRIMARY)
        museum.revealStory(RecordingNavigator(), Surface.PRIMARY)
        museum.nextChapter(Surface.COMPANION)
        museum.nextChapter(Surface.COMPANION)
        val before = museum.state
        val savesBefore = disk.saves
        museum.restartStory(Surface.COMPANION)
        val after = museum.state
        assertEquals(0, after.story!!.chapter)
        assertEquals(before.copy(story = before.story!!.copy(chapter = 0)), after)
        assertEquals(savesBefore, disk.saves) // story state is never written
    }

    @Test
    fun returnLeavesTheVisitorInTheInspectionTheyEnteredFrom() {
        val log = mutableListOf<String>()
        val museum = owner(log)
        museum.select(Catalogue.VACUUM, Surface.PRIMARY)
        museum.turn(Turn.RIGHT, Surface.PRIMARY)
        museum.turn(Turn.RIGHT, Surface.PRIMARY) // inspecting, 60°
        val entered = museum.state
        museum.revealStory(RecordingNavigator(), Surface.PRIMARY)
        museum.nextChapter(Surface.COMPANION)
        museum.nextChapter(Surface.COMPANION)
        museum.returnFromStory(Surface.COMPANION)
        assertNull(museum.state.story)
        assertEquals(entered.copy(workspace = museum.state.workspace), museum.state)
        assertTrue(log.last().startsWith("story-return from=3 entry=inspect:2 source=companion steps=2 selected=vacuum view=inspect"))
    }

    @Test
    fun aChangeTheVisitorMadeInTheStudyDuringTheStorySurvivesReturn() {
        val museum = owner()
        museum.select(Catalogue.VACUUM, Surface.PRIMARY)
        museum.turn(Turn.RIGHT, Surface.PRIMARY)
        museum.turn(Turn.RIGHT, Surface.PRIMARY)
        museum.revealStory(RecordingNavigator(), Surface.PRIMARY)
        museum.reset(Surface.PRIMARY) // the study is still live in the primary while the story is open
        museum.returnFromStory(Surface.COMPANION)
        assertEquals(View.CATALOGUE, museum.state.view)
        assertEquals(0, museum.state.turnSteps)
    }

    @Test
    fun selectingAnotherObjectDuringAStoryStartsThatObjectsStoryAtChapterOne() {
        val log = mutableListOf<String>()
        val museum = owner(log)
        museum.select(Catalogue.VACUUM, Surface.PRIMARY)
        museum.revealStory(RecordingNavigator(), Surface.PRIMARY)
        museum.nextChapter(Surface.COMPANION)
        museum.nextChapter(Surface.COMPANION)
        museum.select(Catalogue.QUENCHER, Surface.COMPANION)
        assertEquals(StoryState(Catalogue.QUENCHER, 0, EntryContext(View.CATALOGUE, 0)), museum.state.story)
        assertTrue(log.last().contains("story=quencher:1"))
        museum.select(Catalogue.QUENCHER, Surface.PRIMARY) // the same object again keeps its chapter
        museum.nextChapter(Surface.COMPANION)
        museum.select(Catalogue.QUENCHER, Surface.PRIMARY)
        assertEquals(1, museum.state.story!!.chapter)
        museum.select("not-in-the-catalogue", Surface.PRIMARY)
        assertEquals(Catalogue.QUENCHER, museum.state.story!!.objectId)
        museum.returnFromStory(Surface.COMPANION)
        assertEquals(Catalogue.QUENCHER, museum.state.selectedId)
        assertEquals(View.CATALOGUE, museum.state.view)
    }

    @Test
    fun closingAndReopeningTheCompanionKeepsTheStoryWhereItWas() {
        val museum = owner()
        val nav = RecordingNavigator()
        museum.select(Catalogue.SMART_SPEAKER, Surface.PRIMARY)
        museum.revealStory(nav, Surface.PRIMARY)
        museum.windowAttached(Surface.COMPANION)
        museum.nextChapter(Surface.COMPANION)
        museum.closeCompanion(nav, Surface.COMPANION)
        museum.windowDetached(Surface.COMPANION)
        assertEquals(StoryState(Catalogue.SMART_SPEAKER, 1, EntryContext(View.CATALOGUE, 0)), museum.state.story)
        museum.revealStory(nav, Surface.PRIMARY)
        museum.windowAttached(Surface.COMPANION)
        assertEquals(1, museum.state.story!!.chapter)
        assertEquals(1, museum.state.workspace.companionInstances)
    }

    @Test
    /**
     * The logical half guards against a future reducer reading the comfort options; the pose half checks every animated
     * end state against the snapped one. The presenter's interruption and snap bookkeeping is covered by
     * [anInterruptedTransitionContinuesFromThePosesOnScreen] through the same pure functions it calls.
     */
    fun reducedMotionTakesTheSameLogicalPathAndArrivesAtTheSameComposition() {
        fun run(reduce: Boolean): List<MuseumState> {
            val museum = owner()
            museum.setReduceMotion(reduce, Surface.PRIMARY)
            val trace = mutableListOf<MuseumState>()
            museum.select(Catalogue.VACUUM, Surface.PRIMARY); trace += museum.state
            museum.revealStory(RecordingNavigator(), Surface.PRIMARY); trace += museum.state
            museum.nextChapter(Surface.COMPANION); trace += museum.state
            museum.nextChapter(Surface.COMPANION); trace += museum.state
            museum.previousChapter(Surface.COMPANION); trace += museum.state
            museum.returnFromStory(Surface.COMPANION); trace += museum.state
            return trace.map { it.copy(options = ComfortOptions()) }
        }
        assertEquals(run(reduce = false), run(reduce = true))

        assertEquals(0, StoryMotion.durationMillis(reduceMotion = true))
        assertTrue(StoryMotion.durationMillis(reduceMotion = false) > 0)
        StoryScenes.all.forEach { diorama ->
            for (from in 0 until diorama.chapters) for (to in 0 until diorama.chapters) {
                val arrived = diorama.props.indices.map { i -> StoryMotion.between(diorama.props[i].poses[from], diorama.props[i].poses[to], 1f) }
                assertEquals(diorama.composition(to), arrived) // animated end == snapped end
            }
        }
    }

    @Test
    fun anInterruptedTransitionContinuesFromThePosesOnScreen() {
        val diorama = StoryScenes.forObject(Catalogue.VACUUM)!!
        val arrived = StoryMotion.retarget(diorama, null, 1f, Catalogue.VACUUM, 0)
        assertEquals(diorama.composition(0), arrived.from) // first composition arrives in place
        assertEquals(diorama.composition(0), StoryMotion.posesAt(diorama, arrived, 1f))

        val toTwo = StoryMotion.retarget(diorama, arrived, 1f, Catalogue.VACUUM, 1)
        val onScreen = StoryMotion.posesAt(diorama, toTwo, 0.3f) // Back pressed 30 % of the way to chapter 2
        val back = StoryMotion.retarget(diorama, toTwo, 0.3f, Catalogue.VACUUM, 0)
        assertEquals(onScreen, StoryMotion.posesAt(diorama, back, 0f)) // no jump at the moment of interruption
        assertEquals(diorama.composition(0), StoryMotion.posesAt(diorama, back, 1f))
        assertNotEquals(diorama.composition(1), StoryMotion.posesAt(diorama, back, 0f)) // it did not snap to chapter 2 first

        val other = StoryMotion.retarget(diorama, back, 0.5f, Catalogue.QUENCHER, 0)
        assertEquals(Catalogue.QUENCHER, other.objectId) // a new object arrives in place, never animated across objects
    }

    @Test
    fun appearingPropsGrowInPlaceAndLeavingPropsShrinkInPlace() {
        val gone = Pose(0f, 0f, 0f, scale = 0f)
        val there = Pose(0.05f, 0.01f, -0.02f, yaw = 90f)
        val half = StoryMotion.between(gone, there, 0.5f)
        assertEquals(there.x, half.x, 1e-6f)
        assertEquals(0.5f, half.scale, 1e-6f)
        val leaving = StoryMotion.between(there, gone, 0.5f)
        assertEquals(there.z, leaving.z, 1e-6f)
        assertEquals(0.5f, leaving.scale, 1e-6f)
        assertFalse(StoryMotion.between(there, gone, 1f).present)
    }

    /** Horizontal half-extents (x, z): boxes are authored unrotated in yaw, round shapes by radius, hero models by 0.12 m x scale. */
    private fun halfExtent(shape: dev.local.everydayfutures.museum.story.Shape, w: Float, d: Float): Pair<Float, Float> = when (shape) {
        dev.local.everydayfutures.museum.story.Shape.BOX -> w / 2f to d / 2f
        dev.local.everydayfutures.museum.story.Shape.HERO -> 0.12f * w to 0.12f * w
        else -> w to w
    }

    @Test
    fun storyStateIsNeverPersisted() {
        val disk = MemoryPersistence()
        val museum = owner(disk = disk)
        museum.select(Catalogue.VACUUM, Surface.PRIMARY)
        museum.revealStory(RecordingNavigator(), Surface.PRIMARY)
        museum.keep(Surface.COMPANION)
        assertNull(owner(disk = disk).state.story)
    }
}
