package dev.local.everydayfutures.museum.content

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import com.pico.spatial.core.ecs.CollisionComponent
import com.pico.spatial.core.ecs.Entity
import com.pico.spatial.core.ecs.HoverEffectComponent
import com.pico.spatial.core.ecs.InteractableComponent
import com.pico.spatial.core.ecs.event.SceneEvents
import com.pico.spatial.core.ecs.resource.PhysicsMaterialResource
import com.pico.spatial.core.ecs.resource.ShapeResource
import com.pico.spatial.core.lifecycle.Cancellable
import com.pico.spatial.core.math.Vector3
import com.pico.spatial.ui.foundation.content.SpatialView
import com.pico.spatial.ui.foundation.gesture.TargetEntity
import com.pico.spatial.ui.foundation.gesture.detectSpatialDragGesture
import dev.local.everydayfutures.museum.catalogue.Artifact
import dev.local.everydayfutures.museum.state.MuseumController
import dev.local.everydayfutures.museum.state.Surface

private const val TAG = "Museum"

/** The `surface=` word in the inspector's `exhibit-load` / `exhibit-model` markers. */
private const val INSPECTOR = "inspector"

/** The smallest collider edge (metres), so a flat model still has something to hit. */
private const val MIN_COLLIDER = 0.01f

/**
 * The inspector's scene: one SpatialView for the life of the window, holding a key light and the model of the object
 * the inspector shows. `initial` cannot re-run, so it only captures the scene content, adds the light and subscribes
 * to the scene's frame events; the model is loaded and swapped from an effect keyed on the object and the attempt
 * (the same lazy loading, delayed loading card, Try again rule and markers as the study, with `surface=inspector`).
 *
 * The model is root > fit > loaded; the root carries a box collider over the fitted model plus Interactable and Hover
 * effect components, and takes the yaw, written before it joins the scene. Every later write (frame, drag, `update`)
 * names [targetId], read from app state together with the yaw: a new selection retargets it at once, before this
 * recomposes, so the previous object's root is hidden (`model-swap surface=inspector hidden=… for=…`) rather than
 * turned to the new object's front until the effect removes it — a hard cut, never a fade out. The new root joins the
 * scene transparent and fades in over [FadeIn.MODEL_MILLIS], as in the study ([fadeInRoot], `model-fade
 * surface=inspector`), or at full opacity at once under Reduce motion; the fade changes opacity only, so spin and drag
 * act on it as on any root.
 * Spin runs from `SceneEvents.Update` (about 20°/s, no recomposition per frame)
 * while app state says spinning (never under Reduce motion) and no drag is in progress; the frame reads app state
 * directly, so a stop takes effect on the very next frame rather than after a recomposition. It accumulates in [pose]
 * (one per object), and the toolbar folds it into app state on Pause, Turn, Reset and Close. Drag-to-rotate uses `detectSpatialDragGesture`
 * targeted at the root (px → dp → degrees) and commits on drag end. Drag data exists only on PICO OS and is not
 * established on the emulator; the toolbar buttons are the complete route. Everything is destroyed, and the frame
 * subscription cancelled, when this leaves composition.
 */
@Composable
internal fun InspectorPresenter(
    artifact: Artifact,
    controller: MuseumController,
    pose: InspectorPose,
    /** The object app state presents now, read outside composition; frame, drag and update writes only reach its root. */
    targetId: () -> String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val slot = remember { ModelSlot() }
    val frames = remember { FrameSubscription() }
    var sceneReady by remember { mutableStateOf(false) }
    var loadState by remember { mutableStateOf<ModelLoadState>(ModelLoadState.Idle) }
    var attempt by remember(artifact.id) { mutableIntStateOf(1) }
    // The frame and gesture callbacks are created once; they always act on the current object's pose.
    val livePose by rememberUpdatedState(pose)
    // Read outside composition (frame events, gesture callbacks), so they always see the latest committed state.
    val committedYaw = { controller.state.inspector?.yawDegrees ?: 0f }
    val spinningNow = { controller.state.let { it.inspector?.spinning == true && !it.options.reduceMotion } }
    // The one write path after attach: a root left from another object is hidden, never turned (logged when it hides).
    val writePose: (Float) -> Unit = { yaw ->
        val target = targetId()
        val stale = slot.rootId
        if (slot.applyPose(target, yaw)) stale?.let { controller.modelSwapHidden(INSPECTOR, it, target) }
    }

    DisposableEffect(Unit) {
        onDispose {
            frames.cancel()
            slot.destroyAll()
        }
    }

    LaunchedEffect(artifact.id, attempt, sceneReady) {
        if (!sceneReady || slot.content == null) return@LaunchedEffect
        val token = slot.nextToken()
        val loading = ModelLoadMachine.start(artifact.id, attempt, token)
        loadState = loading
        slot.clearRoot()
        pose.clear()
        controller.modelLoadStarted(artifact.id, INSPECTOR, loading.attempt)
        val loaded = ExhibitLoader.load(context, artifact, ExhibitBoxes.INSPECTOR, namePrefix = INSPECTOR)
        val skip = FadeIn.skipReason(controller.state.options.reduceMotion)
        val outcome = attachOrDiscard(slot, loaded, token, artifact.id, { loadState }, transparent = skip == null) { root ->
            addInputComponents(root, loaded.placed ?: ExhibitBoxes.INSPECTOR.bounds, artifact.id)
            root.writeYaw(pose.live(committedYaw())) // posed before it joins the scene, as the study does
        } ?: return@LaunchedEffect
        try {
            val next = ModelLoadMachine.complete(loadState, token, artifact.id, outcome) ?: return@LaunchedEffect
            loadState = next
            controller.modelShown(artifact.id, INSPECTOR, outcome.kind.marker, loaded.millis, outcome.reason)
            slot.fadeInRoot(controller, INSPECTOR, artifact.id, skip, targetId)
        } finally {
            // Also when a new object or the window closing cancels the fade: never left part-transparent.
            slot.endFade(loaded.root, targetId())
        }
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        SpatialView(
            // One recognizer per pointerInput block. The target is resolved per event, so it follows model swaps.
            modifier = Modifier.fillMaxSize().pointerInput(Unit) {
                detectSpatialDragGesture(
                    context = context,
                    targetedToEntity = TargetEntity.any { entity -> slot.owns(entity) },
                    onDragStart = { _, _ -> livePose.startDrag() },
                    onDragEnd = { _ -> controller.inspectorDragged(livePose.endDrag(committedYaw()), Surface.INSPECTOR) },
                    onDragCancel = { _ -> controller.inspectorDragged(livePose.endDrag(committedYaw()), Surface.INSPECTOR) },
                ) { value ->
                    livePose.dragBy(value.dragAmount.x.toDp().value)
                    writePose(livePose.live(committedYaw()))
                }
            },
            initial = { content, _ ->
                slot.content = content
                runCatching { ExhibitFactory.keyLight() }
                    .onSuccess { light -> slot.light = light; content.addEntity(light) }
                    .onFailure { Log.e(TAG, "inspector key light not created", it) }
                runCatching {
                    content.subscribe(SceneEvents.Update::class.java) { event ->
                        // A throw here must not reach the SDK's event dispatch.
                        try {
                            val current = livePose
                            current.advance(event.deltaTime, spinningNow())
                            writePose(current.live(committedYaw()))
                        } catch (error: Throwable) {
                            frames.failed(error)
                        }
                    }
                }.onSuccess { frames.set(it) }.onFailure { Log.e(TAG, "inspector frame events unavailable; spin is off", it) }
                sceneReady = true
            },
            // Commits (Turn, Reset, a new object) also land here, so the pose follows state even without frame events.
            update = { _, _ -> writePose(livePose.live(controller.state.inspector?.yawDegrees ?: 0f)) },
        )
        ModelStatusCard(state = loadState, artifact = artifact, onRetry = { next -> attempt = next })
    }
}

/** The frame-event subscription, cancelled on dispose, and a once-only log for a failing frame callback. */
private class FrameSubscription {
    private var subscription: Cancellable? = null
    private var failureLogged = false

    fun set(cancellable: Cancellable) {
        subscription = cancellable
    }

    /** Logs the first failure of the frame callback only (it would otherwise repeat every frame). */
    fun failed(error: Throwable) {
        if (failureLogged) return
        failureLogged = true
        Log.e(TAG, "inspector frame update failed", error)
    }

    fun cancel() {
        runCatching { subscription?.cancel() }
        subscription = null
    }
}

/** A box collider over the fitted model, in the root's frame (so it turns with the root), plus Interactable and Hover. */
private fun addInputComponents(root: Entity, placed: ModelBounds, id: String) {
    runCatching {
        val size = Vector3(
            x = (placed.maxX - placed.minX).coerceAtLeast(MIN_COLLIDER),
            y = (placed.maxY - placed.minY).coerceAtLeast(MIN_COLLIDER),
            z = (placed.maxZ - placed.minZ).coerceAtLeast(MIN_COLLIDER),
        )
        val centre = Vector3(
            x = (placed.minX + placed.maxX) / 2f,
            y = (placed.minY + placed.maxY) / 2f,
            z = (placed.minZ + placed.maxZ) / 2f,
        )
        root.components.set(CollisionComponent(listOf(ShapeResource.createBox(size).offsetByTranslation(centre)), PhysicsMaterialResource()))
        root.components.set(InteractableComponent())
        root.components.set(HoverEffectComponent())
    }.onFailure { Log.w(TAG, "inspector-input-components-failed id=$id; the toolbar still turns it", it) }
}
