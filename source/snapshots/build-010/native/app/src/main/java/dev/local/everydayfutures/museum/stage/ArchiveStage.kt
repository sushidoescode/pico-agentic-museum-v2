package dev.local.everydayfutures.museum.stage

import android.content.Context
import android.os.SystemClock
import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pico.spatial.core.container.SpatialViewContent
import com.pico.spatial.core.ecs.AttachmentPanelComponent
import com.pico.spatial.core.ecs.CollisionComponent
import com.pico.spatial.core.ecs.DirectionalLightComponent
import com.pico.spatial.core.ecs.Entity
import com.pico.spatial.core.ecs.HoverEffectComponent
import com.pico.spatial.core.ecs.InteractableComponent
import com.pico.spatial.core.ecs.ModelComponent
import com.pico.spatial.core.ecs.OpacityControllerComponent
import com.pico.spatial.core.ecs.TransformComponent
import com.pico.spatial.core.ecs.event.SceneEvents
import com.pico.spatial.core.ecs.resource.BlendingMode
import com.pico.spatial.core.ecs.resource.MeshResource
import com.pico.spatial.core.ecs.resource.PhysicsMaterialResource
import com.pico.spatial.core.ecs.resource.ResourceLoadingException.ResourceErrorCode
import com.pico.spatial.core.ecs.resource.ResourceLoadingException
import com.pico.spatial.core.ecs.resource.ShapeResource
import com.pico.spatial.core.ecs.resource.UnlitMaterial
import com.pico.spatial.core.lifecycle.Cancellable
import com.pico.spatial.core.math.Color4
import com.pico.spatial.core.math.EulerAngles
import com.pico.spatial.core.math.Quat
import com.pico.spatial.core.math.Vector3
import com.pico.spatial.tracking.DataProvider
import com.pico.spatial.tracking.hmd.HMDPose
import com.pico.spatial.tracking.hmd.HMDTrackingData
import com.pico.spatial.tracking.hmd.HMDTrackingProvider
import com.pico.spatial.ui.design.Text
import com.pico.spatial.ui.foundation.content.SpatialView
import com.pico.spatial.ui.foundation.gesture.TargetEntity
import com.pico.spatial.ui.foundation.gesture.detectSpatialTapGesture
import dev.local.everydayfutures.museum.R
import dev.local.everydayfutures.museum.catalogue.Artifact
import dev.local.everydayfutures.museum.catalogue.Catalogue
import dev.local.everydayfutures.museum.catalogue.CatalogueFilter
import dev.local.everydayfutures.museum.catalogue.CatalogueQuery
import dev.local.everydayfutures.museum.content.FadeIn
import dev.local.everydayfutures.museum.content.FitTarget
import dev.local.everydayfutures.museum.content.ModelBounds
import dev.local.everydayfutures.museum.content.ModelFit
import dev.local.everydayfutures.museum.content.ModelSource
import dev.local.everydayfutures.museum.content.rememberStageNavigator
import dev.local.everydayfutures.museum.content.realSizePhrase
import dev.local.everydayfutures.museum.content.rememberWindowNavigator
import dev.local.everydayfutures.museum.state.ArchivePhase
import dev.local.everydayfutures.museum.state.ArchiveReducer
import dev.local.everydayfutures.museum.state.ArchiveState
import dev.local.everydayfutures.museum.state.ArchiveViewpoint
import dev.local.everydayfutures.museum.state.MuseumController
import dev.local.everydayfutures.museum.state.MuseumState
import dev.local.everydayfutures.museum.state.Surface
import dev.local.everydayfutures.museum.state.WorkspaceReducer
import dev.local.picopolish.PolishButton
import dev.local.picopolish.PolishButtonTone
import dev.local.picopolish.PolishSurface
import dev.local.picopolish.PolishSurfaceKind
import dev.local.picopolish.PolishTokens
import dev.local.picopolish.PolishTypography
import java.io.FileNotFoundException
import java.util.Locale
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withTimeoutOrNull

private const val TAG = "Museum"
private const val PANEL_ID = "archive-panel"
private const val EXIT_ID = "archive-exit"
private const val ROOM_BUDGET_MS = 10_000L
private const val EXHIBIT_BUDGET_MS = 5_000L

/** Concurrent exhibit loads; the SDK's KDoc asks apps to bound concurrent model loading to the memory budget. */
private const val EXHIBIT_PERMITS = 2

/**
 * Labels fill their placard's clean area from the room layout ([ArchiveGeometry.labelScale]): a plaque label 300 x 100 dp
 * on a 0.20 x 0.075 m brass plate comes out at 0.75x (0.18 m wide); a bay title 560 x 105 dp on its 1.6 x 0.3 m wall
 * area at about 3.2x (1.44 m wide). Build-004 used fixed 0.5x / 1x, which left plaques at 60% of the plate and titles at
 * about 0.4 m on a 1.6 m wall. The fallbacks are used only if a layout gives no size.
 */
private const val PLAQUE_WIDTH_DP = 300f
private const val PLAQUE_HEIGHT_DP = 100f
private const val SIGN_WIDTH_DP = 560f
private const val SIGN_HEIGHT_DP = 105f
private const val PLAQUE_FALLBACK_SCALE = 0.5f
private const val SIGN_FALLBACK_SCALE = 1f

/**
 * The exit caption under the lit ring on the entrance door: a real button (its hover comes from the SDK Button) filling
 * its clean area on the leaf ([ArchiveTargets.exitLabelPlacard], 0.6 x 0.15 m), so about 1.9x.
 */
private const val EXIT_WIDTH_DP = 360f
private const val EXIT_HEIGHT_DP = 90f

/** The focused exhibit's 45° turn eases for this long unless Reduce motion is on: an app choice, to tune natively. */
private const val EXHIBIT_TURN_MILLIS = 400

/** Saved objects the Arrival panel names; the rest are counted. Three fit one row, like a bay's buttons. */
private const val SHORTLIST_SHOWN = 3

/** Engraved-looking text on the brass plates, and warm lettering on the slate walls; no chip behind either. */
private val PLAQUE_INK = Color(0xFF2A1E12)
private val SIGN_INK = Color(0xFFF2E7D3)

/** The floor circles: a pale warm ivory, unlit so the baked room light leaves it alone, paler than the room's brass. */
private val PAD_IVORY = Color4(red = 0.93f, green = 0.88f, blue = 0.78f, alpha = 1f)

private fun plaqueId(exhibitId: String) = "plaque-$exhibitId"

private fun signId(bay: String) = "sign-$bay"

/** What a tap in the room landed on: an exhibit (its `part` is `model` or `support`), its floor circle, or the door's exit. */
private sealed interface RoomTarget {
    data class Exhibit(val id: String, val part: String) : RoomTarget

    data class Pad(val id: String) : RoomTarget

    object Exit : RoomTarget
}

/** Survives recomposition so disposal destroys exactly what this visit created. */
private class ArchiveHandles {
    var world: Entity? = null
    val exhibits = LinkedHashMap<String, Entity>()

    /** Each exhibit's turn entity (between its root and fit), keyed like [exhibits]: what the room's 45° turn rotates. */
    val turntables = LinkedHashMap<String, Entity>()

    /** Invisible tap targets over each exhibit's support (riser, plinth, pedestal, lectern, platform), by exhibit id. */
    val supports = LinkedHashMap<String, Entity>()

    /** Each exhibit's floor circle ([ArchivePads]), by exhibit id: a step up to its close look. */
    val pads = LinkedHashMap<String, Entity>()

    /** The exhibit whose circle is hidden (the one in focus, as last written); that circle is no tap target. */
    var padHidden: String? = null

    /** The tap target over the lit ring on the entrance door. */
    var exit: Entity? = null

    /** The exhibit whose turn the animation last showed: a change of focus snaps, a step on the same one eases. */
    var turnFor: String? = null

    /** The door's ring or caption has asked to leave; a second tap before Return has run is ignored. */
    var exiting = false

    /** The persistent panel (the SDK's attachment, a child of the scene, not the room) and its last written pose. */
    var panel: Entity? = null
    var panelPose: WorldPose? = null

    /** The head frame the room and the panel are shown in: adopted on in-room tap arrivals, kept on every other move. */
    val arrivals = ArrivalFrames()

    /**
     * The world's fade-in and the arrival ring (a child of the world) after a move, and the frame subscription that plays
     * both, cancelled first on dispose.
     */
    var fade: RoomFade? = null
    var cue: ArrivalRing? = null
    var frames: Cancellable? = null

    /** Label attachment entities parented to the room; they belong to the SDK, so they are detached, never destroyed. */
    val labels = mutableListOf<Entity>()
    var disposed = false
    var created = 0

    /** Every entity this visit created (the world and its children), kept to confirm they are invalid after the destroy. */
    val tracked = mutableListOf<Entity>()

    /** Registers a child the visit created and parented to the world. */
    fun createdChild(entity: Entity) {
        created += 1
        tracked += entity
    }

    /** What a tapped entity stands for, found by walking up its parents; null for anything else in the room. */
    fun targetOf(entity: Entity): RoomTarget? {
        var e: Entity? = entity
        while (e != null) {
            val match = e
            if (match == exit) return RoomTarget.Exit
            exhibits.entries.firstOrNull { it.value == match }?.let { return RoomTarget.Exhibit(it.key, "model") }
            supports.entries.firstOrNull { it.value == match }?.let { return RoomTarget.Exhibit(it.key, "support") }
            // Whether a disabled entity's collider still takes hits is not documented, so a hidden circle never counts.
            pads.entries.firstOrNull { it.value == match }?.let { return if (it.key == padHidden) null else RoomTarget.Pad(it.key) }
            e = e.getParent()
        }
        return null
    }

    /**
     * One recursive destroy of the world releases the room, the exhibits, the tap targets, the floor circles, the arrival
     * ring and the lights (the panel entity is the SDK's), and with it any fade still playing. The frame subscription is
     * cancelled first, so no frame reaches the fade or the ring as they go. Returns the number of children the visit
     * created and how many of the tracked entities (those children and the world) report `valid == false` afterwards: a
     * confirmation of the entity destroy, not of every native resource behind it.
     */
    fun destroyAll(): DisposeCount {
        runCatching { frames?.cancel() }
        frames = null
        fade = null
        cue = null
        disposed = true
        val n = created
        labels.forEach { label -> runCatching { label.removeFromParent() } }
        labels.clear()
        runCatching { world?.destroy(recursively = true) }.onFailure { Log.e(TAG, "archive-dispose world not destroyed", it) }
        val invalid = tracked.count { entity -> runCatching { !entity.valid }.getOrDefault(false) }
        val count = DisposeCount(created = n, tracked = tracked.size, invalidAfter = invalid)
        tracked.clear()
        world = null
        exhibits.clear()
        turntables.clear()
        supports.clear()
        pads.clear()
        padHidden = null
        exit = null
        turnFor = null
        panel = null
        panelPose = null
        created = 0
        return count
    }
}

/** What a disposal created and confirmed: [invalidAfter] of [tracked] entities report invalid after the destroy. */
private data class DisposeCount(val created: Int, val tracked: Int, val invalidAfter: Int)

/**
 * The archive visit (L4), the content of the `EverydayFuturesArchive` Stage: the Blender room at the Stage origin, each
 * exhibit's own model on its anchor, two lights for the exhibits (the room's light is baked), and a persistent panel
 * that stays with the visitor — loading state, the bays, the current bay's six objects, the shortlist at Arrival, a
 * close look, and Return to catalogue, which is always the first control and always enabled. An exhibit is tapped on its
 * model or its support (plinth, riser, pedestal, lectern, platform): the first tap focuses it, a tap on the exhibit in
 * focus turns it 45°. Each exhibit also has a floor circle in front of its support ([ArchivePads]): a pinch or click on
 * it steps up to that exhibit's close look exactly as a first tap on the exhibit does, and never turns it; the circle of
 * the exhibit in focus is hidden. The lit ring on the entrance door, and the caption under it, also return to the
 * catalogue. A viewpoint or a close look moves the room root: an instant cut, never an animated ride. A tap that focuses
 * another exhibit reads the visitor's head ([HeadSource]) and shows the close look ahead of where they now face, with
 * the panel front-left of that heading; the panel's own buttons keep the last frame, so the panel does not move under
 * the visitor's hand, and with no head reading every pose is the Stage-origin one. After a move the world (the room,
 * exhibits and floor circles) fades in from transparent over [FadeIn.ROOM_MILLIS] ([RoomFade]; the move itself is still
 * the cut, and the panel never fades), then a brass ring round the visitor's feet widens and fades ([ArrivalCue]).
 * These and the 45° turn's ease are the only motion here, and Reduce motion removes all three (no fade, no ring, the
 * turn snaps). The primary window is minimised once the Stage is up and restored before the Stage closes; everything
 * this visit created is destroyed when the Stage's composition leaves, so repeated visits cannot accumulate scenes.
 * Markers: `archive-layout`, `archive-primary`, `archive-head`, `archive-load start|settled`,
 * `archive-room loaded`, `archive-targets`, `archive-pads count=`, `archive-exhibit id= result=`, `archive-tap id=
 * part=model|support|pad kind=`, `archive-stand frame=`, `archive-fade play|skipped`, `archive-cue`, `archive-exit via=`,
 * `archive-dispose` (and the controller's `archive-focus`, `archive-turn`).
 */
@Composable
fun ArchiveStage(controller: MuseumController) {
    val context = LocalContext.current
    val stage = rememberStageNavigator()
    val windows = rememberWindowNavigator()
    val scope = rememberCoroutineScope()
    val handles = remember { ArchiveHandles() }
    val layout = remember { RoomLayoutParser.load(context) }
    var load by remember { mutableStateOf(ArchiveLoad.start(Catalogue.artifacts.map { it.id }, layout, SystemClock.uptimeMillis())) }
    val head = remember { HeadSource.create() }

    DisposableEffect(Unit) {
        Log.i(TAG, "archive-layout ok=${layout != null} models=${layout?.roomModels?.size ?: 0} anchors=${layout?.anchors?.size ?: 0} viewpoints=${layout?.viewpoints?.keys?.sorted()} exit=${layout?.exitMarker != null}")
        onDispose {
            val disposed = handles.destroyAll()
            // Return restores the primary before closing the Stage. This later call can only succeed while a Stage is still
            // open, which is never the case here: every recorded disposal (builds 001-006) logs restoredPrimary=false. It is
            // kept as a harmless last resort. When the system takes the visitor out of the archive (Home -> "Back to Home"),
            // no disposal happens at all: on resuming, the platform shows the primary again, the Stage stays hidden and the
            // catalogue offers "Return from archive", which closes it (observed on build-006, 2026-09-27).
            val restored = runCatching { stage.restore(WorkspaceReducer.MAIN_ID, null) }.getOrDefault(false)
            // `created` counts what the visit made (room, exhibits, tap targets, floor circles, lights, arrival ring);
            // `invalidAfter` confirms the entity destroy of those and the world. Neither measures native memory (see
            // evidence/demo-hardening-001).
            Log.i(TAG, "archive-dispose created=${disposed.created} invalidAfter=${disposed.invalidAfter}/${disposed.tracked} restoredPrimary=$restored")
        }
    }

    // Head tracking runs only while this Stage does (the provider needs the Full Space and must be stopped).
    DisposableEffect(head) {
        Log.i(TAG, "archive-head start=${head.start()} support=${head.support()}")
        onDispose { head.stop() }
    }

    // SpatialView calls `update` only after `initial` has returned, and `initial` waits for the room and every exhibit;
    // this moves the room as soon as a viewpoint or close look changes, loading or not (review finding, 2026-09-27).
    LaunchedEffect(controller.state.archive) {
        val world = handles.world ?: return@LaunchedEffect
        if (!handles.disposed && world.valid) applyStand(world, controller.state, layout, handles)
    }

    // minimizeWindowContainer only works while a Stage is open; this composition is the earliest point it can succeed.
    LaunchedEffect(Unit) {
        var attempts = 0
        var ok = false
        while (!ok && attempts < 12) {
            attempts += 1
            ok = runCatching { stage.minimize(WorkspaceReducer.MAIN_ID, null) }.getOrDefault(false)
            if (!ok) delay(250)
        }
        Log.i(TAG, "archive-primary minimized=$ok attempts=$attempts")
    }

    // The focused exhibit's turn, written to its turn entity from here rather than from `update`, so it also shows while
    // `initial` is still loading. A new focus arrives with the room's instant cut, so it snaps; a step on the same exhibit
    // eases, or snaps under Reduce motion (a key, so switching it mid-turn snaps too). Every other exhibit faces front.
    val turnYaw = remember { Animatable(0f) }
    val focusedId = controller.state.archive.focusedId
    val turnDegrees = controller.state.archive.turnDegrees
    val reduceMotion = controller.state.options.reduceMotion
    LaunchedEffect(focusedId, turnDegrees, reduceMotion) {
        if (focusedId != handles.turnFor) {
            handles.turnFor = focusedId
            turnYaw.snapTo(turnDegrees)
        } else {
            turnYaw.animateTo(turnDegrees, PolishTokens.motion(reduceMotion, EXHIBIT_TURN_MILLIS)) {
                if (!handles.disposed) applyTurns(handles, focusedId, value)
            }
        }
        if (!handles.disposed) applyTurns(handles, focusedId, turnYaw.value)
    }

    // The focused exhibit's floor circle is hidden, as the visitor now stands at its close look, and shows again when the
    // focus moves: an instant switch, nothing eases. Leaving changes nothing ([showPads]).
    val phase = controller.state.archive.phase
    LaunchedEffect(focusedId, phase) {
        if (!handles.disposed) showPads(handles, controller.state.archive)
    }

    val returnToCatalogue: () -> Unit = {
        scope.launch {
            val restored = runCatching { stage.restore(WorkspaceReducer.MAIN_ID, null) }.getOrDefault(false)
            Log.i(TAG, "archive-primary restored=$restored")
            runCatching { controller.leaveArchive(stage, Surface.ARCHIVE) }
                .onFailure { if (it is CancellationException) throw it else Log.e(TAG, "archive-close failed", it) }
        }
    }

    // The door's ring and caption leave like the panel's Return, but only while OPEN and only once: a tap during CLOSING,
    // or a second tap before Return has run, must not close the Stage twice.
    val exitFromRoom: (String) -> Unit = { via ->
        if (controller.state.archive.phase == ArchivePhase.OPEN && !handles.exiting) {
            handles.exiting = true
            Log.i(TAG, "archive-exit via=$via")
            returnToCatalogue()
        }
    }

    // Only a tap that focuses another exhibit (or focuses from a bay or Arrival) re-faces the visitor: the head is read
    // now, before the move [act] makes, and offered to the arrival it causes. A tap on the exhibit in focus turns it and
    // reads nothing (its floor circle is hidden and no target).
    val tapInRoom: (String, () -> Unit) -> Unit = { id, act ->
        val anew = ArchiveReducer.wouldFocusAnew(controller.state, id)
        if (anew) {
            val sample = head.sample()
            Log.i(TAG, "archive-head ${sample.detail}")
            handles.arrivals.offer(id, sample.frame)
        }
        act()
        if (anew && controller.state.archive.focusedId != id) handles.arrivals.drop()
    }

    SpatialView(
        modifier = Modifier.fillMaxSize().pointerInput(Unit) {
            detectSpatialTapGesture(context, targetedToEntity = TargetEntity.any { handles.targetOf(it) != null }) { tap ->
                // How the tap arrived (the SDK's InteractionKind: gazepinch, raybasedpinch, pointer…), for the native check.
                val kind = tap.interactionKind.name.lowercase()
                when (val target = tap.targetEntity?.let { handles.targetOf(it) } ?: return@detectSpatialTapGesture) {
                    RoomTarget.Exit -> exitFromRoom("marker")
                    is RoomTarget.Exhibit -> {
                        Log.i(TAG, "archive-tap id=${target.id} part=${target.part} kind=$kind")
                        tapInRoom(target.id) {
                            controller.archiveTap(target.id, onDisplay = handles.turntables.containsKey(target.id), Surface.ARCHIVE)
                        }
                    }
                    // A floor circle steps up to its exhibit exactly as a first tap on the exhibit does; it never turns it.
                    is RoomTarget.Pad -> {
                        Log.i(TAG, "archive-tap id=${target.id} part=pad kind=$kind")
                        tapInRoom(target.id) { controller.archiveStep(target.id, Surface.ARCHIVE) }
                    }
                }
            }
        },
        attachments = {
            AttachmentPanel(id = PANEL_ID, alignment = AttachmentPanelComponent.Alignment.TOP_CENTER) {
                ArchivePanel(
                    controller = controller,
                    layout = layout,
                    load = load,
                    onReturn = returnToCatalogue,
                    onOpen3d = { controller.openInspector(windows, Surface.ARCHIVE) },
                )
            }
            // App-owned labels in the room: never baked into textures, so they stay proofread copy and follow the registry.
            layout?.anchors?.forEach { anchor ->
                val item = Catalogue.byId(anchor.id)
                if (anchor.plaque != null && item != null) {
                    AttachmentPanel(id = plaqueId(anchor.id)) { PlaqueLabel(item) }
                }
            }
            layout?.signs?.keys?.forEach { bay ->
                AttachmentPanel(id = signId(bay)) { BayTitle(bay) }
            }
            // The lit ring on the entrance door is an exit; the caption under it says so in the panel's own words.
            if (layout?.exitMarker != null) {
                AttachmentPanel(id = EXIT_ID) {
                    ExitCaption(enabled = controller.state.archive.phase == ArchivePhase.OPEN, onExit = { exitFromRoom("label") })
                }
            }
        },
        initial = { content, attachments ->
            // The panel first, so Return is on screen before any model loads.
            attachments.entity(PANEL_ID)?.let { panel ->
                handles.panel = panel
                placePanel(handles, ArchiveGeometry.panelPose(handles.arrivals.frame))
                content.addEntity(panel)
            } ?: Log.e(TAG, "archive-panel missing")
            val world = Entity().apply { setName("archive_world") }
            handles.world = world
            handles.tracked += world
            content.addEntity(world)
            applyStand(world, controller.state, layout, handles)
            layout?.anchors?.forEach { anchor ->
                val plaque = anchor.plaque ?: return@forEach
                val scale = ArchiveGeometry.labelScale(plaque, PLAQUE_WIDTH_DP, PLAQUE_HEIGHT_DP, PLAQUE_FALLBACK_SCALE)
                attachments.entity(plaqueId(anchor.id))?.let { placeLabel(it, plaque, scale, world, handles) }
            }
            layout?.signs?.forEach { (bay, sign) ->
                val scale = ArchiveGeometry.labelScale(sign, SIGN_WIDTH_DP, SIGN_HEIGHT_DP, SIGN_FALLBACK_SCALE)
                attachments.entity(signId(bay))?.let { placeLabel(it, sign, scale, world, handles) }
            }
            layout?.exitMarker?.let { marker ->
                val caption = ArchiveTargets.exitLabelPlacard(marker)
                val scale = ArchiveGeometry.labelScale(caption, EXIT_WIDTH_DP, EXIT_HEIGHT_DP, SIGN_FALLBACK_SCALE)
                attachments.entity(EXIT_ID)?.let { placeLabel(it, caption, scale, world, handles) }
            }
            for ((name, euler, lux) in listOf(
                Triple("archive_key", EulerAngles(pitch = -55f, yaw = 20f, roll = 0f), 2600f),
                Triple("archive_fill", EulerAngles(pitch = -25f, yaw = 200f, roll = 0f), 900f),
            )) {
                runCatching {
                    Entity().apply {
                        setName(name)
                        components.set(DirectionalLightComponent(Color4(red = 1f, green = 0.95f, blue = 0.88f, alpha = 1f), lux))
                        components.get<TransformComponent>()?.setEulerAngles(euler)
                    }
                }.onSuccess { world.addChild(it); handles.createdChild(it) }.onFailure { Log.e(TAG, "archive-light-failed $name", it) }
            }
            if (layout == null) return@SpatialView
            addMoveEffects(content, world, controller, handles)
            Log.i(TAG, "archive-load start room=${layout.roomModels.size} exhibits=${load.total}")
            // The room alone first (largest), then the exhibits with bounded concurrency. All on Main; loads suspend.
            var roomOk = true
            for (path in layout.roomModels) {
                val started = SystemClock.uptimeMillis()
                val room = loadOrNull(context, "asset://$path", ROOM_BUDGET_MS, "room")
                if (handles.disposed) { room?.destroy(recursively = true); return@SpatialView }
                if (room == null) { roomOk = false; continue }
                room.setName("archive_room")
                if (!world.addChild(room)) { room.destroy(recursively = true); roomOk = false; continue }
                handles.createdChild(room)
                val b = room.getVisualBounds(relativeTo = world, recursive = true, enabledOnly = false)
                Log.i(TAG, "archive-room loaded path=$path ms=${SystemClock.uptimeMillis() - started} min=${b.min} max=${b.max}")
            }
            load = load.roomDone(roomOk, SystemClock.uptimeMillis())
            // The supports and the door's ring are baked into the room without colliders: invisible targets on the world,
            // so they move with every viewpoint and close look like the room itself. So do the floor circles, of which the
            // focused exhibit's starts hidden.
            if (roomOk && !handles.disposed && world.valid) {
                addRoomTargets(world, layout, handles)
                addPads(world, layout, handles)
                showPads(handles, controller.state.archive)
            }
            val gate = Semaphore(EXHIBIT_PERMITS)
            coroutineScope {
                for (anchor in layout.anchors) {
                    val artifact = Catalogue.byId(anchor.id) ?: continue
                    launch {
                        val started = SystemClock.uptimeMillis()
                        val model = gate.withPermit { loadOrNull(context, artifact.model.uri, EXHIBIT_BUDGET_MS, anchor.id) }
                        if (handles.disposed || !world.valid) { model?.destroy(recursively = true); return@launch }
                        val placed = model?.let { m ->
                            runCatching { place(m, anchor, artifact) }
                                .onFailure { Log.e(TAG, "archive-place-failed id=${anchor.id}", it) }
                                .getOrNull()
                        }
                        val ok = placed != null && world.addChild(placed.root)
                        if (!ok) {
                            (placed?.root ?: model)?.destroy(recursively = true)
                        } else {
                            handles.exhibits[anchor.id] = placed!!.root
                            handles.turntables[anchor.id] = placed.turn
                            handles.createdChild(placed.root)
                            // An exhibit that arrives while it is in focus shows its current turn.
                            applyTurns(handles, handles.turnFor, turnYaw.value)
                        }
                        load = load.exhibitDone(anchor.id, ok, SystemClock.uptimeMillis())
                        Log.i(TAG, "archive-exhibit id=${anchor.id} result=${if (ok) "ok" else "failed"} ms=${SystemClock.uptimeMillis() - started}")
                    }
                }
            }
            Log.i(TAG, "archive-load settled room=${load.room.name.lowercase()} ready=${load.ready}/${load.total} failed=${load.failed} ms=${load.allMillis}")
        },
        update = { _, _ ->
            val world = handles.world ?: return@SpatialView
            if (world.valid) applyStand(world, controller.state, layout, handles)
        },
    )
}

/**
 * Writes the room root's absolute pose for the current viewpoint or close look, and the panel's, in the head frame the
 * visit holds ([ArrivalFrames]): idempotent, an instant cut. A move (a new stand, never the first placement) logs where
 * its frame came from, fades the world in ([RoomFade]) and then plays the arrival ring, unless Reduce motion is on.
 */
private fun applyStand(world: Entity, state: MuseumState, layout: RoomLayout?, handles: ArchiveHandles) {
    val archive = state.archive
    // Leaving is not a move: CLOSED is committed while this composition is still alive (closeStage does not wait),
    // and its Arrival reset must not log a stand, fade the room, play the ring or snap the room during the close.
    if (archive.phase == ArchivePhase.CLOSING || archive.phase == ArchivePhase.CLOSED) return
    val stand = standFor(archive, layout) ?: return
    val source = handles.arrivals.arrive(stand, archive.focusedId)
    // The world turns transparent before its new pose is written, so the new stand is never shown at full opacity first.
    val faded = source != null && startFade(handles, state.options.reduceMotion)
    val frame = handles.arrivals.frame
    val pose = ArchiveGeometry.worldPoseFor(stand, frame)
    world.components.get<TransformComponent>()?.apply {
        setPosition(Vector3(x = pose.position.x, y = pose.position.y, z = pose.position.z))
        setEulerAngles(EulerAngles(pitch = 0f, yaw = pose.yawDegrees, roll = 0f))
    }
    placePanel(handles, ArchiveGeometry.panelPose(frame))
    source ?: return
    Log.i(TAG, "archive-stand frame=${source.word} heading=${frame?.let { Math.round(it.yawDegrees) } ?: "none"}")
    if (faded) {
        Log.i(TAG, "archive-fade play ms=${FadeIn.ROOM_MILLIS}")
    } else {
        Log.i(TAG, "archive-fade skipped reason=${FadeIn.skipReason(state.options.reduceMotion) ?: "unavailable"}")
    }
    val ring = handles.cue
    when {
        !ArrivalCue.shouldPlay(state.options.reduceMotion, moved = true) -> {
            ring?.stop()
            Log.i(TAG, "archive-cue skipped reason=reduce-motion")
        }
        ring == null -> Log.i(TAG, "archive-cue skipped reason=unavailable")
        else -> {
            // It shows once the room is back at full opacity, not at a fraction of its strength during the fade.
            val delay = ArrivalCue.delaySeconds(roomFades = faded)
            runCatching { ring.start(stand.position, delay) }
                .onSuccess { Log.i(TAG, "archive-cue play delayMs=${Math.round(delay * 1000f)}") }
                .onFailure { handles.cue = null; Log.e(TAG, "archive-cue failed; cue off", it) }
        }
    }
}

/**
 * Starts the world's fade-in for a move, or restarts it from transparent when a move comes mid-fade. False, with the
 * world at full opacity, under Reduce motion, when this visit has no fade (no frame events) or when it cannot start; a
 * fade that fails is switched off for the visit.
 */
private fun startFade(handles: ArchiveHandles, reduceMotion: Boolean): Boolean {
    val fade = handles.fade ?: return false
    if (!FadeIn.shouldPlay(reduceMotion)) {
        runCatching { fade.finish() }
        return false
    }
    return runCatching { fade.start() }
        .onFailure { error ->
            handles.fade = null
            runCatching { fade.finish() }
            Log.e(TAG, "archive-fade failed; fade off", error)
        }
        .isSuccess
}

/** Writes the panel's top-centre pose when it changed (it moves only with a new head frame). */
private fun placePanel(handles: ArchiveHandles, pose: WorldPose) {
    val panel = handles.panel ?: return
    if (pose == handles.panelPose) return
    panel.components.get<TransformComponent>()?.apply {
        setPosition(Vector3(x = pose.position.x, y = pose.position.y, z = pose.position.z))
        setEulerAngles(EulerAngles(pitch = 0f, yaw = pose.yawDegrees, roll = 0f))
        setScaleVector(Vector3(x = ArchiveGeometry.PANEL_SCALE, y = ArchiveGeometry.PANEL_SCALE, z = ArchiveGeometry.PANEL_SCALE))
    }
    handles.panelPose = pose
}

/**
 * The visitor's head, read once at an in-room tap that focuses another exhibit, from the SDK's HMD tracking (a Full
 * Space provider, started and stopped with this Stage). Its pose is documented only in "global coordinate space": that
 * this is the Stage frame the room and the panel are placed in is a hypothesis (H1), logged as `archive-head` for the
 * native check, not established. Null-safe throughout: no provider, a provider not STARTED or not SUPPORTED, no data yet
 * or no usable heading give no frame, and the room keeps the one it has (the Stage origin facing −Z until a tap supplies
 * one). Main thread; `latestData` is an immutable value the tracking thread replaces, so a read is a whole pose, at most
 * slightly stale. No permission is named for it in the SDK (eye tracking names one); not verified on the device.
 */
private class HeadSource(private val provider: HMDTrackingProvider?) {
    /** The provider's value before any data has arrived (HMDTrackingProvider.kt:38-39): identity pose, timestamp 0. */
    private val unset by lazy { HMDTrackingData(HMDPose(Vector3.ZERO, Quat.identity()), 0L) }

    /** `SUCCESS` or `PENDING` (it then starts by itself once supported), or why there is nothing to start. */
    fun start(): String =
        provider?.let { p -> runCatching { p.start().name }.getOrElse { "error-${it.javaClass.simpleName}" } } ?: "unavailable"

    fun support(): String =
        provider?.let { p -> runCatching { p.supportState.name }.getOrElse { "error-${it.javaClass.simpleName}" } } ?: "unavailable"

    fun stop() {
        runCatching { provider?.stop() }
    }

    /** One reading now: a frame and its `yaw= x= y= z=` detail, or no frame and `none reason=…`. */
    fun sample(): HeadSample {
        val p = provider ?: return HeadSample(null, "none reason=unavailable")
        return runCatching { read(p) }.getOrElse { HeadSample(null, "none reason=error-${it.javaClass.simpleName}") }
    }

    private fun read(p: HMDTrackingProvider): HeadSample {
        val state = p.state
        if (state != DataProvider.State.STARTED) return HeadSample(null, "none reason=state-${state.name.lowercase()}")
        val support = p.supportState
        if (support != DataProvider.SupportState.SUPPORTED) return HeadSample(null, "none reason=support-${support.name.lowercase()}")
        val data = p.latestData
        if (data == unset) return HeadSample(null, "none reason=no-data")
        val pose = data.hmdPose
        val at = pose.position
        // The head looks along its −Z; its +X is its right (an assumption the turn-left check on the device confirms).
        val forward = pose.rotation.rotateVector(Vector3(x = 0f, y = 0f, z = -1f))
        val right = pose.rotation.rotateVector(Vector3(x = 1f, y = 0f, z = 0f))
        val frame = ArchiveGeometry.headFrame(
            Point3(at.x, at.y, at.z),
            Point3(forward.x, forward.y, forward.z),
            Point3(right.x, right.y, right.z),
        ) ?: return HeadSample(null, "none reason=no-heading")
        return HeadSample(frame, String.format(Locale.US, "yaw=%.1f x=%.2f y=%.2f z=%.2f", frame.yawDegrees, at.x, at.y, at.z))
    }

    companion object {
        /** The provider needs the platform's tracking service; when it cannot be made, the room keeps today's poses. */
        fun create(): HeadSource = runCatching { HeadSource(HMDTrackingProvider()) }.getOrElse {
            Log.w(TAG, "archive-head unavailable reason=${it.javaClass.simpleName}")
            HeadSource(null)
        }
    }
}

/** One head reading: the frame, if any, and the detail for the `archive-head` marker. */
private class HeadSample(val frame: HeadFrame?, val detail: String)

/**
 * The move effects once the world is up: the frame subscription that plays them first, then the world's fade-in
 * ([RoomFade]) and the arrival ring ([ArrivalCue]; a child of the world, counted and destroyed with it). The fade only
 * starts, and the ring is only ever enabled, by a move while the subscription stands, so a missing frame event can never
 * leave the room transparent or the ring on screen; without the subscription there is neither, and a move is the plain
 * cut. Each frame advances them under separate guards: a failing one is switched off for the visit, not the other.
 */
private fun addMoveEffects(content: SpatialViewContent, world: Entity, controller: MuseumController, handles: ArchiveHandles) {
    runCatching {
        content.subscribe(SceneEvents.Update::class.java) { event ->
            val reduceMotion = controller.state.options.reduceMotion
            // A throw here must not reach the SDK's event dispatch; a failing fade ends at full opacity.
            try {
                handles.fade?.advance(event.deltaTime, reduceMotion)
            } catch (error: Throwable) {
                handles.fade?.let { fade -> runCatching { fade.finish() } }
                handles.fade = null
                Log.e(TAG, "archive-fade frame failed; fade off", error)
            }
            try {
                handles.cue?.advance(event.deltaTime, reduceMotion)
            } catch (error: Throwable) {
                handles.cue?.let { ring -> runCatching { ring.stop() } }
                handles.cue = null
                Log.e(TAG, "archive-cue frame failed; cue off", error)
            }
        }
    }.onSuccess { handles.frames = it }.onFailure { Log.w(TAG, "archive-cue frames unavailable; cue and fade off", it) }
    if (handles.frames == null) return
    handles.fade = RoomFade(world)
    runCatching { ArrivalRing.create() }
        .onSuccess { ring ->
            if (world.addChild(ring.entity)) {
                handles.createdChild(ring.entity)
                handles.cue = ring
            } else {
                ring.entity.destroy(recursively = true)
            }
        }
        .onFailure { Log.w(TAG, "archive-cue unavailable reason=${it.javaClass.simpleName}") }
}

/**
 * The world's fade-in after a move ([FadeIn.ROOM_MILLIS]): an [OpacityControllerComponent] on the world root, whose
 * opacity the SDK documents as multiplied into every descendant's — the room, the exhibits, the floor circles and the
 * ring (whether it also reaches the label attachments parented here is not established). The panel is the scene's
 * child, not the world's, so it never fades. Played by the frame events, like the ring. Every end (the fade over, Reduce
 * motion, a failure) removes the component (see [finish]), so the room at rest renders as with no fade at
 * all; whether a part-transparent room sorts cleanly is for the native check. Main thread.
 */
private class RoomFade(private val world: Entity) {
    /** Milliseconds since it started; negative while it is not playing. */
    private var elapsed = -1f

    /** Starts from fully transparent, or starts again there when a new move comes mid-fade. */
    fun start() {
        val opacity = world.components.get<OpacityControllerComponent>()
        if (opacity == null) world.components.set(OpacityControllerComponent(0f)) else opacity.opacity = 0f
        elapsed = 0f
    }

    /** One frame: eases the world in, and ends the fade once it is over or as soon as Reduce motion is on. */
    fun advance(deltaSeconds: Float, reduceMotion: Boolean) {
        if (elapsed < 0f) return
        elapsed += deltaSeconds * 1000f
        if (reduceMotion || FadeIn.isOver(elapsed, FadeIn.ROOM_MILLIS)) {
            finish()
        } else {
            world.components.get<OpacityControllerComponent>()?.opacity = FadeIn.opacityAt(elapsed, FadeIn.ROOM_MILLIS)
        }
    }

    /**
     * Full opacity and no component; safe at any time, and nothing to do when no fade has left one. Removing the component
     * alone restores full opacity: an opacity write here would be batched inside the frame callback and its end-of-frame
     * flush pushes the removed component back natively (System.componentJniBatching), so none is written unless the
     * removal itself fails.
     */
    fun finish() {
        elapsed = -1f
        if (!world.valid) return
        if (world.components.get<OpacityControllerComponent>() == null) return
        runCatching { world.components.remove(OpacityControllerComponent::class.java) }
            .onFailure { world.components.get<OpacityControllerComponent>()?.opacity = 1f }
    }
}

/**
 * The ring's entity: a thin unlit brass torus flat on the room's floor, transparent, its opacity on an
 * [OpacityControllerComponent]; no collider, so never a tap target. Disabled except while it shows. Main thread.
 */
private class ArrivalRing(val entity: Entity) {
    private var playing = false

    /** Seconds since it showed; negative while it still waits ([ArrivalCue.delaySeconds]). */
    private var elapsed = 0f

    /** Whether it is enabled, as last written (tracked here, not read back). */
    private var shown = false

    /** Plays the ring once round the room point [at] (the stand, under the visitor's feet), [delaySeconds] from now. */
    fun start(at: Point3, delaySeconds: Float) {
        val first = ArrivalCue.at(0f) ?: return
        entity.components.get<TransformComponent>()?.setPosition(Vector3(x = at.x, y = ArrivalCue.LIFT, z = at.z))
        write(first)
        elapsed = -delaySeconds.coerceAtLeast(0f)
        shown = elapsed >= 0f
        entity.enabled = shown
        playing = true
    }

    /** One frame: holds the ring back while it waits, then eases it on; stops it once over, or as soon as Reduce motion is on. */
    fun advance(deltaSeconds: Float, reduceMotion: Boolean) {
        if (!playing) return
        if (reduceMotion) {
            stop()
            return
        }
        elapsed += deltaSeconds
        if (elapsed < 0f) return
        val frame = ArrivalCue.at(elapsed)
        if (frame == null) {
            stop()
            return
        }
        write(frame)
        if (!shown) {
            entity.enabled = true
            shown = true
        }
    }

    fun stop() {
        playing = false
        shown = false
        entity.enabled = false
        entity.components.get<OpacityControllerComponent>()?.opacity = 0f
    }

    private fun write(frame: ArrivalCue.Frame) {
        val s = frame.radius / ArrivalCue.MESH_OUTER
        entity.components.get<TransformComponent>()?.setScaleVector(Vector3(x = s, y = 1f, z = s))
        entity.components.get<OpacityControllerComponent>()?.opacity = frame.opacity
    }

    companion object {
        /** The room's brass (the stand-ins' palette), unlit so the baked room light does not change it. */
        private val BRASS = Color4(red = 0.85f, green = 0.73f, blue = 0.54f, alpha = 1f)

        fun create(): ArrivalRing {
            val material = UnlitMaterial.create(BlendingMode.TRANSPARENT).apply {
                setBaseColor(BRASS)
                // It lies just above the floor and its inlays; not writing depth keeps it from cutting into them.
                setDepthWrite(false)
            }
            val ring = Entity().apply {
                setName("archive_arrival_cue")
                components.set(ModelComponent(MeshResource.createTorus(ArrivalCue.MESH_OUTER, ArrivalCue.MESH_INNER), material))
                components.set(OpacityControllerComponent(0f))
                enabled = false
            }
            return ArrivalRing(ring)
        }
    }
}

/** A focused exhibit's close look, else the named viewpoint, else arrival. */
private fun standFor(archive: ArchiveState, layout: RoomLayout?): StandPoint? {
    layout ?: return null
    archive.focusedId?.let { id -> layout.anchor(id)?.let { return ArchiveGeometry.closeLook(it) } }
    return layout.viewpoints[archive.viewpoint.key] ?: layout.viewpoints[RoomLayout.ARRIVAL]
}

/** A placed exhibit: its root (anchor pose) and its turn entity (the room's 45° turn, collider, hover). */
private class Placed(val root: Entity, val turn: Entity)

/**
 * exhibit_<id> (anchor pose) > exhibit_<id>_turn (the room's turn about the anchor's vertical axis; collider,
 * interactable, hover) > fit > the loaded model. Exhibit models under `models/exhibits/` are authored at real size with
 * the base centre at their origin, so they only take the anchor's display scale; the accepted vessel (a normalised
 * generated model) is fitted to its real height on its own origin. Either way the fit sits below the turn, so a turn
 * keeps the object on its plinth.
 */
private fun place(model: Entity, anchor: Anchor, artifact: Artifact): Placed {
    val root = Entity().apply { setName("exhibit_${anchor.id}") }
    val turn = Entity().apply { setName("exhibit_${anchor.id}_turn") }
    val fit = Entity().apply { setName("exhibit_${anchor.id}_fit") }
    check(root.addChild(turn)) { "attach turn" }
    check(turn.addChild(fit)) { "attach fit" }
    check(fit.addChild(model)) { "attach model" }
    val spec = artifact.model
    if (spec.uri.contains("/exhibits/")) {
        fit.components.get<TransformComponent>()?.setScaleVector(Vector3(x = anchor.displayScale, y = anchor.displayScale, z = anchor.displayScale))
    } else {
        val b = model.getVisualBounds(relativeTo = fit, recursive = true, enabledOnly = false)
        val measured = ModelBounds(b.min.x, b.min.y, b.min.z, b.max.x, b.max.y, b.max.z)
        val target = FitTarget(height = spec.realSize.height * anchor.displayScale, baseY = 0f)
        val p = ModelFit.fit(measured, target, spec.anchor)
            ?: spec.authoredBounds?.let { ModelFit.fit(it, target, spec.anchor) }
            ?: error("bounds")
        fit.components.get<TransformComponent>()?.apply {
            setScaleVector(Vector3(x = p.scale, y = p.scale, z = p.scale))
            setPosition(Vector3(x = p.x, y = p.y, z = p.z))
        }
    }
    root.components.get<TransformComponent>()?.apply {
        setPosition(Vector3(x = anchor.position.x, y = anchor.position.y, z = anchor.position.z))
        setEulerAngles(EulerAngles(pitch = 0f, yaw = anchor.yawDegrees, roll = 0f))
    }
    // The collider is in the turn entity's frame, so it turns with the model (as the inspector's does on its rotating
    // root); the turn is at the front here, so it is the box the root carried before.
    val box = fit.getVisualBounds(relativeTo = turn, recursive = true, enabledOnly = false)
    if (!box.isEmpty()) {
        turn.components.set(CollisionComponent(listOf(ShapeResource.createBox(box.size).offsetByTranslation(box.center)), PhysicsMaterialResource()))
        turn.components.set(InteractableComponent())
        turn.components.set(HoverEffectComponent())
    }
    return Placed(root, turn)
}

/** Writes the turn: the focused exhibit at [yaw] on its plinth, every other exhibit at its front (idempotent). */
private fun applyTurns(handles: ArchiveHandles, focusedId: String?, yaw: Float) {
    handles.turntables.forEach { (id, turn) ->
        if (turn.valid) {
            turn.components.get<TransformComponent>()?.setEulerAngles(EulerAngles(pitch = 0f, yaw = if (id == focusedId) yaw else 0f, roll = 0f))
        }
    }
}

/**
 * The invisible tap targets once the room is in: one per placed anchor over its support and plaque, mapped to its
 * exhibit, and one over the lit ring on the entrance door. Each is a child of the world (counted, destroyed with it).
 */
private fun addRoomTargets(world: Entity, layout: RoomLayout, handles: ArchiveHandles) {
    for (anchor in layout.anchors) {
        if (Catalogue.byId(anchor.id) == null) continue
        val boxes = ArchiveTargets.supportBoxes(anchor)
        if (boxes.isEmpty()) continue
        runCatching { roomTarget("exhibit_${anchor.id}_support", anchor.position, anchor.yawDegrees, boxes) }
            .onSuccess { target -> if (world.addChild(target)) { handles.supports[anchor.id] = target; handles.createdChild(target) } else target.destroy(recursively = true) }
            .onFailure { Log.w(TAG, "archive-target-failed id=${anchor.id} reason=${it.javaClass.simpleName}") }
    }
    layout.exitMarker?.let { marker ->
        runCatching { roomTarget("archive_exit", Point3(0f, 0f, 0f), 0f, listOf(ArchiveTargets.exitMarkerBox(marker))) }
            .onSuccess { target -> if (world.addChild(target)) { handles.exit = target; handles.createdChild(target) } else target.destroy(recursively = true) }
            .onFailure { Log.w(TAG, "archive-target-failed id=exit reason=${it.javaClass.simpleName}") }
    }
    Log.i(TAG, "archive-targets supports=${handles.supports.size} exit=${handles.exit != null}")
}

/**
 * The floor circles once the room is in ([ArchivePads]): one per placed anchor, on the floor in front of its support,
 * mapped to its exhibit. Each is a child of the world (counted, destroyed with it); one that cannot be built is left out.
 */
private fun addPads(world: Entity, layout: RoomLayout, handles: ArchiveHandles) {
    for (anchor in layout.anchors) {
        if (Catalogue.byId(anchor.id) == null) continue
        runCatching { floorPad(anchor) }
            .onSuccess { pad -> if (world.addChild(pad)) { handles.pads[anchor.id] = pad; handles.createdChild(pad) } else pad.destroy(recursively = true) }
            .onFailure { Log.w(TAG, "archive-pad-failed id=${anchor.id} reason=${it.javaClass.simpleName}") }
    }
    Log.i(TAG, "archive-pads count=${handles.pads.size}")
}

/**
 * exhibit_<id>_pad (on the floor at [ArchivePads.centre], turned like its anchor; collider, interactable, hover) > a
 * thin ring and a faint disc. The hover is the system's, drawn out of process: how it shows on the ring is not yet
 * observed natively. A circle that fails part-way is destroyed before the failure goes on.
 */
private fun floorPad(anchor: Anchor): Entity {
    val pad = Entity().apply { setName("exhibit_${anchor.id}_pad") }
    return runCatching {
        val at = ArchivePads.centre(anchor)
        pad.components.get<TransformComponent>()?.apply {
            setPosition(Vector3(x = at.x, y = at.y, z = at.z))
            setEulerAngles(EulerAngles(pitch = 0f, yaw = anchor.yawDegrees, roll = 0f))
        }
        pad.components.set(CollisionComponent(listOf(ArchivePads.COLLIDER.shape()), PhysicsMaterialResource()))
        pad.components.set(InteractableComponent())
        pad.components.set(HoverEffectComponent())
        val ring = MeshResource.createTorus(outerRingRadius = ArchivePads.RADIUS, innerRingRadius = ArchivePads.RING_INNER)
        check(pad.addChild(padPart("exhibit_${anchor.id}_pad_ring", ring, ArchivePads.RING_OPACITY, ArchivePads.RING_LIFT))) { "attach ring" }
        val disc = MeshResource.createCylinder(height = ArchivePads.DISC_HEIGHT, radius = ArchivePads.DISC_RADIUS)
        check(pad.addChild(padPart("exhibit_${anchor.id}_pad_disc", disc, ArchivePads.DISC_OPACITY, ArchivePads.DISC_LIFT))) { "attach disc" }
        pad
    }.getOrElse { error ->
        runCatching { pad.destroy(recursively = true) }
        throw error
    }
}

/** One part of a floor circle: [mesh] in [PAD_IVORY], unlit, see-through at [opacity], its centre [lift] above the floor. */
private fun padPart(name: String, mesh: MeshResource, opacity: Float, lift: Float): Entity {
    val material = UnlitMaterial.create(BlendingMode.TRANSPARENT).apply {
        setBaseColor(PAD_IVORY)
        setOpacity(opacity)
        // Millimetres above the floor, like the arrival ring: not writing depth keeps the ring and disc from cutting in.
        setDepthWrite(false)
    }
    return Entity().apply {
        setName(name)
        components.set(ModelComponent(mesh, material))
        components.get<TransformComponent>()?.setPosition(Vector3(x = 0f, y = lift, z = 0f))
    }
}

/**
 * Hides the focused exhibit's floor circle, shows every other one ([ArchivePads.shown]) and records the hidden one as no
 * tap target. Leaving is no change of focus: CLOSED (focus cleared) is committed while this composition is still alive,
 * so CLOSING and CLOSED write nothing, as in [applyStand].
 */
private fun showPads(handles: ArchiveHandles, archive: ArchiveState) {
    if (archive.phase == ArchivePhase.CLOSING || archive.phase == ArchivePhase.CLOSED) return
    handles.padHidden = archive.focusedId
    handles.pads.forEach { (id, pad) -> if (pad.valid) pad.enabled = ArchivePads.shown(id, archive.focusedId) }
}

/**
 * One invisible target: [boxes] in its own frame, placed at [at] and turned by [yawDegrees]. What the hover effect
 * shows on an entity with no mesh of its own or below it is not yet observed natively.
 */
private fun roomTarget(name: String, at: Point3, yawDegrees: Float, boxes: List<TargetBox>): Entity = Entity().apply {
    setName(name)
    components.get<TransformComponent>()?.apply {
        setPosition(Vector3(x = at.x, y = at.y, z = at.z))
        setEulerAngles(EulerAngles(pitch = 0f, yaw = yawDegrees, roll = 0f))
    }
    components.set(CollisionComponent(boxes.map { it.shape() }, PhysicsMaterialResource()))
    components.set(InteractableComponent())
    components.set(HoverEffectComponent())
}

private fun TargetBox.shape(): ShapeResource =
    ShapeResource.createBox(Vector3(x = size.x, y = size.y, z = size.z)).offsetByTranslation(Vector3(x = centre.x, y = centre.y, z = centre.z))

private suspend fun loadOrNull(context: Context, uri: String, budgetMillis: Long, what: String): Entity? {
    val result = runCatching { withTimeoutOrNull(budgetMillis) { ModelSource.load(context, uri) } }
    result.exceptionOrNull()?.let { e ->
        if (e is CancellationException) throw e
        // openFd also throws FileNotFoundException for an entry that exists but is compressed (the noCompress rule lost):
        // that is a packaging fault, reported with its trace, not a missing file.
        val notStored = e is FileNotFoundException && ModelSource.exists(context, uri.removePrefix("asset://"))
        val missing = !notStored && (e is FileNotFoundException || (e is ResourceLoadingException && e.errorCode == ResourceErrorCode.NO_ENTRY))
        val reason = when {
            notStored -> "load-not-stored"
            e is ResourceLoadingException -> "load-${e.errorCode.name}"
            else -> "load-${e.javaClass.simpleName}"
        }
        // A missing file is an expected state of a partly installed collection: one line. Anything else keeps its trace.
        if (missing) Log.w(TAG, "archive-load-failed what=$what uri=$uri reason=$reason")
        else Log.e(TAG, "archive-load-failed what=$what uri=$uri reason=$reason", e)
        return null
    }
    return result.getOrNull().also { if (it == null) Log.e(TAG, "archive-load-failed what=$what uri=$uri reason=timeout") }
}

/** Parents a label attachment to the room at its placard (front toward the visitor, tilted up by its pitch). */
private fun placeLabel(label: Entity, placard: Placard, scale: Float, world: Entity, handles: ArchiveHandles) {
    runCatching {
        label.components.get<TransformComponent>()?.apply {
            val at = ArchiveGeometry.labelPosition(placard)
            setPosition(Vector3(x = at.x, y = at.y, z = at.z))
            // Positive pitch about +X turns a +Z front downward, so a face tilted up takes a negative pitch.
            setEulerAngles(EulerAngles(pitch = -placard.pitchDegrees, yaw = placard.yawDegrees, roll = 0f))
            setScaleVector(Vector3(x = scale, y = scale, z = scale))
        }
        check(world.addChild(label)) { "attach label" }
        handles.labels += label
    }.onFailure { Log.w(TAG, "archive-label-failed reason=${it.javaClass.simpleName}") }
}

@Composable
private fun PlaqueLabel(item: Artifact) {
    Column(
        modifier = Modifier.size(PLAQUE_WIDTH_DP.dp, PLAQUE_HEIGHT_DP.dp).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = "No. ${item.number}", color = PLAQUE_INK, fontSize = 14.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.5.sp)
        Text(
            text = item.name,
            color = PLAQUE_INK,
            fontSize = 24.sp,
            lineHeight = 26.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun BayTitle(bay: String) {
    val v = ArchiveViewpoint.entries.firstOrNull { it.key == bay } ?: return
    Column(
        modifier = Modifier.size(SIGN_WIDTH_DP.dp, SIGN_HEIGHT_DP.dp).padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = stringResource(R.string.archive_overline), color = SIGN_INK.copy(alpha = 0.75f), fontSize = 15.sp, letterSpacing = 2.sp)
        Text(
            text = stringResource(v.label),
            color = SIGN_INK,
            fontSize = 52.sp,
            lineHeight = 58.sp,
            fontWeight = FontWeight.Light,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

/** The caption under the door's lit ring: Return to catalogue, as on the panel; enabled only while the archive is OPEN. */
@Composable
private fun ExitCaption(enabled: Boolean, onExit: () -> Unit) {
    Box(modifier = Modifier.size(EXIT_WIDTH_DP.dp, EXIT_HEIGHT_DP.dp), contentAlignment = Alignment.Center) {
        PolishButton(onClick = onExit, enabled = enabled, tone = PolishButtonTone.Standard) {
            Text(text = stringResource(R.string.archive_return))
        }
    }
}

private val ArchiveViewpoint.label: Int
    get() = when (this) {
        ArchiveViewpoint.ARRIVAL -> R.string.archive_view_arrival
        ArchiveViewpoint.DAILY -> R.string.archive_view_daily
        ArchiveViewpoint.CONNECTED -> R.string.archive_view_connected
        ArchiveViewpoint.DOMESTIC -> R.string.archive_view_domestic
    }

/**
 * The persistent archive panel. Return is the first control and is always enabled, during loading and after errors.
 * Arrival names the visitor's shortlist; a close look adds the object's one-line subtitle, Turn 45°, and the shortlist
 * toggle (focus selected the object, so it saves exactly what is shown).
 */
@Composable
private fun ArchivePanel(
    controller: MuseumController,
    layout: RoomLayout?,
    load: ArchiveLoad,
    onReturn: () -> Unit,
    onOpen3d: () -> Unit,
) {
    val archive = controller.state.archive
    PolishSurface(modifier = Modifier.width(760.dp).heightIn(min = ArchiveGeometry.PANEL_MIN_HEIGHT_DP.dp), kind = PolishSurfaceKind.Panel) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(PolishTokens.SpaceMedium)) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = stringResource(R.string.archive_overline), style = PolishTypography.overline)
                Text(
                    text = if (archive.viewpoint == ArchiveViewpoint.ARRIVAL) stringResource(R.string.archive_title_arrival)
                    else stringResource(R.string.archive_title_bay, stringResource(archive.viewpoint.label)),
                    style = PolishTypography.sectionTitle,
                )
            }
            PolishButton(onClick = onReturn, tone = PolishButtonTone.Emphasis, enabled = archive.phase != ArchivePhase.CLOSING) {
                Text(text = stringResource(R.string.archive_return))
            }
        }
        Text(text = loadLine(load, layout), style = PolishTypography.caption)
        Row(horizontalArrangement = Arrangement.spacedBy(PolishTokens.SpaceSmall)) {
            for (v in ArchiveViewpoint.entries) {
                PolishButton(
                    onClick = { controller.archiveGoTo(v, Surface.ARCHIVE) },
                    tone = if (v == archive.viewpoint && archive.focusedId == null) PolishButtonTone.Emphasis else PolishButtonTone.Quiet,
                ) { Text(text = stringResource(v.label)) }
            }
        }
        if (archive.viewpoint != ArchiveViewpoint.ARRIVAL) {
            val bayItems = Catalogue.artifacts.filter { it.section.key == archive.viewpoint.key }
            bayItems.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(PolishTokens.SpaceSmall)) {
                    row.forEach { item ->
                        PolishButton(
                            onClick = { controller.archiveFocus(item.id, Surface.ARCHIVE) },
                            enabled = layout?.anchor(item.id) != null,
                            tone = if (archive.focusedId == item.id) PolishButtonTone.Emphasis else PolishButtonTone.Standard,
                        ) { Text(text = "${item.number} ${item.name}") }
                    }
                }
            }
        }
        val focused = archive.focusedId?.let { Catalogue.byId(it) }
        if (focused == null) {
            val hint = if (archive.viewpoint == ArchiveViewpoint.ARRIVAL) R.string.archive_hint_arrival else R.string.archive_hint_bay
            Text(text = stringResource(hint), style = PolishTypography.caption)
        }
        if (archive.viewpoint == ArchiveViewpoint.ARRIVAL) ShortlistRow(controller, layout)
        focused?.let { item ->
            val onDisplay = load.exhibits[item.id] == ExhibitLoad.READY
            // The shortlist toggle sits beside the name, like Return beside the title, so the card gains no row for it.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(PolishTokens.SpaceMedium),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.archive_focus_overline, item.number, item.section.label, realSizePhrase(item.model.realSize)),
                        style = PolishTypography.overline,
                    )
                    Text(text = item.name, style = PolishTypography.objectTitle)
                    // The one-line reason it is in the collection: the registry's subtitle, no new facts.
                    Text(text = item.subtitle, style = PolishTypography.body(), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                ShortlistToggle(controller, item)
            }
            Text(text = item.identification, style = PolishTypography.body(multiline = true))
            Text(
                text = stringResource(if (onDisplay) R.string.archive_hint_focus else R.string.archive_not_on_display),
                style = PolishTypography.caption,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(PolishTokens.SpaceSmall)) {
                PolishButton(onClick = { controller.archiveTurn(item.id, Surface.ARCHIVE) }, enabled = onDisplay, tone = PolishButtonTone.Quiet) {
                    Text(text = stringResource(R.string.archive_turn))
                }
                PolishButton(onClick = onOpen3d, tone = PolishButtonTone.Standard) { Text(text = stringResource(R.string.action_open_3d)) }
                PolishButton(onClick = { controller.archiveGoTo(archive.viewpoint, Surface.ARCHIVE) }, tone = PolishButtonTone.Quiet) {
                    Text(text = stringResource(R.string.archive_back_to_bay))
                }
            }
        }
    }
}

/**
 * The visitor's shortlist in the room: the catalogue's Saved view (accession order, unknown ids dropped), up to
 * [SHORTLIST_SHOWN] as buttons, each an instant cut to that object's close look; the rest are counted.
 */
@Composable
private fun ShortlistRow(controller: MuseumController, layout: RoomLayout?) {
    val saved = CatalogueQuery.visible(Catalogue.artifacts, CatalogueFilter(savedOnly = true), controller.state.savedIds)
        .filter { layout?.anchor(it.id) != null }
    Text(text = stringResource(R.string.archive_saved_heading), style = PolishTypography.overline)
    if (saved.isEmpty()) {
        Text(text = stringResource(R.string.archive_saved_none), style = PolishTypography.caption)
        return
    }
    Row(horizontalArrangement = Arrangement.spacedBy(PolishTokens.SpaceSmall)) {
        saved.take(SHORTLIST_SHOWN).forEach { item ->
            PolishButton(onClick = { controller.archiveFocus(item.id, Surface.ARCHIVE) }, tone = PolishButtonTone.Quiet) {
                Text(text = "${item.number} ${item.name}")
            }
        }
    }
    if (saved.size > SHORTLIST_SHOWN) {
        Text(text = stringResource(R.string.archive_saved_more, saved.size - SHORTLIST_SHOWN), style = PolishTypography.caption)
    }
}

/**
 * Save to shortlist / Remove from shortlist for the object in focus, one toggle as in the catalogue (Standard to save,
 * Quiet once saved). Keep and unkeep act on the selection, which focus set; it is disabled should the two ever differ.
 */
@Composable
private fun ShortlistToggle(controller: MuseumController, item: Artifact) {
    val state = controller.state
    val isSaved = state.isSaved()
    PolishButton(
        onClick = { if (isSaved) controller.unkeep(Surface.ARCHIVE) else controller.keep(Surface.ARCHIVE) },
        enabled = state.selectedId == item.id,
        tone = if (isSaved) PolishButtonTone.Quiet else PolishButtonTone.Standard,
    ) {
        Text(text = if (isSaved) stringResource(R.string.action_unkeep) else stringResource(R.string.action_keep))
    }
}

@Composable
private fun loadLine(load: ArchiveLoad, layout: RoomLayout?): String = when {
    layout == null -> stringResource(R.string.archive_layout_failed)
    load.room == RoomLoad.FAILED -> stringResource(R.string.archive_room_failed, load.ready, load.total)
    !load.settled -> stringResource(R.string.archive_loading, load.ready, load.total)
    load.failed > 0 -> stringResource(R.string.archive_loaded_partly, load.ready, load.total, load.failed)
    else -> stringResource(R.string.archive_loaded, load.total)
}
