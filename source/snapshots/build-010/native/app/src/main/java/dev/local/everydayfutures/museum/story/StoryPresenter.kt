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
import kotlinx.coroutines.android.awaitFrame

private const val TAG = "Museum"

/** One built miniature: its tilted root and one entity per prop, in the diorama's prop order. */
private class BuiltDiorama(val diorama: Diorama, val root: Entity, val props: List<Entity?>) {
    /** Every prop was created; an incomplete miniature is destroyed and reported as failed rather than shown. */
    val complete: Boolean get() = props.none { it == null }
}

/** Survives recomposition so the disposal effect can destroy exactly what `initial` created. */
private class MiniatureHandles {
    val dioramas = LinkedHashMap<String, BuiltDiorama>()
    var light: Entity? = null
    var disposed = false

    /** The (object, chapter) whose arrival has been logged, so a first arrival is logged once, after the scene exists. */
    var arrivalLogged: Pair<String, Int>? = null

    fun has(objectId: String): Boolean = dioramas.containsKey(objectId)

    fun destroyAll() {
        disposed = true
        dioramas.values.forEach { it.root.destroy(recursively = true) }
        dioramas.clear()
        light?.destroy(recursively = true)
        light = null
    }
}

/**
 * The companion's story scene: the one reusable miniature path for every object. `initial` (main thread) builds the
 * story's own miniature first, at the story's current chapter, records it in [SceneBuild] through [onSceneBuild] and
 * waits two display frames before building the other two, so the region's preparing state can be recomposed away and
 * drawn once the miniature on screen exists instead of waiting for the whole scene; a miniature that cannot be created
 * completely is destroyed and recorded as failed, never as ready. Only
 * the story's object is enabled. A chapter change moves every prop from the poses on screen to the new chapter's poses
 * ([StoryMotion.plan]) with the kit's standard easing, or snaps under Reduce motion (also when Reduce motion is switched
 * on mid-transition); `update` writes the interpolated poses and re-runs while the progress animates, then stops.
 * Markers: `story-scene-preparing`, then `story-scene-ready` or `story-scene-failed` for the story's object, and each
 * arrival once (`story-scene … motion=arrive|animated|still`). This composable owns and destroys its entities; a new
 * [attempt] is a new composition (the caller keys it), so a retry disposes this scene and builds a fresh one. Nothing
 * here touches the primary window's scene. [rootScale] scales each whole miniature: [StoryScenes.ROOT_SCALE] in the
 * companion window, [StoryScenes.COMPACT_ROOT_SCALE] in the guide's narrower Subwindow prototype.
 */
@Composable
fun StoryPresenter(
    objectId: String,
    chapter: Int,
    reduceMotion: Boolean,
    attempt: Int,
    controller: MuseumController,
    onSceneBuild: (SceneBuild) -> Unit,
    modifier: Modifier = Modifier,
    rootScale: Float = StoryScenes.ROOT_SCALE,
) {
    val handles = remember { MiniatureHandles() }
    var track by remember { mutableStateOf<SceneTrack?>(null) }
    val progress = remember { Animatable(1f) }
    val preparingSince = remember { SystemClock.uptimeMillis() }
    val currentObject by rememberUpdatedState(objectId)
    val currentChapter by rememberUpdatedState(chapter)

    DisposableEffect(Unit) {
        onDispose { handles.destroyAll() }
    }

    // Keyed on reduceMotion too: switching it on mid-transition restarts this effect, which then snaps from the poses on
    // screen. When nothing is moving and the target is unchanged, the restart does nothing and logs nothing.
    LaunchedEffect(objectId, chapter, reduceMotion) {
        val diorama = StoryScenes.forObject(objectId) ?: return@LaunchedEffect
        val started = SystemClock.uptimeMillis()
        val plan = StoryMotion.plan(diorama, track, progress.value, objectId, chapter, reduceMotion)
        if (plan.kind == Transition.NONE) return@LaunchedEffect
        track = plan.track
        if (!handles.has(objectId)) {
            // Nothing of this object is on screen yet: `initial` builds it at the current chapter and logs its arrival.
            progress.snapTo(1f)
            return@LaunchedEffect
        }
        when (plan.kind) {
            Transition.ARRIVE -> {
                progress.snapTo(1f)
                logArrival(handles, controller, objectId, chapter)
            }
            Transition.SNAP -> {
                progress.snapTo(1f)
                controller.storySceneSettled(objectId, chapter, motion = "still", millis = SystemClock.uptimeMillis() - started)
            }
            Transition.ANIMATE -> {
                progress.snapTo(0f)
                progress.animateTo(1f, tween(durationMillis = StoryMotion.durationMillis(false), easing = PolishTokens.EasingStandard))
                controller.storySceneSettled(objectId, chapter, motion = "animated", millis = SystemClock.uptimeMillis() - started)
            }
            Transition.NONE -> Unit
        }
    }

    SpatialView(
        modifier = modifier,
        initial = { content, _ ->
            // Logged here, not from an effect, so it always precedes this attempt's ready or failed marker. The preparing
            // state itself has been composed since [preparingSince]; ready's `ms` counts from then.
            controller.storyScenePreparing(currentObject, currentChapter, attempt)
            // Factories can throw ResourceLoadingException; each miniature is built under its own guard.
            runCatching { ExhibitFactory.keyLight() }
                .onSuccess { light -> handles.light = light; content.addEntity(light) }
                .onFailure { Log.e(TAG, "story light not created", it) }
            var record = SceneBuild()
            // The story's own miniature first, so the preparing state can clear as soon as it exists.
            val order = StoryScenes.all.sortedBy { if (it.objectId == currentObject) 0 else 1 }
            order.forEachIndexed { index, diorama ->
                // Built at the story's current chapter, so a reopened window never flashes chapter 1 first.
                val chapterToBuild = if (diorama.objectId == currentObject) currentChapter else 0
                val result = runCatching { build(diorama, chapterToBuild, rootScale) }
                val built = result.getOrNull()
                if (handles.disposed) {
                    built?.root?.destroy(recursively = true)
                    return@SpatialView
                }
                val complete = built?.complete == true
                if (built != null && complete) {
                    built.root.enabled = diorama.objectId == currentObject
                    handles.dioramas[diorama.objectId] = built
                    content.addEntity(built.root)
                } else {
                    built?.root?.destroy(recursively = true)
                    Log.e(TAG, "story-build-failed id=${diorama.objectId}", result.exceptionOrNull())
                }
                handles.dioramas.forEach { (id, b) -> b.root.enabled = id == currentObject }
                record = record.record(diorama.objectId, complete)
                onSceneBuild(record)
                if (diorama.objectId == currentObject) {
                    when (record.status(currentObject)) {
                        SceneStatus.READY -> {
                            controller.storySceneReady(currentObject, currentChapter, attempt, SystemClock.uptimeMillis() - preparingSince)
                            logArrival(handles, controller, currentObject, currentChapter)
                        }
                        SceneStatus.FAILED -> controller.storySceneFailed(
                            currentObject, currentChapter, attempt,
                            reason = result.exceptionOrNull()?.javaClass?.simpleName ?: "incomplete",
                        )
                        SceneStatus.PREPARING -> Unit
                    }
                }
                // `initial` runs on Dispatchers.Main, where yield() only re-posts to the main Handler (no frame). Two
                // Choreographer frames: in the first, Compose applies the state written above and draws the region
                // without the preparing card; the second starts the next (blocking) build only after that frame.
                if (index < order.lastIndex) repeat(2) { awaitFrame() }
            }
            Log.i(TAG, "story-scene-built dioramas=${handles.dioramas.size}/${order.size} props=${handles.dioramas.values.sumOf { it.props.size }} attempt=$attempt")
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

/** Builds a diorama's tilted root (at [rootScale]) and its props at [chapter]; a prop whose mesh fails is skipped (null) rather than failing the scene. */
private fun build(diorama: Diorama, chapter: Int, rootScale: Float): BuiltDiorama {
    val root = Entity().apply {
        setName("story_${diorama.objectId}")
        components.get<TransformComponent>()?.apply {
            setPosition(Vector3(x = 0f, y = StoryScenes.ROOT_LIFT, z = 0f))
            setEulerAngles(EulerAngles(pitch = StoryScenes.TILT_DEGREES, yaw = 0f, roll = 0f))
            setScaleVector(Vector3(x = rootScale, y = rootScale, z = rootScale))
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
