package dev.local.everydayfutures.museum.state

import dev.local.everydayfutures.museum.catalogue.Catalogue
import dev.local.everydayfutures.museum.story.Diorama
import dev.local.everydayfutures.museum.story.Pose
import dev.local.everydayfutures.museum.story.SceneBuild
import dev.local.everydayfutures.museum.story.SceneStatus
import dev.local.everydayfutures.museum.story.SceneTrack
import dev.local.everydayfutures.museum.story.Shape
import dev.local.everydayfutures.museum.story.StoryMotion
import dev.local.everydayfutures.museum.story.StoryScenes
import dev.local.everydayfutures.museum.story.Transition
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
            assertTrue("${artifact.id} has its own model", diorama.props.any { it.shape == Shape.HERO })
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

    /**
     * The presenter's decision when its effect restarts ([StoryMotion.plan], the function it calls): Reduce motion switched
     * on 40 % of the way through a chapter change snaps from the poses on screen to that chapter, and switching it either
     * way at rest does nothing. It does not show that Compose restarts the effect when the key changes, or what the device
     * draws: the mid-transition switch has not been exercised on the emulator.
     */
    @Test
    fun reduceMotionSwitchedOnMidTransitionSnapsFromThePosesOnScreen() {
        val vacuum = StoryScenes.forObject(Catalogue.VACUUM)!!
        val arrive = StoryMotion.plan(vacuum, null, 1f, Catalogue.VACUUM, 0, reduceMotion = false)
        assertEquals(Transition.ARRIVE, arrive.kind)
        assertEquals(vacuum.composition(0), StoryMotion.posesAt(vacuum, arrive.track, 1f))
        val toTwo = StoryMotion.plan(vacuum, arrive.track, 1f, Catalogue.VACUUM, 1, reduceMotion = false)
        assertEquals(Transition.ANIMATE, toTwo.kind)

        val onScreen = StoryMotion.posesAt(vacuum, toTwo.track, 0.4f)
        val switched = StoryMotion.plan(vacuum, toTwo.track, 0.4f, Catalogue.VACUUM, 1, reduceMotion = true)
        assertEquals(Transition.SNAP, switched.kind)
        assertEquals(onScreen, switched.track.from) // no jump back to chapter 1 first
        assertEquals(vacuum.composition(1), StoryMotion.posesAt(vacuum, switched.track, 1f))

        assertEquals(Transition.NONE, StoryMotion.plan(vacuum, switched.track, 1f, Catalogue.VACUUM, 1, reduceMotion = false).kind)
        assertEquals(Transition.NONE, StoryMotion.plan(vacuum, switched.track, 1f, Catalogue.VACUUM, 1, reduceMotion = true).kind)
        val next = StoryMotion.plan(vacuum, switched.track, 1f, Catalogue.VACUUM, 2, reduceMotion = true)
        assertEquals(Transition.SNAP, next.kind)
        assertEquals(vacuum.composition(2), StoryMotion.posesAt(vacuum, next.track, 1f))
        val quencher = StoryScenes.forObject(Catalogue.QUENCHER)!!
        assertEquals(Transition.ARRIVE, StoryMotion.plan(quencher, next.track, 1f, Catalogue.QUENCHER, 0, reduceMotion = true).kind)
    }

    /**
     * The vacuum's body — the L1 model's bumper radius 0.115 and its height, 0.0325 below to 0.0445 above its origin
     * (ExhibitFactory.vacuum), times the hero scale — against every other prop's box, scaled as it grows or shrinks in
     * place, at 201 moments of each of the nine chapter changes, and of each change interrupted at 25, 50 or 75 % and sent
     * to any chapter. Only the ground (plinth and floor tile) is exempt. It checks the poses the presenter writes, not what
     * the renderer draws; before the demo-polish layout it found the chair's side panels, the sofa and the dock.
     */
    @Test
    fun theVacuumNeverPassesThroughAnotherPropDuringAChapterChange() {
        val vacuum = StoryScenes.forObject(Catalogue.VACUUM)!!
        val clashes = mutableListOf<String>()
        fun sweep(track: SceneTrack, label: String) {
            for (k in 0..200) {
                val t = k / 200f
                clashes += vacuumClashes(vacuum, StoryMotion.posesAt(vacuum, track, t)).map { "$label t=$t $it" }
            }
        }
        for (from in 0 until 3) for (to in 0 until 3) {
            val track = SceneTrack(Catalogue.VACUUM, vacuum.composition(from), to)
            sweep(track, "${from + 1}->${to + 1}")
            for (cut in listOf(0.25f, 0.5f, 0.75f)) for (next in 0 until 3) {
                sweep(StoryMotion.retarget(vacuum, track, cut, Catalogue.VACUUM, next), "${from + 1}->${to + 1} cut $cut ->${next + 1}")
            }
        }
        assertTrue("${clashes.size} clashes, first ${clashes.take(5)}", clashes.isEmpty())

        // Not vacuous: the vacuum set down on the chair is caught.
        val chairLeg = vacuum.props.first { it.id == "chair-left" }.poses[0]
        val onChair = vacuum.composition(0).mapIndexed { i, p -> if (vacuum.props[i].shape == Shape.HERO) p.copy(x = chairLeg.x, z = chairLeg.z) else p }
        assertTrue(vacuumClashes(vacuum, onChair).any { it.startsWith("chair-left") })
    }

    /** Props (other than the ground) whose box overlaps the vacuum's body in [poses]. */
    private fun vacuumClashes(diorama: Diorama, poses: List<Pose>): List<String> {
        val heroIndex = diorama.props.indexOfFirst { it.shape == Shape.HERO }
        val hero = poses[heroIndex]
        val size = diorama.props[heroIndex].w * hero.scale
        val radius = 0.115f * size
        val bottom = hero.y - 0.0325f * size
        val top = hero.y + 0.0445f * size
        return diorama.props.indices.filter { it != heroIndex && diorama.props[it].id !in setOf("plinth", "floor") && poses[it].present }.mapNotNull { i ->
            val prop = diorama.props[i]
            val p = poses[i]
            val half = when (prop.shape) {
                Shape.BOX -> Triple(prop.w / 2f, prop.h / 2f, prop.d / 2f)
                Shape.CYLINDER -> Triple(prop.w, prop.h / 2f, prop.w)
                Shape.CAPSULE -> Triple(prop.w, prop.h / 2f + prop.w, prop.w)
                Shape.SPHERE, Shape.HERO -> Triple(prop.w, prop.w, prop.w)
            }
            val hx = half.first * p.scale
            val hy = half.second * p.scale
            val hz = half.third * p.scale
            if (p.y + hy <= bottom || p.y - hy >= top) return@mapNotNull null
            val dx = hero.x - hero.x.coerceIn(p.x - hx, p.x + hx)
            val dz = hero.z - hero.z.coerceIn(p.z - hz, p.z + hz)
            val gap = kotlin.math.sqrt(dx * dx + dz * dz) - radius
            if (gap < 0f) "${prop.id} overlap=${-gap}" else null
        }
    }

    /** The loading state clears only for the story's own, complete miniature; a failed one is never ready. */
    @Test
    fun theStoryRegionIsReadyOnlyWhenTheStorysOwnMiniatureExists() {
        var build = SceneBuild()
        assertEquals(SceneStatus.PREPARING, build.status(Catalogue.VACUUM))
        build = build.record(Catalogue.QUENCHER, complete = true)
        assertEquals(SceneStatus.PREPARING, build.status(Catalogue.VACUUM)) // another object's miniature does not clear it
        build = build.record(Catalogue.VACUUM, complete = false)
        assertEquals(SceneStatus.FAILED, build.status(Catalogue.VACUUM)) // incomplete is a failure, never ready
        assertEquals(SceneStatus.READY, build.status(Catalogue.QUENCHER))
        assertEquals(SceneStatus.READY, SceneBuild().record(Catalogue.VACUUM, complete = true).status(Catalogue.VACUUM))
        assertTrue(SceneBuild.canRetry(1))
        assertTrue(SceneBuild.canRetry(2))
        assertFalse(SceneBuild.canRetry(SceneBuild.MAX_ATTEMPTS)) // bounded: the third attempt offers only Return and Close
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
    private fun halfExtent(shape: Shape, w: Float, d: Float): Pair<Float, Float> = when (shape) {
        Shape.BOX -> w / 2f to d / 2f
        Shape.HERO -> 0.12f * w to 0.12f * w
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
