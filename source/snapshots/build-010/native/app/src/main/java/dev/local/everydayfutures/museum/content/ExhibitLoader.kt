package dev.local.everydayfutures.museum.content

import android.content.Context
import android.os.SystemClock
import android.util.Log
import com.pico.spatial.core.ecs.Entity
import com.pico.spatial.core.ecs.TransformComponent
import com.pico.spatial.core.ecs.resource.ResourceLoadingException
import com.pico.spatial.core.math.EulerAngles
import com.pico.spatial.core.math.Vector3
import dev.local.everydayfutures.museum.catalogue.Artifact
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.withTimeoutOrNull

private const val TAG = "Museum"

/**
 * Loads one exhibit's packaged model for a surface that shows one object at a time (the study or the inspector) and
 * fits it into that surface's [FitBox]: `<prefix>_<id>_root` (takes the surface's yaw) > `_fit` (uniform scale and
 * offset) > the loaded model, whose own node transforms are left as authored. Everything runs on the calling (main)
 * thread except the asset existence check; the SDK's suspend loader suspends rather than blocks, and cancelling the
 * calling coroutine cancels the native load. Nothing is added to a scene here, and nothing is left behind on failure.
 * The three original objects fall back to their procedural stand-ins; every other object reports an explicit failure.
 */
object ExhibitLoader {
    /** Upper bound on one model load; on expiry the load is cancelled and the attempt fails with `timeout`. */
    const val LOAD_BUDGET_MS = 3000L

    /** One attempt's result: [root] is null when nothing can be shown; [placed] is the model's box in the root's frame. */
    class Loaded(val root: Entity?, val outcome: ModelOutcome, val millis: Long, val placed: ModelBounds?)

    private class Attempt(
        val root: Entity?,
        val cause: String,
        val authoredBounds: Boolean = false,
        val placed: ModelBounds? = null,
        val error: Throwable? = null,
    )

    /** Must be called on the main thread. The caller's cancellation is rethrown, never reported as a failure. */
    suspend fun load(
        context: Context,
        artifact: Artifact,
        box: FitBox,
        namePrefix: String,
        budgetMillis: Long = LOAD_BUDGET_MS,
    ): Loaded {
        val started = SystemClock.uptimeMillis()
        val name = "${namePrefix}_${artifact.id}"
        val model = loadModel(context, artifact, box, name, budgetMillis)
        if (model.root != null) {
            return Loaded(model.root, ModelOutcome.Generated(model.authoredBounds), SystemClock.uptimeMillis() - started, model.placed)
        }
        // A missing file is expected while the collection fills: the caller's one `exhibit-model … reason=` line says so.
        // Anything else is unexpected and keeps its stack trace.
        model.error?.takeUnless { ModelLoadMachine.isMissing(model.cause) }
            ?.let { Log.w(TAG, "exhibit-load-error id=${artifact.id} cause=${model.cause}", it) }
        val fallback = artifact.model.proceduralFallback
        val procedural = if (fallback) buildProcedural(artifact, box, "${name}_procedural") else null
        procedural?.let { p -> p.error?.let { Log.w(TAG, "exhibit-procedural-error id=${artifact.id} cause=${p.cause}", it) } }
        val outcome = ModelLoadMachine.afterModelFailure(model.cause, fallback, procedural?.root != null)
        return Loaded(procedural?.root, outcome, SystemClock.uptimeMillis() - started, procedural?.placed)
    }

    private suspend fun loadModel(context: Context, artifact: Artifact, box: FitBox, name: String, budgetMillis: Long): Attempt {
        val spec = artifact.model
        val path = spec.assetPath ?: return Attempt(null, "uri")
        if (!ModelSource.exists(context, path)) return Attempt(null, "missing")
        val load = runCatching { withTimeoutOrNull(budgetMillis) { ModelSource.load(context, spec.uri) } }
        load.exceptionOrNull()?.let { error ->
            if (error is CancellationException) throw error
            val cause = if (error is ResourceLoadingException) "load-${error.errorCode.name}" else "load-${error.javaClass.simpleName}"
            return Attempt(null, cause, error = error)
        }
        val loaded = load.getOrNull() ?: return Attempt(null, "timeout")
        return fitInto(loaded, spec.anchor, spec.authoredBounds, spec.yawOffsetDegrees, box, name, keepUnmeasured = false)
    }

    /** The procedural stand-in, fitted like a loaded model (or left as authored if the runtime cannot measure it). */
    private fun buildProcedural(artifact: Artifact, box: FitBox, name: String): Attempt {
        val built = runCatching { ExhibitFactory.build(artifact.id) }
        val procedural = built.getOrElse { return Attempt(null, "procedural-${it.javaClass.simpleName}", error = it) }
        // Authored with the object's axis at the origin (the vessel's handle toward +x), like the vessel asset.
        return fitInto(procedural, HorizontalAnchor.MODEL_ORIGIN, null, 0f, box, name, keepUnmeasured = true)
    }

    /**
     * Wraps [model] as root > fit (> orient) > model, measures it in the fit entity's frame (identity until placed, so the
     * model's own transforms count) and applies [ModelFit.fitWithFallback]. On failure everything created here, and the
     * model, is destroyed; with [keepUnmeasured] an unmeasurable model is kept at its authored size instead.
     */
    private fun fitInto(
        model: Entity,
        anchor: HorizontalAnchor,
        authored: ModelBounds?,
        yawOffsetDegrees: Float,
        box: FitBox,
        name: String,
        keepUnmeasured: Boolean,
    ): Attempt {
        var root: Entity? = null
        val fitted = runCatching {
            val built = Entity().apply { setName("${name}_root") }.also { root = it }
            val fit = Entity().apply { setName("${name}_fit") }
            check(built.addChild(fit)) { "attach fit" }
            val holder = if (yawOffsetDegrees != 0f) {
                Entity().apply {
                    setName("${name}_orient")
                    components.get<TransformComponent>()?.setEulerAngles(EulerAngles(pitch = 0f, yaw = yawOffsetDegrees, roll = 0f))
                }.also { check(fit.addChild(it)) { "attach orient" } }
            } else {
                fit
            }
            // A failed native attach would leave an empty root that still "fits"; fail here instead.
            check(holder.addChild(model)) { "attach model" }
            val box3 = model.getVisualBounds(relativeTo = fit, recursive = true, enabledOnly = false)
            val measured = if (box3.isEmpty()) null else ModelBounds(box3.min.x, box3.min.y, box3.min.z, box3.max.x, box3.max.y, box3.max.z)
            val result = ModelFit.fitWithFallback(measured, authored, box, anchor)
            result?.placement?.let { p ->
                fit.components.get<TransformComponent>()?.apply {
                    setScaleVector(Vector3(x = p.scale, y = p.scale, z = p.scale))
                    setPosition(Vector3(x = p.x, y = p.y, z = p.z))
                }
            }
            result
        }
        val result = fitted.getOrNull()
        if (result != null) return Attempt(root, "ok", authoredBounds = result.authoredBounds, placed = result.placed)
        val error = fitted.exceptionOrNull()
        if (error == null && keepUnmeasured && root != null) return Attempt(root, "ok")
        // The model may not have been attached yet, so it is destroyed on its own as well.
        root?.destroy(recursively = true)
        runCatching { model.destroy(recursively = true) }
        return Attempt(null, if (error == null) "bounds" else "fit-${error.javaClass.simpleName}", error = error)
    }

}
