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
import androidx.compose.ui.platform.LocalContext
import com.pico.spatial.core.container.SpatialViewContent
import com.pico.spatial.core.ecs.Entity
import com.pico.spatial.core.ecs.TransformComponent
import com.pico.spatial.core.math.EulerAngles
import com.pico.spatial.ui.foundation.content.SpatialView
import dev.local.everydayfutures.museum.catalogue.Artifact
import dev.local.everydayfutures.museum.state.MuseumController

private const val TAG = "Museum"

/** The `surface=` word in the study's `exhibit-load` / `exhibit-model` markers. */
private const val STUDY = "study"

/**
 * Survives recomposition: the scene content captured in `initial`, the key light and the one model root a surface
 * shows. Main thread only. [disposed] stops a late result from being added after the composable has left.
 */
internal class ModelSlot {
    var content: SpatialViewContent? = null
    var light: Entity? = null
    var root: Entity? = null
        private set
    var disposed = false
        private set
    private var tokens = 0L
    private var appliedYaw = Float.NaN
    private var appliedVisible: Boolean? = null

    /** A new request token: results carrying an older one are stale (ModelLoadMachine.complete). */
    fun nextToken(): Long = ++tokens

    /** Adds [entity] to the scene as this slot's model root. */
    fun attach(entity: Entity) {
        val scene = checkNotNull(content) { "scene not ready" }
        scene.addEntity(entity)
        root = entity
        appliedYaw = Float.NaN
        appliedVisible = entity.enabled
    }

    /** Hides, removes and destroys the current model root (a new selection, or a retry). */
    fun clearRoot() {
        val old = root ?: return
        root = null
        runCatching {
            old.enabled = false
            content?.removeEntity(old)
            old.destroy(recursively = true)
        }.onFailure { Log.e(TAG, "model root not removed", it) }
    }

    /** Shows or hides the current root without destroying it (its pose is kept); tracked here, not read back. */
    fun setVisible(visible: Boolean) {
        val current = root ?: return
        if (!current.valid || visible == appliedVisible) return
        current.enabled = visible
        appliedVisible = visible
    }

    /** Writes an ABSOLUTE yaw to the current root (skipped when it is already there). */
    fun applyYaw(yaw: Float) {
        val current = root ?: return
        if (!current.valid || yaw == appliedYaw) return
        current.components.get<TransformComponent>()?.setEulerAngles(EulerAngles(pitch = 0f, yaw = yaw, roll = 0f))
        appliedYaw = yaw
    }

    /** True when [entity] is the model root or part of it (a gesture target walk). */
    fun owns(entity: Entity): Boolean {
        val target = root ?: return false
        var e: Entity? = entity
        while (e != null) {
            if (e == target) return true
            e = e.getParent()
        }
        return false
    }

    /** SpatialView only detaches entities on release; this destroys them. */
    fun destroyAll() {
        disposed = true
        runCatching { root?.destroy(recursively = true) }
        root = null
        runCatching { light?.destroy(recursively = true) }
        light = null
        content = null
    }
}

/**
 * The study scene: only the selected object's model, loaded lazily. `initial` (main thread) creates the scene with its
 * key light and nothing else; the load runs in an effect keyed on the selection and the attempt, so a new selection
 * cancels the in-flight load (cancelling the coroutine cancels the native job), destroys the previous root and shows
 * a loading card naming the object until the new root is attached; then the ABSOLUTE yaw from state is written, and
 * `update` keeps writing it on every turn. A model that cannot be loaded shows an error card naming the object with a
 * bounded Try again (two retries per selection) — never another object's model; the three original objects show their
 * procedural stand-in instead. Markers: `exhibit-load id=… surface=study attempt=n`, then `exhibit-model id=…
 * surface=study model=generated|procedural|none ms=… reason=…` and `exhibits-ready count=0|1 light=… model=… id=…`.
 * While [shownInInspector] (the Open in 3D inspector shows the same object) the model is hidden — not destroyed, its
 * pose kept — and a card says where it is and brings that window forward; it reappears when the inspector goes
 * (`study-model hidden=1|0 reason=inspector`).
 * This composable destroys everything it created when it leaves composition.
 */
@Composable
fun ObjectPresenter(
    artifact: Artifact,
    turnDegrees: Float,
    controller: MuseumController,
    modifier: Modifier = Modifier,
    shownInInspector: Boolean = false,
    onBringForward: () -> Unit = {},
) {
    val context = LocalContext.current
    val slot = remember { ModelSlot() }
    var sceneReady by remember { mutableStateOf(false) }
    var loadState by remember { mutableStateOf<ModelLoadState>(ModelLoadState.Idle) }
    var attempt by remember(artifact.id) { mutableIntStateOf(1) }
    val yaw by rememberUpdatedState(turnDegrees)
    val hidden by rememberUpdatedState(shownInInspector)

    DisposableEffect(Unit) {
        onDispose { slot.destroyAll() }
    }

    // One marker per actual change (none at start, when nothing is hidden).
    var loggedHidden by remember { mutableStateOf(false) }
    LaunchedEffect(shownInInspector) {
        if (shownInInspector != loggedHidden) {
            loggedHidden = shownInInspector
            controller.studyModelHidden(shownInInspector, artifact.id)
        }
    }

    LaunchedEffect(artifact.id, attempt, sceneReady) {
        if (!sceneReady || slot.content == null) return@LaunchedEffect
        val token = slot.nextToken()
        val loading = ModelLoadMachine.start(artifact.id, attempt, token)
        loadState = loading
        slot.clearRoot()
        controller.modelLoadStarted(artifact.id, STUDY, loading.attempt)
        val loaded = ExhibitLoader.load(context, artifact, ExhibitBoxes.STUDY, namePrefix = STUDY)
        val outcome = attachOrDiscard(slot, loaded, token, artifact.id, { loadState }) { root ->
            root.writeYaw(yaw)
            root.enabled = !hidden // a model loaded while the inspector shows it arrives hidden
        } ?: return@LaunchedEffect
        val next = ModelLoadMachine.complete(loadState, token, artifact.id, outcome) ?: return@LaunchedEffect
        loadState = next
        controller.modelShown(artifact.id, STUDY, outcome.kind.marker, loaded.millis, outcome.reason, lightReady = slot.light != null)
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        SpatialView(
            modifier = Modifier.fillMaxSize(),
            initial = { content, _ ->
                // Factories can throw ResourceLoadingException; an unhandled throw inside the SDK's launched scope would
                // take the process down, so the light is created under its own guard and nothing else is built here.
                slot.content = content
                runCatching { ExhibitFactory.keyLight() }
                    .onSuccess { light -> slot.light = light; content.addEntity(light) }
                    .onFailure { Log.e(TAG, "key light not created", it) }
                sceneReady = true
            },
            update = { _, _ ->
                slot.applyYaw(yaw)
                slot.setVisible(!hidden)
            },
        )
        if (shownInInspector) {
            ModelElsewhereCard(onBringForward = onBringForward)
        } else {
            ModelStatusCard(state = loadState, artifact = artifact, onRetry = { next -> attempt = next })
        }
    }
}

/**
 * The card over a model region: loading until the selected object's root is attached, an error naming it when
 * nothing can be shown, nothing once it is shown.
 */
@Composable
internal fun ModelStatusCard(state: ModelLoadState, artifact: Artifact, onRetry: (Int) -> Unit) {
    when {
        state is ModelLoadState.Ready && state.id == artifact.id -> Unit
        state is ModelLoadState.Failed && state.id == artifact.id -> ModelErrorCard(
            artifact = artifact,
            canRetry = state.canRetry,
            onRetry = { ModelLoadMachine.retryAttempt(state)?.let(onRetry) },
        )
        else -> ModelLoadingCard(artifact = artifact)
    }
}

/**
 * Attaches a finished attempt's root to the slot's scene, or destroys it when the attempt is stale or the composable
 * has left. Returns the outcome to record (an attach failure becomes `attach-<error>`), or null when stale.
 */
internal fun attachOrDiscard(
    slot: ModelSlot,
    loaded: ExhibitLoader.Loaded,
    token: Long,
    id: String,
    currentState: () -> ModelLoadState,
    prepare: (Entity) -> Unit,
): ModelOutcome? {
    val root = loaded.root
    val stale = slot.disposed || ModelLoadMachine.complete(currentState(), token, id, loaded.outcome) == null
    if (stale) {
        root?.let { runCatching { it.destroy(recursively = true) } }
        return null
    }
    if (root == null) return loaded.outcome
    val attached = runCatching {
        prepare(root)
        slot.attach(root)
    }
    return attached.exceptionOrNull()?.let { error ->
        Log.e(TAG, "exhibit-attach-failed id=$id", error)
        runCatching { root.destroy(recursively = true) }
        ModelOutcome.Failed("attach-${error.javaClass.simpleName}")
    } ?: loaded.outcome
}

private fun Entity.writeYaw(yaw: Float) {
    components.get<TransformComponent>()?.setEulerAngles(EulerAngles(pitch = 0f, yaw = yaw, roll = 0f))
}
