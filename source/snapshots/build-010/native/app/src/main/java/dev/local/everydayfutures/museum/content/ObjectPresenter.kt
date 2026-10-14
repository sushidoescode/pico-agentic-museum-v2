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
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.pico.spatial.core.container.SpatialViewContent
import com.pico.spatial.core.ecs.Entity
import com.pico.spatial.core.ecs.OpacityControllerComponent
import com.pico.spatial.core.ecs.TransformComponent
import com.pico.spatial.core.math.EulerAngles
import com.pico.spatial.ui.foundation.content.SpatialView
import dev.local.everydayfutures.museum.catalogue.Artifact
import dev.local.everydayfutures.museum.state.MuseumController
import kotlinx.coroutines.delay

private const val TAG = "Museum"

/** The `surface=` word in the study's `exhibit-load` / `exhibit-model` markers. */
private const val STUDY = "study"

/**
 * Survives recomposition: the scene content captured in `initial`, the key light and the one model root a surface
 * shows, with the object it was built for ([rootId]) and, while it fades in, the root itself as [fading]. Main thread
 * only. [disposed] stops a late result from being added after the composable has left.
 */
internal class ModelSlot {
    var content: SpatialViewContent? = null
    var light: Entity? = null
    var root: Entity? = null
        private set
    var rootId: String? = null
        private set
    var disposed = false
        private set

    /** The root attached transparent for its fade-in ([fadeInRoot]) until [endFade]; null when nothing is fading. */
    var fading: Entity? = null
        private set
    private var tokens = 0L
    private var appliedYaw = Float.NaN
    private var appliedVisible: Boolean? = null

    /** A new request token: results carrying an older one are stale (ModelLoadMachine.complete). */
    fun nextToken(): Long = ++tokens

    /**
     * Adds [entity] to the scene as this slot's model root, built for object [id]. When [transparent], it is given an
     * [OpacityControllerComponent] at 0 before it joins the scene, so its first frame is already transparent for its
     * fade-in; a root that cannot be made transparent simply shows at full opacity.
     */
    fun attach(entity: Entity, id: String, transparent: Boolean = false) {
        val scene = checkNotNull(content) { "scene not ready" }
        val faded = transparent && runCatching { entity.components.set(OpacityControllerComponent(0f)) }
            .onFailure { runCatching { entity.components.remove(OpacityControllerComponent::class.java) } }
            .isSuccess
        scene.addEntity(entity)
        root = entity
        rootId = id
        fading = if (faded) entity else null
        appliedYaw = Float.NaN
        appliedVisible = entity.enabled
    }

    /** Hides, removes and destroys the current model root (a new selection, or a retry). */
    fun clearRoot() {
        val old = root ?: return
        root = null
        rootId = null
        fading = null
        runCatching {
            old.enabled = false
            content?.removeEntity(old)
            old.destroy(recursively = true)
        }.onFailure { Log.e(TAG, "model root not removed", it) }
    }

    /**
     * The one write path for frame, update and drag callbacks: the current root takes [yaw] and [visible] only when it
     * was built for [targetId]; a root left from another object (the selection moved on before the effect removed it)
     * is hidden instead of turned (ModelSwap.rootWrite). Returns true only when that hide changed the root's visibility.
     */
    fun applyPose(targetId: String?, yaw: Float, visible: Boolean = true): Boolean =
        when (ModelSwap.rootWrite(rootId, targetId)) {
            RootWrite.APPLY -> {
                applyYaw(yaw)
                setVisible(visible)
                false
            }
            RootWrite.HIDE_STALE -> setVisible(false) // a disabled entity is not rendered
            RootWrite.SKIP -> false
        }

    /**
     * One frame of the fade-in: [opacity] reaches the fading root only while it is still the current root and state still
     * names the object it was built for ([targetId]), as for a pose. False when it does not, which ends the fade.
     */
    fun applyOpacity(targetId: String?, opacity: Float): Boolean {
        val current = fading ?: return false
        if (current != root || ModelSwap.rootWrite(rootId, targetId) != RootWrite.APPLY) return false
        return runCatching { current.components.get<OpacityControllerComponent>()?.opacity = opacity }.isSuccess
    }

    /**
     * Ends the fade-in of [faded] (the root its load attached), however it ended (over, stopped, or its effect cancelled):
     * the root goes to full opacity and loses its opacity component, so at rest it renders exactly like a root that never
     * faded. Another load's fade is not touched, and a root state has moved on from ([targetId] names another object) is
     * left as it is: it is hidden at once and removed by the next load.
     */
    fun endFade(faded: Entity?, targetId: String?) {
        val current = fading ?: return
        if (current != faded) return
        fading = null
        if (current != root || ModelSwap.rootWrite(rootId, targetId) == RootWrite.HIDE_STALE) return
        runCatching {
            if (current.valid) {
                current.components.get<OpacityControllerComponent>()?.opacity = 1f
                current.components.remove(OpacityControllerComponent::class.java)
            }
        }.onFailure { Log.e(TAG, "model-fade end failed", it) }
    }

    /** Shows or hides the current root without destroying it (its pose is kept); tracked here, not read back. True if changed. */
    private fun setVisible(visible: Boolean): Boolean {
        val current = root ?: return false
        if (!current.valid || visible == appliedVisible) return false
        current.enabled = visible
        appliedVisible = visible
        return true
    }

    /** Writes an ABSOLUTE yaw to the current root (skipped when it is already there). */
    private fun applyYaw(yaw: Float) {
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
        fading = null
        runCatching { root?.destroy(recursively = true) }
        root = null
        rootId = null
        runCatching { light?.destroy(recursively = true) }
        light = null
        content = null
    }
}

/**
 * The study scene: only the selected object's model, loaded lazily. `initial` (main thread) creates the scene with its
 * key light and nothing else; the load runs in an effect keyed on the selection and the attempt, so a new selection
 * cancels the in-flight load (cancelling the coroutine cancels the native job), destroys the previous root and, if the
 * load takes longer than [ModelSwap.LOADING_CARD_DELAY_MS], shows a loading card naming the object until the new root
 * is attached; the new root is posed with the ABSOLUTE yaw from state before it joins the scene, and `update` keeps
 * writing it on every turn. The previous object's root goes at once: `update` poses only a root built for the selected
 * object; if it runs before the effect removes the previous object's root, that root is hidden (`model-swap
 * surface=study hidden=… for=…`), never turned to the new object's front and never faded out. The new root joins the
 * scene transparent and fades in over [FadeIn.MODEL_MILLIS] ([fadeInRoot]); under Reduce motion, or arriving hidden, it
 * shows at full opacity from its first frame. A model that cannot be loaded shows an error card naming the object
 * with a bounded Try again (two retries per selection) — never another object's model; the three original objects show
 * their procedural stand-in instead. Markers: `exhibit-load id=… surface=study attempt=n`, then `exhibit-model id=…
 * surface=study model=generated|procedural|none ms=… reason=…` and `exhibits-ready count=0|1 light=… model=… id=…`, and
 * for a shown root `model-fade surface=study id=… ms=…`.
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
    // Updated in the same recomposition as yaw, so a write always names the object its yaw belongs to.
    val shownId by rememberUpdatedState(artifact.id)

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
        // Decided as the root arrives: under Reduce motion, or arriving hidden, it shows at full opacity at once.
        val skip = FadeIn.skipReason(controller.state.options.reduceMotion, visible = !hidden)
        val outcome = attachOrDiscard(slot, loaded, token, artifact.id, { loadState }, transparent = skip == null) { root ->
            root.writeYaw(yaw)
            root.enabled = !hidden // a model loaded while the inspector shows it arrives hidden
        } ?: return@LaunchedEffect
        try {
            val next = ModelLoadMachine.complete(loadState, token, artifact.id, outcome) ?: return@LaunchedEffect
            loadState = next
            val lightReady = slot.light != null
            controller.modelShown(artifact.id, STUDY, outcome.kind.marker, loaded.millis, outcome.reason, lightReady = lightReady)
            slot.fadeInRoot(controller, STUDY, artifact.id, skip) { shownId }
        } finally {
            // Also when a new selection or the composable leaving cancels the fade: never left part-transparent.
            slot.endFade(loaded.root, shownId)
        }
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
                val target = shownId
                val stale = slot.rootId
                if (slot.applyPose(target, yaw, visible = !hidden)) stale?.let { controller.modelSwapHidden(STUDY, it, target) }
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
 * The card over a model region: loading until the selected object's root is attached (only once the load has taken
 * [ModelSwap.LOADING_CARD_DELAY_MS], so a quick switch shows no text), an error naming it at once when nothing can be
 * shown, nothing once it is shown.
 */
@Composable
internal fun ModelStatusCard(state: ModelLoadState, artifact: Artifact, onRetry: (Int) -> Unit) {
    // Restarts per object; a Try again for the same object shows the loading text at once.
    var loadingDue by remember(artifact.id) { mutableStateOf(false) }
    LaunchedEffect(artifact.id) {
        delay(ModelSwap.LOADING_CARD_DELAY_MS)
        loadingDue = true
    }
    when (ModelSwap.card(state, artifact.id, loadingDue)) {
        ModelCard.NONE -> Unit
        ModelCard.LOADING -> ModelLoadingCard(artifact = artifact)
        ModelCard.FAILED -> if (state is ModelLoadState.Failed) {
            ModelErrorCard(
                artifact = artifact,
                canRetry = state.canRetry,
                onRetry = { ModelLoadMachine.retryAttempt(state)?.let(onRetry) },
            )
        }
    }
}

/**
 * Attaches a finished attempt's root to the slot's scene ([transparent]: at opacity 0, for its fade-in), or destroys it
 * when the attempt is stale or the composable has left. Returns the outcome to record (an attach failure becomes
 * `attach-<error>`), or null when stale.
 */
internal fun attachOrDiscard(
    slot: ModelSlot,
    loaded: ExhibitLoader.Loaded,
    token: Long,
    id: String,
    currentState: () -> ModelLoadState,
    transparent: Boolean = false,
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
        slot.attach(root, id, transparent)
    }
    return attached.exceptionOrNull()?.let { error ->
        Log.e(TAG, "exhibit-attach-failed id=$id", error)
        runCatching { root.destroy(recursively = true) }
        ModelOutcome.Failed("attach-${error.javaClass.simpleName}")
    } ?: loaded.outcome
}

/**
 * The fade-in of the root the load effect has just attached (both surfaces): its opacity from 0 to 1 over
 * [FadeIn.MODEL_MILLIS] on the composition's frame clock, which also runs the story's chapter moves, logged as
 * `model-fade surface=… id=… ms=…`. A root attached opaque logs `ms=0 reason=…` ([skip], or `unavailable` when it could
 * not be made transparent) and returns at once; with nothing attached (a failure) nothing is logged. Each frame goes
 * through [ModelSlot.applyOpacity], so it stops early once state names another object, and it stops when Reduce motion
 * comes on. The caller ends the fade in a `finally` ([ModelSlot.endFade]), so a cancelled one is never left
 * part-transparent.
 */
internal suspend fun ModelSlot.fadeInRoot(
    controller: MuseumController,
    surface: String,
    id: String,
    skip: String?,
    targetId: () -> String?,
) {
    if (root == null) return
    if (fading == null) {
        controller.modelFade(id, surface, 0L, skip ?: "unavailable")
        return
    }
    controller.modelFade(id, surface, FadeIn.MODEL_MILLIS)
    val start = withFrameMillis { it }
    while (!controller.state.options.reduceMotion) {
        val elapsed = (withFrameMillis { it } - start).toFloat()
        if (FadeIn.isOver(elapsed, FadeIn.MODEL_MILLIS)) return
        if (!applyOpacity(targetId(), FadeIn.opacityAt(elapsed, FadeIn.MODEL_MILLIS))) return
    }
}

/** Poses a root before it joins the scene (both surfaces), so it never appears at another yaw first. */
internal fun Entity.writeYaw(yaw: Float) {
    components.get<TransformComponent>()?.setEulerAngles(EulerAngles(pitch = 0f, yaw = yaw, roll = 0f))
}
