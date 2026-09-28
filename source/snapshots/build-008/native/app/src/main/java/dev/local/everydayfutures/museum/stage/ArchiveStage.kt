package dev.local.everydayfutures.museum.stage

import android.content.Context
import android.os.SystemClock
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
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
import com.pico.spatial.core.ecs.AttachmentPanelComponent
import com.pico.spatial.core.ecs.CollisionComponent
import com.pico.spatial.core.ecs.DirectionalLightComponent
import com.pico.spatial.core.ecs.Entity
import com.pico.spatial.core.ecs.InteractableComponent
import com.pico.spatial.core.ecs.TransformComponent
import com.pico.spatial.core.ecs.resource.PhysicsMaterialResource
import com.pico.spatial.core.ecs.resource.ResourceLoadingException.ResourceErrorCode
import com.pico.spatial.core.ecs.resource.ResourceLoadingException
import com.pico.spatial.core.ecs.resource.ShapeResource
import com.pico.spatial.core.math.Color4
import com.pico.spatial.core.math.EulerAngles
import com.pico.spatial.core.math.Vector3
import com.pico.spatial.ui.design.Text
import com.pico.spatial.ui.foundation.content.SpatialView
import com.pico.spatial.ui.foundation.gesture.TargetEntity
import com.pico.spatial.ui.foundation.gesture.detectSpatialTapGesture
import dev.local.everydayfutures.museum.R
import dev.local.everydayfutures.museum.catalogue.Artifact
import dev.local.everydayfutures.museum.catalogue.Catalogue
import dev.local.everydayfutures.museum.content.FitTarget
import dev.local.everydayfutures.museum.content.ModelBounds
import dev.local.everydayfutures.museum.content.ModelFit
import dev.local.everydayfutures.museum.content.ModelSource
import dev.local.everydayfutures.museum.content.rememberStageNavigator
import dev.local.everydayfutures.museum.content.realSizePhrase
import dev.local.everydayfutures.museum.content.rememberWindowNavigator
import dev.local.everydayfutures.museum.state.ArchivePhase
import dev.local.everydayfutures.museum.state.ArchiveState
import dev.local.everydayfutures.museum.state.ArchiveViewpoint
import dev.local.everydayfutures.museum.state.MuseumController
import dev.local.everydayfutures.museum.state.Surface
import dev.local.everydayfutures.museum.state.WorkspaceReducer
import dev.local.picopolish.PolishButton
import dev.local.picopolish.PolishButtonTone
import dev.local.picopolish.PolishSurface
import dev.local.picopolish.PolishSurfaceKind
import dev.local.picopolish.PolishTokens
import dev.local.picopolish.PolishTypography
import java.io.FileNotFoundException
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withTimeoutOrNull

private const val TAG = "Museum"
private const val PANEL_ID = "archive-panel"
private const val ROOM_BUDGET_MS = 10_000L
private const val EXHIBIT_BUDGET_MS = 5_000L

/** Concurrent exhibit loads; the SDK's KDoc asks apps to bound concurrent model loading to the memory budget. */
private const val EXHIBIT_PERMITS = 2

/**
 * The persistent panel stays with the visitor (a child of the scene, not of the room): front-left of the Stage origin,
 * turned toward them. Build-001's first frame (-0.55, 1.15, -1.0), unscaled, showed it about 0.8 m wide, far below the
 * emulator's eye height (1.81 m in the stale calibration) and cut off by the view's lower edge, with half its bay
 * buttons out of view; build-002 at 0.6x was fully in view but its text was small, so 0.75x. Build-004's full room
 * showed the panel (centred at 1.42 m) covering the leftmost exhibit of the Daily and Connected bays, and its buttons
 * moving up as the content grew, so the panel now hangs from its top edge at [PANEL_TOP] (the controls never move) with
 * a constant height ([PANEL_MIN_HEIGHT], 0.25 m at this scale), its lower edge about 1.59 m high, above the exhibit line
 * seen from the viewpoints. Frame-tuned in the emulator, not a comfort claim.
 */
private val PANEL_TOP = Vector3(x = -0.62f, y = 1.84f, z = -1.3f)
private const val PANEL_YAW = 25f
private const val PANEL_SCALE = 0.75f
private val PANEL_MIN_HEIGHT = 420.dp

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

/** Engraved-looking text on the brass plates, and warm lettering on the slate walls; no chip behind either. */
private val PLAQUE_INK = Color(0xFF2A1E12)
private val SIGN_INK = Color(0xFFF2E7D3)

private fun plaqueId(exhibitId: String) = "plaque-$exhibitId"

private fun signId(bay: String) = "sign-$bay"

/** Survives recomposition so disposal destroys exactly what this visit created. */
private class ArchiveHandles {
    var world: Entity? = null
    val exhibits = LinkedHashMap<String, Entity>()

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

    fun exhibitIdOf(entity: Entity): String? {
        var e: Entity? = entity
        while (e != null) {
            val match = e
            exhibits.entries.firstOrNull { it.value == match }?.let { return it.key }
            e = e.getParent()
        }
        return null
    }

    /**
     * One recursive destroy of the world releases the room, the exhibits and the lights (the panel entity is the SDK's).
     * Returns the number of children the visit created and how many of the tracked entities (those children and the world)
     * report `valid == false` afterwards: a confirmation of the entity destroy, not of every native resource behind it.
     */
    fun destroyAll(): DisposeCount {
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
        created = 0
        return count
    }
}

/** What a disposal created and confirmed: [invalidAfter] of [tracked] entities report invalid after the destroy. */
private data class DisposeCount(val created: Int, val tracked: Int, val invalidAfter: Int)

/**
 * The archive visit (L4), the content of the `EverydayFuturesArchive` Stage: the Blender room at the Stage origin, each
 * exhibit's own model on its anchor, two lights for the exhibits (the room's light is baked), and a persistent panel
 * that stays with the visitor — loading state, the bays, the current bay's six objects, a close look, and Return to
 * catalogue, which is always the first control and always enabled. A viewpoint or a close look moves the room root: an
 * instant cut, never an animated ride, so Reduce motion has nothing to change here. The primary window is minimised once
 * the Stage is up and restored before the Stage closes; everything this visit created is destroyed when the Stage's
 * composition leaves, so repeated visits cannot accumulate scenes. Markers: `archive-layout`, `archive-primary`,
 * `archive-load start|settled`, `archive-room loaded`, `archive-exhibit id= result=`, `archive-dispose`.
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

    DisposableEffect(Unit) {
        Log.i(TAG, "archive-layout ok=${layout != null} models=${layout?.roomModels?.size ?: 0} anchors=${layout?.anchors?.size ?: 0} viewpoints=${layout?.viewpoints?.keys?.sorted()}")
        onDispose {
            val disposed = handles.destroyAll()
            // Return restores the primary before closing the Stage. This later call can only succeed while a Stage is still
            // open, which is never the case here: every recorded disposal (builds 001-006) logs restoredPrimary=false. It is
            // kept as a harmless last resort. When the system takes the visitor out of the archive (Home -> "Back to Home"),
            // no disposal happens at all: on resuming, the platform shows the primary again, the Stage stays hidden and the
            // catalogue offers "Return from archive", which closes it (observed on build-006, 2026-09-27).
            val restored = runCatching { stage.restore(WorkspaceReducer.MAIN_ID, null) }.getOrDefault(false)
            // `created` counts what the visit made (room, exhibits, lights); `invalidAfter` confirms the entity destroy of
            // those and the world. Neither measures native memory (see evidence/demo-hardening-001).
            Log.i(TAG, "archive-dispose created=${disposed.created} invalidAfter=${disposed.invalidAfter}/${disposed.tracked} restoredPrimary=$restored")
        }
    }

    // SpatialView calls `update` only after `initial` has returned, and `initial` waits for the room and every exhibit;
    // this moves the room as soon as a viewpoint or close look changes, loading or not (review finding, 2026-09-27).
    LaunchedEffect(controller.state.archive) {
        val world = handles.world ?: return@LaunchedEffect
        if (!handles.disposed && world.valid) applyStand(world, controller.state.archive, layout)
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

    val returnToCatalogue: () -> Unit = {
        scope.launch {
            val restored = runCatching { stage.restore(WorkspaceReducer.MAIN_ID, null) }.getOrDefault(false)
            Log.i(TAG, "archive-primary restored=$restored")
            runCatching { controller.leaveArchive(stage, Surface.ARCHIVE) }
                .onFailure { if (it is CancellationException) throw it else Log.e(TAG, "archive-close failed", it) }
        }
    }

    SpatialView(
        modifier = Modifier.fillMaxSize().pointerInput(Unit) {
            detectSpatialTapGesture(context, targetedToEntity = TargetEntity.any { handles.exhibitIdOf(it) != null }) { tap ->
                val id = tap.targetEntity?.let { handles.exhibitIdOf(it) } ?: return@detectSpatialTapGesture
                Log.i(TAG, "archive-tap id=$id")
                controller.archiveFocus(id, Surface.ARCHIVE)
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
        },
        initial = { content, attachments ->
            // The panel first, so Return is on screen before any model loads.
            attachments.entity(PANEL_ID)?.let { panel ->
                panel.components.get<TransformComponent>()?.apply {
                    setPosition(PANEL_TOP)
                    setEulerAngles(EulerAngles(pitch = 0f, yaw = PANEL_YAW, roll = 0f))
                    setScaleVector(Vector3(x = PANEL_SCALE, y = PANEL_SCALE, z = PANEL_SCALE))
                }
                content.addEntity(panel)
            } ?: Log.e(TAG, "archive-panel missing")
            val world = Entity().apply { setName("archive_world") }
            handles.world = world
            handles.tracked += world
            content.addEntity(world)
            applyStand(world, controller.state.archive, layout)
            layout?.anchors?.forEach { anchor ->
                val plaque = anchor.plaque ?: return@forEach
                val scale = ArchiveGeometry.labelScale(plaque, PLAQUE_WIDTH_DP, PLAQUE_HEIGHT_DP, PLAQUE_FALLBACK_SCALE)
                attachments.entity(plaqueId(anchor.id))?.let { placeLabel(it, plaque, scale, world, handles) }
            }
            layout?.signs?.forEach { (bay, sign) ->
                val scale = ArchiveGeometry.labelScale(sign, SIGN_WIDTH_DP, SIGN_HEIGHT_DP, SIGN_FALLBACK_SCALE)
                attachments.entity(signId(bay))?.let { placeLabel(it, sign, scale, world, handles) }
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
                        val ok = placed != null && world.addChild(placed)
                        if (!ok) {
                            (placed ?: model)?.destroy(recursively = true)
                        } else {
                            handles.exhibits[anchor.id] = placed!!
                            handles.createdChild(placed)
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
            if (world.valid) applyStand(world, controller.state.archive, layout)
        },
    )
}

/** Writes the room root's absolute pose for the current viewpoint or close look (idempotent; an instant cut). */
private fun applyStand(world: Entity, archive: ArchiveState, layout: RoomLayout?) {
    val stand = standFor(archive, layout) ?: return
    val pose = ArchiveGeometry.worldPoseFor(stand)
    world.components.get<TransformComponent>()?.apply {
        setPosition(Vector3(x = pose.position.x, y = pose.position.y, z = pose.position.z))
        setEulerAngles(EulerAngles(pitch = 0f, yaw = pose.yawDegrees, roll = 0f))
    }
}

/** A focused exhibit's close look, else the named viewpoint, else arrival. */
private fun standFor(archive: ArchiveState, layout: RoomLayout?): StandPoint? {
    layout ?: return null
    archive.focusedId?.let { id -> layout.anchor(id)?.let { return ArchiveGeometry.closeLook(it) } }
    return layout.viewpoints[archive.viewpoint.key] ?: layout.viewpoints[RoomLayout.ARRIVAL]
}

/**
 * exhibit_<id> (anchor pose, collider, interactable) > fit > the loaded model. Exhibit models under `models/exhibits/`
 * are authored at real size with the base centre at their origin, so they only take the anchor's display scale; the
 * accepted vessel (a normalised generated model) is fitted to its real height on its own origin.
 */
private fun place(model: Entity, anchor: Anchor, artifact: Artifact): Entity {
    val root = Entity().apply { setName("exhibit_${anchor.id}") }
    val fit = Entity().apply { setName("exhibit_${anchor.id}_fit") }
    check(root.addChild(fit)) { "attach fit" }
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
    val box = fit.getVisualBounds(relativeTo = root, recursive = true, enabledOnly = false)
    if (!box.isEmpty()) {
        root.components.set(CollisionComponent(listOf(ShapeResource.createBox(box.size).offsetByTranslation(box.center)), PhysicsMaterialResource()))
        root.components.set(InteractableComponent())
    }
    return root
}

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

private val ArchiveViewpoint.label: Int
    get() = when (this) {
        ArchiveViewpoint.ARRIVAL -> R.string.archive_view_arrival
        ArchiveViewpoint.DAILY -> R.string.archive_view_daily
        ArchiveViewpoint.CONNECTED -> R.string.archive_view_connected
        ArchiveViewpoint.DOMESTIC -> R.string.archive_view_domestic
    }

/** The persistent archive panel. Return is the first control and is always enabled, during loading and after errors. */
@Composable
private fun ArchivePanel(
    controller: MuseumController,
    layout: RoomLayout?,
    load: ArchiveLoad,
    onReturn: () -> Unit,
    onOpen3d: () -> Unit,
) {
    val archive = controller.state.archive
    PolishSurface(modifier = Modifier.width(760.dp).heightIn(min = PANEL_MIN_HEIGHT), kind = PolishSurfaceKind.Panel) {
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
        focused?.let { item ->
            Text(
                text = stringResource(R.string.archive_focus_overline, item.number, item.section.label, realSizePhrase(item.model.realSize)),
                style = PolishTypography.overline,
            )
            Text(text = item.name, style = PolishTypography.objectTitle)
            Text(text = item.identification, style = PolishTypography.body(multiline = true))
            if (load.exhibits[item.id] != ExhibitLoad.READY) {
                Text(text = stringResource(R.string.archive_not_on_display), style = PolishTypography.caption)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(PolishTokens.SpaceSmall)) {
                PolishButton(onClick = onOpen3d, tone = PolishButtonTone.Standard) { Text(text = stringResource(R.string.action_open_3d)) }
                PolishButton(onClick = { controller.archiveGoTo(archive.viewpoint, Surface.ARCHIVE) }, tone = PolishButtonTone.Quiet) {
                    Text(text = stringResource(R.string.archive_back_to_bay))
                }
            }
        }
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
