package dev.local.everydayfutures.museum.content

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import com.pico.spatial.core.ecs.Entity
import com.pico.spatial.core.ecs.TransformComponent
import com.pico.spatial.core.math.EulerAngles
import com.pico.spatial.ui.foundation.content.SpatialView
import dev.local.everydayfutures.museum.catalogue.Catalogue

private const val TAG = "Museum"

/** Survives recomposition so the disposal effect can destroy exactly what `initial` created. */
private class PresenterHandles {
    val roots = LinkedHashMap<String, Entity>()
    var light: Entity? = null

    fun destroyAll() {
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
            Log.i(TAG, "exhibits-ready count=${handles.roots.size} light=${handles.light != null}")
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
