package dev.local.everydayfutures.museum.content

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import com.pico.spatial.core.container.SpatialViewContent
import com.pico.spatial.core.ecs.Entity
import com.pico.spatial.core.ecs.TransformComponent
import com.pico.spatial.core.math.EulerAngles
import com.pico.spatial.ui.foundation.content.SpatialView
import dev.local.everydayfutures.museum.catalogue.Catalogue

private const val TAG = "Museum"

/**
 * Upper bound on the generated vessel's load. `initial` waits for it and SpatialView runs `update` only once `initial`
 * has returned, so a selection or turn made meanwhile reaches the study late (not lost: `update` writes absolute state).
 * The load suspends without blocking the main thread; this bound keeps a slow or stalled runtime from holding the
 * study's first update back, and on expiry the load is cancelled and the procedural vessel stays.
 */
private const val GENERATED_LOAD_BUDGET_MS = 3000L

/** Survives recomposition so the disposal effect can destroy exactly what `initial` created. */
private class PresenterHandles {
    val roots = LinkedHashMap<String, Entity>()
    var light: Entity? = null
    var disposed = false

    fun destroyAll() {
        disposed = true
        roots.values.forEach { it.destroy(recursively = true) }
        roots.clear()
        light?.destroy(recursively = true)
        light = null
    }
}

/**
 * Hosts the study scene. All three objects are built once in `initial` (main thread) and kept; selection
 * toggles `Entity.enabled`, and every turn writes an ABSOLUTE yaw derived from state. `update` runs after
 * `initial`, on every layout reposition and whenever the state it reads changes, so it must stay idempotent.
 * With [ExhibitFactory.GENERATED_VESSEL] on, `initial` then tries the generated vessel and, if it loads and fits, puts
 * it in the procedural vessel's place ([swapInGeneratedVessel]); markers: `exhibit-model`, then `exhibits-ready … model=`.
 * SpatialView only detaches entities on release; this composable destroys them itself when it leaves composition.
 */
@Composable
fun ObjectPresenter(selectedId: String, turnDegrees: Float, modifier: Modifier = Modifier) {
    val handles = remember { PresenterHandles() }
    val selected by rememberUpdatedState(selectedId)
    val yaw by rememberUpdatedState(turnDegrees)

    DisposableEffect(Unit) {
        onDispose { handles.destroyAll() }
    }

    SpatialView(
        modifier = modifier,
        initial = { content, _ ->
            // Mesh and material factories can throw ResourceLoadingException; an unhandled throw inside the
            // SDK's launched scope would take the process down, so each object is built under its own guard.
            runCatching { ExhibitFactory.keyLight() }
                .onSuccess { light -> handles.light = light; content.addEntity(light) }
                .onFailure { Log.e(TAG, "key light not created", it) }
            Catalogue.artifacts.forEach { artifact ->
                runCatching { ExhibitFactory.build(artifact.id) }
                    .onSuccess { root ->
                        root.enabled = artifact.id == selected
                        root.components.get<TransformComponent>()?.setEulerAngles(EulerAngles(pitch = 0f, yaw = 0f, roll = 0f))
                        handles.roots[artifact.id] = root
                        content.addEntity(root)
                    }
                    .onFailure { Log.e(TAG, "exhibit-build-failed id=${artifact.id}", it) }
            }
            // Here rather than in a later coroutine: SpatialView calls `update` only after `initial` returns, so no update
            // can interleave with the swap, and exhibits-ready names the model the study actually shows. The procedural
            // vessel is already added and drawn while the generated one loads, and stays on any failure.
            val model = if (ExhibitFactory.GENERATED_VESSEL) {
                swapInGeneratedVessel(content, handles) { selected }
            } else {
                Log.i(TAG, "exhibit-model id=${Catalogue.QUENCHER} model=procedural ms=0 reason=off")
                "procedural"
            }
            Log.i(TAG, "exhibits-ready count=${handles.roots.size} light=${handles.light != null} model=$model")
        },
        update = { _, _ ->
            val currentId = selected
            val currentYaw = yaw
            handles.roots.forEach { (id, root) ->
                if (!root.valid) return@forEach
                val isCurrent = id == currentId
                root.enabled = isCurrent
                if (isCurrent) {
                    root.components.get<TransformComponent>()
                        ?.setEulerAngles(EulerAngles(pitch = 0f, yaw = currentYaw, roll = 0f))
                }
            }
        },
    )
}

/**
 * Loads the generated vessel and, when it loads and fits, puts it in the procedural vessel's place: added to the same
 * content (so under the same parent), enabled by the rule `update` applies, at the procedural root's current rotation,
 * and registered under the quencher's id so `update` and disposal manage it; the procedural root is then hidden,
 * removed and destroyed. On any failure the procedural root stays and nothing generated is left behind. Logs one
 * `exhibit-model` marker (with the failure, if any) and returns the model the study shows.
 */
private suspend fun swapInGeneratedVessel(content: SpatialViewContent, handles: PresenterHandles, selectedId: () -> String): String {
    val generated = ExhibitFactory.generatedQuencher(GENERATED_LOAD_BUDGET_MS)
    val candidate = generated.root
    var model = "procedural"
    var reason = generated.reason
    var error = generated.error
    if (candidate != null && handles.disposed) {
        candidate.destroy(recursively = true)
        reason = "disposed"
    } else if (candidate != null) {
        val procedural = handles.roots[Catalogue.QUENCHER]
        runCatching {
            // Derived like `update` does rather than read back: `enabled` reads false whenever a parent is disabled.
            candidate.enabled = selectedId() == Catalogue.QUENCHER
            procedural?.components?.get<TransformComponent>()?.quaternion?.let { rotation ->
                candidate.components.get<TransformComponent>()?.setQuaternion(rotation)
            }
            content.addEntity(candidate)
        }.onSuccess {
            handles.roots[Catalogue.QUENCHER] = candidate
            model = "generated"
            procedural?.let { old ->
                runCatching {
                    old.enabled = false
                    content.removeEntity(old)
                    old.destroy(recursively = true)
                }.onFailure { Log.e(TAG, "exhibit-model procedural root not removed", it) }
            }
        }.onFailure {
            candidate.destroy(recursively = true)
            reason = "swap-${it.javaClass.simpleName}"
            error = it
        }
    }
    Log.i(TAG, "exhibit-model id=${Catalogue.QUENCHER} model=$model ms=${generated.millis} reason=$reason", error)
    return model
}
