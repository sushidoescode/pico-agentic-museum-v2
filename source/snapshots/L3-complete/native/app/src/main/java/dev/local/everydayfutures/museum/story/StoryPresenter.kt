package dev.local.everydayfutures.museum.story

import android.os.SystemClock
import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.pico.spatial.core.ecs.Entity
import com.pico.spatial.core.ecs.TransformComponent
import com.pico.spatial.core.math.EulerAngles
import com.pico.spatial.core.math.Vector3
import com.pico.spatial.ui.foundation.content.SpatialView
import dev.local.everydayfutures.museum.content.ExhibitFactory
import dev.local.everydayfutures.museum.state.MuseumController
import dev.local.picopolish.PolishTokens

private const val TAG = "Museum"

/** One built miniature: its tilted root and one entity per prop, in the diorama's prop order. */
private class BuiltDiorama(val diorama: Diorama, val root: Entity, val props: List<Entity?>)

/** Survives recomposition so the disposal effect can destroy exactly what `initial` created. */
private class MiniatureHandles {
    val dioramas = LinkedHashMap<String, BuiltDiorama>()
    var light: Entity? = null

    /** The (object, chapter) whose arrival has been logged, so a first arrival is logged once, after the scene exists. */
    var arrivalLogged: Pair<String, Int>? = null

    val built: Boolean get() = dioramas.isNotEmpty()

    fun destroyAll() {
        dioramas.values.forEach { it.root.destroy(recursively = true) }
        dioramas.clear()
        light?.destroy(recursively = true)
        light = null
    }
}

/**
 * The companion's story scene: the one reusable miniature path for every object. All dioramas are built once in
 * `initial` (main thread), posed at the story's current chapter, and only the story's object is enabled. A chapter change
 * moves every prop from the poses on screen to the new chapter's poses ([StoryMotion.retarget]) with the kit's standard
 * easing, or snaps under Reduce motion (also when Reduce motion is switched on mid-transition); `update` writes the
 * interpolated poses and re-runs while the progress animates, then stops. Each arrival is logged once
 * (`story-scene … motion=arrive|animated|still`). This composable owns and destroys its entities; nothing here touches
 * the primary window's scene.
 */
@Composable
fun StoryPresenter(
    objectId: String,
    chapter: Int,
    reduceMotion: Boolean,
    controller: MuseumController,
    modifier: Modifier = Modifier,
) {
    val handles = remember { MiniatureHandles() }
    var track by remember { mutableStateOf<SceneTrack?>(null) }
    val progress = remember { Animatable(1f) }
    val currentObject by rememberUpdatedState(objectId)
    val currentChapter by rememberUpdatedState(chapter)

    DisposableEffect(Unit) {
        onDispose { handles.destroyAll() }
    }

    // Keyed on reduceMotion too: switching it on mid-transition restarts this effect, which then snaps from the poses on
    // screen. When nothing is moving and the target is unchanged, the restart does nothing and logs nothing.
    LaunchedEffect(objectId, chapter, reduceMotion) {
        val diorama = StoryScenes.forObject(objectId) ?: return@LaunchedEffect
        val current = track
        val arriving = current == null || current.objectId != objectId
        if (!arriving && current!!.toChapter == chapter && progress.value >= 1f) return@LaunchedEffect
        val started = SystemClock.uptimeMillis()
        track = StoryMotion.retarget(diorama, current, progress.value, objectId, chapter)
        when {
            arriving -> {
                progress.snapTo(1f)
                // Before `initial` has built the scene the arrival is logged there instead, once the miniature exists.
                if (handles.built) logArrival(handles, controller, objectId, chapter)
            }
            reduceMotion -> {
                progress.snapTo(1f)
                controller.storySceneSettled(objectId, chapter, motion = "still", millis = SystemClock.uptimeMillis() - started)
            }
            else -> {
                progress.snapTo(0f)
                progress.animateTo(1f, tween(durationMillis = StoryMotion.durationMillis(false), easing = PolishTokens.EasingStandard))
                controller.storySceneSettled(objectId, chapter, motion = "animated", millis = SystemClock.uptimeMillis() - started)
            }
        }
    }

    SpatialView(
        modifier = modifier,
        initial = { content, _ ->
            // Factories can throw ResourceLoadingException; each miniature is built under its own guard.
            runCatching { ExhibitFactory.keyLight() }
                .onSuccess { light -> handles.light = light; content.addEntity(light) }
                .onFailure { Log.e(TAG, "story light not created", it) }
            val shownObject = currentObject
            val shownChapter = currentChapter
            StoryScenes.all.forEach { diorama ->
                // Built at the story's current chapter, so a reopened window never flashes chapter 1 first.
                val chapterToBuild = if (diorama.objectId == shownObject) shownChapter else 0
                runCatching { build(diorama, chapterToBuild) }
                    .onSuccess { built ->
                        built.root.enabled = diorama.objectId == shownObject
                        handles.dioramas[diorama.objectId] = built
                        content.addEntity(built.root)
                    }
                    .onFailure { Log.e(TAG, "story-build-failed id=${diorama.objectId}", it) }
            }
            Log.i(TAG, "story-scene-ready dioramas=${handles.dioramas.size} props=${handles.dioramas.values.sumOf { it.props.count { p -> p != null } }}")
            logArrival(handles, controller, shownObject, shownChapter)
        },
        update = { _, _ ->
            val shown = track ?: return@SpatialView
            val t = progress.value
            handles.dioramas.forEach { (id, built) ->
                if (!built.root.valid) return@forEach
                val active = id == shown.objectId
                built.root.enabled = active
                if (!active) return@forEach
                val poses = StoryMotion.posesAt(built.diorama, shown, t)
                built.diorama.props.forEachIndexed { i, prop ->
                    val entity = built.props[i] ?: return@forEachIndexed
                    if (entity.valid) apply(entity, prop, poses[i])
                }
            }
        },
    )
}

/** Logs an arrival once per (object, chapter) after the scene exists. */
private fun logArrival(handles: MiniatureHandles, controller: MuseumController, objectId: String, chapter: Int) {
    if (handles.arrivalLogged == objectId to chapter) return
    handles.arrivalLogged = objectId to chapter
    controller.storySceneSettled(objectId, chapter, motion = "arrive", millis = 0)
}

/** Builds a diorama's tilted root and its props at [chapter]; a prop whose mesh fails is skipped (null) rather than failing the scene. */
private fun build(diorama: Diorama, chapter: Int): BuiltDiorama {
    val root = Entity().apply {
        setName("story_${diorama.objectId}")
        components.get<TransformComponent>()?.apply {
            setPosition(Vector3(x = 0f, y = StoryScenes.ROOT_LIFT, z = 0f))
            setEulerAngles(EulerAngles(pitch = StoryScenes.TILT_DEGREES, yaw = 0f, roll = 0f))
            setScaleVector(Vector3(x = StoryScenes.ROOT_SCALE, y = StoryScenes.ROOT_SCALE, z = StoryScenes.ROOT_SCALE))
        }
    }
    val props = diorama.props.map { prop ->
        runCatching { ExhibitFactory.miniatureProp(prop, diorama.objectId) }
            .onFailure { Log.e(TAG, "story-prop-failed id=${diorama.objectId}/${prop.id}", it) }
            .getOrNull()
            ?.also { entity ->
                apply(entity, prop, prop.poses[chapter.coerceIn(0, prop.poses.size - 1)])
                root.addChild(entity)
            }
    }
    return BuiltDiorama(diorama, root, props)
}

/** Writes one absolute pose; an absent prop is disabled rather than scaled to zero. */
private fun apply(entity: Entity, prop: Prop, pose: Pose) {
    entity.enabled = pose.present
    if (!pose.present) return
    val size = if (prop.shape == Shape.HERO) prop.w * pose.scale else pose.scale
    entity.components.get<TransformComponent>()?.apply {
        setPosition(Vector3(x = pose.x, y = pose.y, z = pose.z))
        setEulerAngles(EulerAngles(pitch = 0f, yaw = pose.yaw, roll = pose.roll))
        setScaleVector(Vector3(x = size, y = size, z = size))
    }
}
