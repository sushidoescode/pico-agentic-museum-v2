package dev.local.everydayfutures.museum.story

import dev.local.everydayfutures.museum.catalogue.Catalogue

/** Primitive of a miniature prop. [HERO] is the object's own procedural model (ExhibitFactory) at miniature scale. */
enum class Shape { BOX, SPHERE, CYLINDER, CAPSULE, HERO }

/** The existing visual language: the L1 palette plus a glowing brass for traces and signals. */
enum class Finish { SLATE, MIST, INK, BRASS, GLOW }

/**
 * One chapter's placement of a prop in the miniature's frame (metres, +x right, +y up, +z toward the viewer; degrees).
 * [scale] multiplies the prop's size; 0 means the prop is absent in that chapter.
 */
data class Pose(
    val x: Float,
    val y: Float,
    val z: Float,
    val yaw: Float = 0f,
    val roll: Float = 0f,
    val scale: Float = 1f,
) {
    val present: Boolean get() = scale > ABSENT

    companion object {
        const val ABSENT = 0.001f
    }
}

/**
 * A miniature prop: shape, base size (metres: box = width/height/depth; sphere = radius in [w]; cylinder = radius [w] and
 * height [h]; capsule = radius [w] and height [h]; hero = uniform model scale in [w]), finish and one pose per chapter.
 */
data class Prop(
    val id: String,
    val shape: Shape,
    val w: Float,
    val h: Float = 0f,
    val d: Float = 0f,
    val finish: Finish,
    val poses: List<Pose>,
)

/** One object's bounded miniature: every prop has exactly one pose per chapter. */
data class Diorama(val objectId: String, val props: List<Prop>) {
    val chapters: Int get() = props.firstOrNull()?.poses?.size ?: 0

    /** The pose of every prop at [chapter] (clamped), in prop order. */
    fun composition(chapter: Int): List<Pose> {
        val c = chapter.coerceIn(0, (chapters - 1).coerceAtLeast(0))
        return props.map { it.poses[c] }
    }
}

/** Interpolation between two chapter compositions. Pure, so the end state is testable with and without motion. */
object StoryMotion {
    /** Duration of a chapter change; reduced motion snaps (0 ms) through the same poses. */
    const val CHAPTER_MILLIS = 700

    fun durationMillis(reduceMotion: Boolean): Int = if (reduceMotion) 0 else CHAPTER_MILLIS

    /**
     * The pose of a prop moving from [from] to [to] at progress [t] (0..1, already eased by the caller). A prop that
     * appears grows in place at its destination; one that leaves shrinks in place where it was.
     */
    fun between(from: Pose, to: Pose, t: Float): Pose {
        val p = t.coerceIn(0f, 1f)
        if (p >= 1f) return to
        val start = if (from.present) from else to.copy(scale = 0f)
        val end = if (to.present) to else from.copy(scale = 0f)
        return Pose(
            x = lerp(start.x, end.x, p),
            y = lerp(start.y, end.y, p),
            z = lerp(start.z, end.z, p),
            yaw = lerp(start.yaw, end.yaw, p),
            roll = lerp(start.roll, end.roll, p),
            scale = lerp(start.scale, end.scale, p),
        )
    }

    private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

    /** Every prop's pose on a [track] at progress [t]. At t = 1 this is exactly the target chapter's composition. */
    fun posesAt(diorama: Diorama, track: SceneTrack, t: Float): List<Pose> =
        diorama.props.mapIndexed { i, prop -> between(track.from[i], prop.poses[track.toChapter], t) }

    /**
     * The next track when the story moves to ([objectId], [chapter]) while the scene shows [current] at progress [t].
     * A new object (or the first composition) arrives in place; otherwise the new transition starts from the poses on
     * screen at that moment, so an interrupted transition continues instead of jumping to the old target first.
     */
    fun retarget(diorama: Diorama, current: SceneTrack?, t: Float, objectId: String, chapter: Int): SceneTrack {
        val c = chapter.coerceIn(0, (diorama.chapters - 1).coerceAtLeast(0))
        return if (current == null || current.objectId != objectId) {
            SceneTrack(objectId, diorama.composition(c), c)
        } else {
            SceneTrack(objectId, posesAt(diorama, current, t), c)
        }
    }

    /**
     * What the presenter does when the story, or Reduce motion, changes while the scene shows [current] at progress [t]:
     * nothing if the scene is already at rest on that chapter; otherwise a new track from the poses on screen, reached by
     * arriving in place (a new object or the first composition), snapping (Reduce motion — also when it was switched on
     * part-way through a transition) or animating. The presenter's effect is keyed on the object, the chapter and Reduce
     * motion, and calls this each time it (re)starts.
     */
    fun plan(diorama: Diorama, current: SceneTrack?, t: Float, objectId: String, chapter: Int, reduceMotion: Boolean): TransitionPlan {
        val arriving = current == null || current.objectId != objectId
        val c = chapter.coerceIn(0, (diorama.chapters - 1).coerceAtLeast(0))
        if (!arriving && current!!.toChapter == c && t >= 1f) return TransitionPlan(Transition.NONE, current)
        val track = retarget(diorama, current, t, objectId, chapter)
        val kind = when {
            arriving -> Transition.ARRIVE
            reduceMotion -> Transition.SNAP
            else -> Transition.ANIMATE
        }
        return TransitionPlan(kind, track)
    }
}

/** What the miniature is doing: for [objectId], every prop moving from [from] (one pose per prop) to chapter [toChapter]. */
data class SceneTrack(val objectId: String, val from: List<Pose>, val toChapter: Int)

/** How the scene reaches a new target: [ARRIVE] and [SNAP] jump to progress 1, [ANIMATE] eases there, [NONE] leaves it. */
enum class Transition { NONE, ARRIVE, SNAP, ANIMATE }

/** The presenter's next [track] (unchanged for [Transition.NONE]) and how to reach its end. */
data class TransitionPlan(val kind: Transition, val track: SceneTrack)

/**
 * The three authored miniatures (story copy in [Catalogue]; design in docs/lessons/lesson-3/STORY_DESIGN.md). All share
 * a round ink plinth and a mist floor; unscaled they fit a footprint of about 0.21 m (see [HALF_FOOTPRINT]).
 */
object StoryScenes {
    /** Top of the floor tile, in the miniature's frame. */
    private const val FLOOR = -0.055f

    /** Tilt of the whole miniature toward the viewer, so a tabletop reads from the front. */
    const val TILT_DEGREES = 24f

    /**
     * Scale and lift of the whole miniature in the companion's scene. From the first opened frames (L3 build-001) the
     * unscaled miniature filled about two thirds of its 440 dp region and sat low; 1.25 keeps the tilted plinth inside
     * the 400 dp volume depth (about ±0.134 scene metres at the companion's observed 1,490 dp per metre). Unscaled, every
     * prop's extent stays within [HALF_FOOTPRINT] of the origin horizontally (checked by StoryTest).
     */
    const val ROOT_SCALE = 1.25f
    const val ROOT_LIFT = 0.02f

    /** Largest horizontal half-extent of any prop in the unscaled miniature (metres); times [ROOT_SCALE] it stays inside the volume. */
    const val HALF_FOOTPRINT = 0.106f

    /**
     * The miniature's scale in the guide's Subwindow prototype (state/Workspace.kt, CompanionHost.SUBWINDOW), whose width
     * is fixed at 360 dp, about 328 dp inside its padding. At the companion's observed 1,490 dp per metre the footprint
     * is then 2 × [HALF_FOOTPRINT] × 1.0 ≈ 316 dp (at [ROOT_SCALE] about 395 dp). The Subwindow's own dp per metre is not
     * measured yet; the prototype's native check records it.
     */
    const val COMPACT_ROOT_SCALE = 1.0f

    fun forObject(objectId: String): Diorama? = all.firstOrNull { it.objectId == objectId }

    val all: List<Diorama> by lazy { listOf(quencher(), vacuum(), speaker()) }

    private fun same(p: Pose) = listOf(p, p, p)

    private fun only(chapter: Int, p: Pose) = List(3) { if (it == chapter) p else p.copy(scale = 0f) }

    private fun from(chapter: Int, p: Pose) = List(3) { if (it >= chapter) p else p.copy(scale = 0f) }

    private fun base(): List<Prop> = listOf(
        Prop("plinth", Shape.CYLINDER, w = 0.105f, h = 0.01f, finish = Finish.INK, poses = same(Pose(0f, FLOOR - 0.007f, 0f))),
        Prop("floor", Shape.BOX, w = 0.17f, h = 0.004f, d = 0.13f, finish = Finish.MIST, poses = same(Pose(0f, FLOOR - 0.002f, 0f))),
    )

    /**
     * Robot vacuum — Routine / Boundary / Human trace. The walkthrough anchor.
     *
     * The vacuum moves in a straight line between its dock (chapters 1 and 3) and the threshold (chapter 2), so the room
     * is laid out around that line rather than the vacuum steering round the room: the chair and the sofa island stand in
     * front of it, the doorway is wide enough for the vacuum's body, the dock sits just behind the docked vacuum, and every
     * route mark lies off the line. Until the demo-polish pass the straight move crossed the chair's side panels, the sofa
     * and the dock; StoryTest now samples the vacuum's body along every chapter change, interrupted ones included.
     */
    private fun vacuum(): Diorama {
        val hero = Prop(
            "vacuum", Shape.HERO, w = 0.28f, finish = Finish.MIST,
            poses = listOf(
                Pose(-0.05f, FLOOR + 0.009f, -0.035f, yaw = 0f),
                Pose(0.066f, FLOOR + 0.009f, -0.012f, yaw = 90f),
                Pose(-0.05f, FLOOR + 0.009f, -0.035f, yaw = 180f),
            ),
        )
        val chairPose = Pose(-0.012f, FLOOR, 0.036f)
        val chair = listOf(
            Prop("chair-seat", Shape.BOX, w = 0.036f, h = 0.005f, d = 0.034f, finish = Finish.SLATE, poses = until3(chairPose.copy(y = FLOOR + 0.03f))),
            Prop("chair-back", Shape.BOX, w = 0.036f, h = 0.034f, d = 0.004f, finish = Finish.SLATE, poses = until3(chairPose.copy(y = FLOOR + 0.05f, z = chairPose.z - 0.016f))),
            Prop("chair-left", Shape.BOX, w = 0.004f, h = 0.03f, d = 0.03f, finish = Finish.INK, poses = until3(chairPose.copy(x = chairPose.x - 0.016f, y = FLOOR + 0.015f))),
            Prop("chair-right", Shape.BOX, w = 0.004f, h = 0.03f, d = 0.03f, finish = Finish.INK, poses = until3(chairPose.copy(x = chairPose.x + 0.016f, y = FLOOR + 0.015f))),
        )
        val crumb = Prop("crumb", Shape.SPHERE, w = 0.0045f, finish = Finish.BRASS, poses = until3(Pose(-0.004f, FLOOR + 0.0045f, 0.049f)))
        // Behind the docked vacuum, off the floor tile, standing on the plinth.
        val dock = Prop("dock", Shape.BOX, w = 0.034f, h = 0.014f, d = 0.01f, finish = Finish.INK, poses = same(Pose(-0.05f, FLOOR + 0.005f, -0.073f)))
        val sofa = Prop("sofa", Shape.BOX, w = 0.05f, h = 0.022f, d = 0.026f, finish = Finish.SLATE, poses = only(1, Pose(0.035f, FLOOR + 0.011f, 0.049f)))
        // The doorway's opening (0.076 m between the posts) is wider than the vacuum's body; the vacuum stops in it.
        val door = listOf(
            Prop("door-near", Shape.BOX, w = 0.004f, h = 0.062f, d = 0.004f, finish = Finish.INK, poses = only(1, Pose(0.078f, FLOOR + 0.031f, 0.028f))),
            Prop("door-far", Shape.BOX, w = 0.004f, h = 0.062f, d = 0.004f, finish = Finish.INK, poses = only(1, Pose(0.078f, FLOOR + 0.031f, -0.052f))),
            Prop("door-lintel", Shape.BOX, w = 0.004f, h = 0.004f, d = 0.084f, finish = Finish.INK, poses = only(1, Pose(0.078f, FLOOR + 0.064f, -0.012f))),
        )
        // The route: from the dock to the chair and beneath it, along the sofa island to the threshold, and (chapter 3
        // only, lifted) back behind the vacuum's line to the dock.
        val loop = listOf(
            -0.04f to 0.016f, -0.018f to 0.016f, -0.01f to 0.036f, 0.016f to 0.022f,
            0.036f to 0.026f, 0.056f to 0.029f, 0.03f to -0.066f, -0.008f to -0.072f,
        )
        val marks = loop.mapIndexed { i, (x, z) ->
            val flat = Pose(x, FLOOR + 0.0015f, z)
            val lifted = Pose(x, FLOOR + 0.008f, z, scale = 1.35f)
            val visible = when {
                i < 3 -> listOf(flat, flat, lifted)
                i < 6 -> listOf(flat.copy(scale = 0f), flat, lifted)
                else -> listOf(flat.copy(scale = 0f), flat.copy(scale = 0f), lifted)
            }
            // Brass, like the crumb: build-001's glowing marks were near-white on the mist floor and unreadable until lifted.
            Prop("route-${i + 1}", Shape.CYLINDER, w = 0.007f, h = 0.003f, finish = Finish.BRASS, poses = visible)
        }
        return Diorama(Catalogue.VACUUM, base() + dock + chair + crumb + sofa + door + marks + hero)
    }

    /** Quencher cup — Ritual / Signal / Afterlife. */
    private fun quencher(): Diorama {
        // The model's body centre is its origin; its base sits 0.075 m below it before scaling.
        val onDesk = Pose(0f, FLOOR + 0.03f + 0.075f * 0.2f, 0f, yaw = -20f, scale = 1f)
        val onPlinth = Pose(0f, FLOOR + 0.034f + 0.075f * 0.24f, -0.012f, yaw = 10f, scale = 1.2f)
        val lying = Pose(0f, FLOOR + 0.01f, 0.002f, yaw = 25f, roll = 90f, scale = 1f)
        val hero = Prop("cup", Shape.HERO, w = 0.2f, finish = Finish.SLATE, poses = listOf(onDesk, onPlinth, lying))
        val desk = Prop("desk", Shape.BOX, w = 0.1f, h = 0.03f, d = 0.05f, finish = Finish.SLATE, poses = only(0, Pose(0f, FLOOR + 0.015f, 0f)))
        val tap = Prop("tap", Shape.CYLINDER, w = 0.004f, h = 0.03f, finish = Finish.MIST, poses = only(0, Pose(0.0f, FLOOR + 0.105f, 0f)))
        val spout = Prop("spout", Shape.BOX, w = 0.022f, h = 0.004f, d = 0.004f, finish = Finish.MIST, poses = only(0, Pose(-0.009f, FLOOR + 0.12f, 0f)))
        val drop = Prop("drop", Shape.SPHERE, w = 0.0045f, finish = Finish.GLOW, poses = only(0, Pose(0f, FLOOR + 0.084f, 0f)))
        val badge = Prop("badge-plinth", Shape.CYLINDER, w = 0.024f, h = 0.034f, finish = Finish.BRASS, poses = only(1, Pose(0f, FLOOR + 0.017f, -0.012f)))
        val crowd = listOf(-0.045f to 0.032f, 0f to 0.045f, 0.045f to 0.032f).mapIndexed { i, (x, z) ->
            Prop("crowd-${i + 1}", Shape.CAPSULE, w = 0.007f, h = 0.03f, finish = Finish.MIST, poses = only(1, Pose(x, FLOOR + 0.015f, z)))
        }
        val case = listOf(-0.046f to -0.03f, 0.046f to -0.03f, -0.046f to 0.03f, 0.046f to 0.03f).mapIndexed { i, (x, z) ->
            Prop("case-post-${i + 1}", Shape.BOX, w = 0.003f, h = 0.05f, d = 0.003f, finish = Finish.INK, poses = only(2, Pose(x, FLOOR + 0.025f, z)))
        } + Prop("case-frame", Shape.BOX, w = 0.095f, h = 0.003f, d = 0.063f, finish = Finish.INK, poses = only(2, Pose(0f, FLOOR + 0.051f, 0f)))
        val label = Prop("label", Shape.BOX, w = 0.03f, h = 0.006f, d = 0.012f, finish = Finish.BRASS, poses = only(2, Pose(0.03f, FLOOR + 0.003f, 0.05f)))
        return Diorama(Catalogue.QUENCHER, base() + desk + tap + spout + drop + badge + crowd + case + label + hero)
    }

    /** Smart speaker — Invitation / Connection / Silence. */
    private fun speaker(): Diorama {
        val table = Prop("side-table", Shape.BOX, w = 0.04f, h = 0.03f, d = 0.04f, finish = Finish.SLATE, poses = same(Pose(0.05f, FLOOR + 0.015f, -0.01f)))
        // The model spans about 0.08 m below its origin (base) before scaling.
        val onTable = Pose(0.05f, FLOOR + 0.03f + 0.08f * 0.25f, -0.01f)
        val hero = Prop("speaker", Shape.HERO, w = 0.25f, finish = Finish.SLATE, poses = listOf(onTable, onTable.copy(yaw = 25f), onTable))
        val standing = Pose(-0.06f, FLOOR + 0.0225f, 0.012f)
        val seated = Pose(-0.06f, FLOOR + 0.026f, 0.012f, scale = 0.7f)
        val figure = Prop("figure", Shape.CAPSULE, w = 0.008f, h = 0.045f, finish = Finish.MIST, poses = listOf(standing, standing, seated))
        val chair = Prop("chair", Shape.BOX, w = 0.022f, h = 0.012f, d = 0.022f, finish = Finish.SLATE, poses = from(2, Pose(-0.06f, FLOOR + 0.006f, 0.012f)))
        // Network nodes arch over the room from the speaker; in chapter 1 they wait, absent, at the speaker.
        val arc = listOf(0.05f to 0.085f, 0.03f to 0.115f, 0.0f to 0.13f, -0.03f to 0.115f, -0.05f to 0.085f)
        val nodes = arc.mapIndexed { i, (x, y) ->
            val at = Pose(x, FLOOR + y, -0.03f)
            Prop("node-${i + 1}", Shape.SPHERE, w = 0.0055f, finish = Finish.GLOW, poses = only(1, at))
        }
        val links = arc.zipWithNext().mapIndexed { i, (a, b) ->
            val dx = b.first - a.first
            val dy = b.second - a.second
            val length = kotlin.math.sqrt(dx * dx + dy * dy)
            val roll = Math.toDegrees(kotlin.math.atan2(dy.toDouble(), dx.toDouble())).toFloat()
            val mid = Pose((a.first + b.first) / 2f, FLOOR + (a.second + b.second) / 2f, -0.03f, roll = roll)
            Prop("link-${i + 1}", Shape.BOX, w = length, h = 0.0018f, d = 0.0018f, finish = Finish.BRASS, poses = only(1, mid))
        }
        // Voice marks cross the room in chapter 1 and rise into the network in chapter 2.
        val voice = listOf(-0.032f, -0.006f, 0.02f).mapIndexed { i, x ->
            val crossing = Pose(x, FLOOR + 0.044f, 0.004f - i * 0.004f)
            val risen = Pose(x * 0.8f, FLOOR + 0.1f + i * 0.008f, -0.02f, scale = 0.8f)
            Prop("voice-${i + 1}", Shape.SPHERE, w = 0.004f, finish = Finish.GLOW, poses = listOf(crossing, risen, risen.copy(scale = 0f)))
        }
        val lamp = listOf(
            Prop("lamp-stem", Shape.CYLINDER, w = 0.0025f, h = 0.05f, finish = Finish.INK, poses = from(2, Pose(-0.078f, FLOOR + 0.025f, -0.045f))),
            Prop("lamp-shade", Shape.SPHERE, w = 0.011f, finish = Finish.GLOW, poses = from(2, Pose(-0.078f, FLOOR + 0.055f, -0.045f))),
        )
        return Diorama(Catalogue.SMART_SPEAKER, base() + table + chair + figure + nodes + links + voice + lamp + hero)
    }

    /** Present in chapters 1 and 2, gone in 3 (the vacuum's furniture and crumb). */
    private fun until3(p: Pose) = listOf(p, p, p.copy(scale = 0f))
}
