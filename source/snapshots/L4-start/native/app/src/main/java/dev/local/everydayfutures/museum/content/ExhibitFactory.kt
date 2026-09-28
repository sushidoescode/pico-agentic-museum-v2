package dev.local.everydayfutures.museum.content

import android.os.SystemClock
import com.pico.spatial.core.ecs.DirectionalLightComponent
import com.pico.spatial.core.ecs.Entity
import com.pico.spatial.core.ecs.ModelComponent
import com.pico.spatial.core.ecs.TransformComponent
import com.pico.spatial.core.ecs.resource.BlendingMode
import com.pico.spatial.core.ecs.resource.BoxOption
import com.pico.spatial.core.ecs.resource.CapsuleOption
import com.pico.spatial.core.ecs.resource.Material
import com.pico.spatial.core.ecs.resource.MeshResource
import com.pico.spatial.core.ecs.resource.PhysicallyBasedMaterial
import com.pico.spatial.core.ecs.resource.PrimitiveBoxTopology
import com.pico.spatial.core.ecs.resource.PrimitiveCapsuleTopology
import com.pico.spatial.core.ecs.resource.PrimitiveSphereTopology
import com.pico.spatial.core.ecs.resource.ResourceLoadingException
import com.pico.spatial.core.ecs.resource.SphereOption
import com.pico.spatial.core.math.Color4
import com.pico.spatial.core.math.EulerAngles
import com.pico.spatial.core.math.Vector3
import dev.local.everydayfutures.museum.catalogue.Catalogue
import dev.local.everydayfutures.museum.story.Finish
import dev.local.everydayfutures.museum.story.Prop
import dev.local.everydayfutures.museum.story.Shape
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Original procedural stand-ins for the three objects, built from SDK primitive meshes (metres) and
 * physically based materials. They carry known provenance (authored here) and replace nothing imported. The one
 * exception, [generatedQuencher], loads a generated vessel from app assets that the primary window's study may show in
 * the procedural vessel's place; the procedural vessel stays the baseline and fallback.
 * Every function must run on the main thread: Entity(), ModelComponent() and TransformComponent setters
 * enforce it at runtime. Materials and meshes are owned by their ModelComponents and are never closed here.
 */
object ExhibitFactory {
    // 2000 (the SDK sample value) left the slate bodies nearly black in the first opened frames (2026-09-23/24); raised from an opened frame.
    private const val KEY_LIGHT_LUX = 3200f

    /** Shows the generated vessel as the primary window's study model; false keeps the procedural one. The story's hero stays procedural either way. */
    const val GENERATED_VESSEL = true

    /** The generated vessel, packaged in the app's assets so the study needs no network. */
    const val GENERATED_VESSEL_URI = "asset://models/vessel_study.glb"

    /**
     * The procedural vessel's footprint, so the study's composition does not move: base at the capsule body's bottom
     * (0.15 m tall, centred on the root: y = -0.075 m), 0.28 m up to the straw's tip, on the body's axis at x/z = 0.
     */
    private val VESSEL_FIT = FitTarget(height = 0.28f, baseY = -0.075f, centreX = 0f, centreZ = 0f)

    /**
     * The asset is authored with its body's axis at the origin and the handle toward +x (scripts/postprocess-vessel-glb.py),
     * so the origin, not the bounds' centre, goes on the axis: the handle stays off to +x as in the procedural model and
     * the study's yaw turns the body about its own axis. [HorizontalAnchor.BOUNDS_CENTRE] is the choice for an asset
     * without that authoring.
     */
    private val VESSEL_ANCHOR = HorizontalAnchor.MODEL_ORIGIN

    /**
     * The asset's bounds as authored, in its own frame: the GLB's POSITION min/max (±0.1787, ±0.4990, ±0.2627) under its
     * node transform (-90° about +Y, then +0.073 on x), as derived in evidence/polish-001/ASSET-RECORD.md and matching the
     * headless Blender inspection. Used only if the runtime reports an empty box for the not-yet-attached model,
     * so an unmeasurable model is still fitted to the footprint; the marker then says `reason=ok-authored-bounds`.
     */
    private val VESSEL_AUTHORED_BOUNDS = ModelBounds(-0.190f, -0.499f, -0.179f, 0.336f, 0.499f, 0.179f)

    /** One attempt at the generated vessel: [root] is null when the procedural model must stay, and [reason] says why. */
    class GeneratedModel(val root: Entity?, val reason: String, val millis: Long, val error: Throwable? = null)

    /**
     * Loads the generated vessel with the SDK's suspend loader on the calling (main) thread, which suspends rather than
     * blocks: the native load completes through a callback. The model is then measured and fitted to [VESSEL_FIT] and returned under
     * a root named like the procedural one, not yet added anywhere: quencher_root (takes the study's yaw) >
     * quencher_fit (scale and offset) > the loaded model, whose own node transforms are left as authored. The root is null,
     * and nothing created here is left behind, when the asset is missing or unreadable, the load outlasts [budgetMillis]
     * (it is then cancelled), or the bounds cannot be fitted. The caller's cancellation is rethrown, never reported.
     */
    suspend fun generatedQuencher(budgetMillis: Long): GeneratedModel {
        val started = SystemClock.uptimeMillis()
        val load = runCatching { withTimeoutOrNull(budgetMillis) { Entity.loadSuspend(GENERATED_VESSEL_URI) } }
        val millis = SystemClock.uptimeMillis() - started
        load.exceptionOrNull()?.let { error ->
            if (error is CancellationException) throw error
            val reason = if (error is ResourceLoadingException) "load-${error.errorCode.name}" else "load-${error.javaClass.simpleName}"
            return GeneratedModel(root = null, reason = reason, millis = millis, error = error)
        }
        val loaded = load.getOrNull() ?: return GeneratedModel(root = null, reason = "timeout", millis = millis)
        var built: Entity? = null
        var authoredBounds = false
        val fitted = runCatching {
            val fit = Entity().apply { setName("quencher_fit") }
            val quencherRoot = root("quencher_root").also { built = it }
            check(quencherRoot.addChild(fit)) { "attach fit" }
            // A failed native attach would leave an empty root that still "fits"; fail here so the procedural vessel stays.
            check(fit.addChild(loaded)) { "attach model" }
            // Measured in the fit entity's frame (identity until placed), so the loaded model's own transforms count.
            val box = loaded.getVisualBounds(relativeTo = fit, recursive = true, enabledOnly = false)
            val bounds = ModelBounds(box.min.x, box.min.y, box.min.z, box.max.x, box.max.y, box.max.z)
            val placement = ModelFit.fit(bounds, VESSEL_FIT, VESSEL_ANCHOR)
                ?: ModelFit.fit(VESSEL_AUTHORED_BOUNDS, VESSEL_FIT, VESSEL_ANCHOR)?.also { authoredBounds = true }
            placement?.also { p ->
                fit.components.get<TransformComponent>()?.apply {
                    setScaleVector(Vector3(x = p.scale, y = p.scale, z = p.scale))
                    setPosition(Vector3(x = p.x, y = p.y, z = p.z))
                }
            }
        }
        if (fitted.getOrNull() == null) {
            // The loaded model may not have been attached yet, so it is destroyed on its own as well.
            built?.destroy(recursively = true)
            loaded.destroy(recursively = true)
            val error = fitted.exceptionOrNull()
            val reason = if (error == null) "bounds" else "fit-${error.javaClass.simpleName}"
            return GeneratedModel(root = null, reason = reason, millis = millis, error = error)
        }
        return GeneratedModel(root = built, reason = if (authoredBounds) "ok-authored-bounds" else "ok", millis = millis)
    }

    /** Builds the root entity for [artifactId]; unknown ids fall back to the quencher study. */
    fun build(artifactId: String): Entity = when (artifactId) {
        Catalogue.VACUUM -> vacuum()
        Catalogue.SMART_SPEAKER -> speaker()
        else -> quencher()
    }

    /**
     * One warm key light so the PBR surfaces are not left to whatever lighting the window supplies.
     * Intensity is in lux and the SDK's own sample uses 2000 (resolved 6.1.9 source,
     * core-6.1.9-sources.jar!com/pico/spatial/core/ecs/DirectionalLightComponent.kt:52,96-99); tune only from opened frames.
     */
    fun keyLight(): Entity = Entity().apply {
        setName("museum_key_light")
        components.set(DirectionalLightComponent(Color4(red = 1f, green = 0.97f, blue = 0.92f, alpha = 1f), KEY_LIGHT_LUX))
        components.get<TransformComponent>()?.setEulerAngles(EulerAngles(pitch = -40f, yaw = 35f, roll = 0f))
    }

    // Cylinders have no option class in 6.1.9, so createCylinder(height, radius) is the only route (legacy topology).
    private fun quencher(): Entity {
        val body = MeshResource.createCapsule(
            CapsuleOption(height = 0.15f, radius = 0.045f)
                .setTopology(PrimitiveCapsuleTopology.STANDARD)
                .setName("quencher_body"),
        )
        val lid = MeshResource.createCylinder(height = 0.02f, radius = 0.05f)
        val straw = MeshResource.createBox(
            BoxOption(Vector3(x = 0.008f, y = 0.10f, z = 0.008f))
                .setTopology(PrimitiveBoxTopology.STANDARD)
                .setCornerRadius(0.003f)
                .setName("quencher_straw"),
        )
        val handle = MeshResource.createBox(
            BoxOption(Vector3(x = 0.018f, y = 0.09f, z = 0.018f))
                .setTopology(PrimitiveBoxTopology.STANDARD)
                .setCornerRadius(0.008f)
                .setName("quencher_handle"),
        )
        return root("quencher_root").apply {
            addChild(part("quencher_body", body, Palette.slate()))
            addChild(part("quencher_lid", lid, Palette.mist(), position = Vector3(x = 0f, y = 0.095f, z = 0f)))
            addChild(part("quencher_straw", straw, Palette.mist(), position = Vector3(x = 0.02f, y = 0.15f, z = 0f), euler = EulerAngles(pitch = 0f, yaw = 0f, roll = 12f)))
            addChild(part("quencher_handle", handle, Palette.brass(), position = Vector3(x = 0.065f, y = 0f, z = 0f)))
        }
    }

    private fun vacuum(): Entity {
        val puck = MeshResource.createCylinder(height = 0.06f, radius = 0.11f)
        val bumper = MeshResource.createCylinder(height = 0.025f, radius = 0.115f)
        val dome = MeshResource.createCylinder(height = 0.015f, radius = 0.035f)
        val sensor = MeshResource.createSphere(SphereOption(radius = 0.012f).setTopology(PrimitiveSphereTopology.LATLON).setName("vacuum_sensor"))
        return root("vacuum_root").apply {
            addChild(part("vacuum_body", puck, Palette.mist()))
            addChild(part("vacuum_bumper", bumper, Palette.ink(), position = Vector3(x = 0f, y = -0.02f, z = 0f)))
            addChild(part("vacuum_dome", dome, Palette.slate(), position = Vector3(x = 0f, y = 0.037f, z = 0f)))
            addChild(part("vacuum_sensor", sensor, Palette.brass(), position = Vector3(x = 0f, y = 0.03f, z = 0.09f)))
        }
    }

    private fun speaker(): Entity {
        val body = MeshResource.createCapsule(
            CapsuleOption(height = 0.14f, radius = 0.055f)
                .setTopology(PrimitiveCapsuleTopology.STANDARD)
                .setName("speaker_body"),
        )
        val ring = MeshResource.createCylinder(height = 0.006f, radius = 0.058f)
        val base = MeshResource.createCylinder(height = 0.01f, radius = 0.05f)
        return root("speaker_root").apply {
            addChild(part("speaker_body", body, Palette.slate()))
            addChild(part("speaker_ring", ring, Palette.brass(emissive = true), position = Vector3(x = 0f, y = 0.06f, z = 0f)))
            addChild(part("speaker_base", base, Palette.ink(), position = Vector3(x = 0f, y = -0.075f, z = 0f)))
        }
    }

    /**
     * One miniature prop for the L3 story scene, built at its base size so a pose only positions, turns and scales it.
     * [Shape.HERO] is the object's own model from [build]; the presenter scales it to miniature size.
     */
    fun miniatureProp(prop: Prop, objectId: String): Entity {
        if (prop.shape == Shape.HERO) return build(objectId).apply { setName("story_hero_$objectId") }
        val mesh = when (prop.shape) {
            Shape.BOX -> MeshResource.createBox(
                BoxOption(Vector3(x = prop.w, y = prop.h, z = prop.d))
                    .setTopology(PrimitiveBoxTopology.STANDARD)
                    .setCornerRadius(minOf(prop.w, prop.h, prop.d) * 0.2f)
                    .setName("story_${prop.id}"),
            )
            Shape.SPHERE -> MeshResource.createSphere(SphereOption(radius = prop.w).setTopology(PrimitiveSphereTopology.LATLON).setName("story_${prop.id}"))
            Shape.CYLINDER -> MeshResource.createCylinder(height = prop.h, radius = prop.w)
            Shape.CAPSULE -> MeshResource.createCapsule(
                CapsuleOption(height = prop.h, radius = prop.w).setTopology(PrimitiveCapsuleTopology.STANDARD).setName("story_${prop.id}"),
            )
            Shape.HERO -> error("handled above")
        }
        return part("story_${prop.id}", mesh, Palette.of(prop.finish))
    }

    private fun root(name: String): Entity = Entity().apply { setName(name) }

    /** One mesh + one material per entity; the auto-created TransformComponent is configured, never replaced. */
    private fun part(
        name: String,
        mesh: MeshResource,
        material: Material,
        position: Vector3 = Vector3(x = 0f, y = 0f, z = 0f),
        euler: EulerAngles = EulerAngles(pitch = 0f, yaw = 0f, roll = 0f),
    ): Entity = Entity().apply {
        setName(name)
        components.set(ModelComponent(mesh, material))
        components.get<TransformComponent>()?.apply {
            setPosition(position)
            setEulerAngles(euler)
        }
    }

    /**
     * The browser study's palette, approximated as gamma 0..1 channels. PBR defaults are metallic 1 /
     * roughness 1 (dark rough metal), so plastic-like values are set explicitly on every material.
     */
    private object Palette {
        fun of(finish: Finish): Material = when (finish) {
            Finish.SLATE -> slate()
            Finish.MIST -> mist()
            Finish.INK -> ink()
            Finish.BRASS -> brass()
            Finish.GLOW -> brass(emissive = true)
        }

        fun slate(): Material = pbr(red = 0.21f, green = 0.34f, blue = 0.37f, roughness = 0.45f)
        fun mist(): Material = pbr(red = 0.76f, green = 0.82f, blue = 0.81f, roughness = 0.55f)
        fun ink(): Material = pbr(red = 0.08f, green = 0.18f, blue = 0.21f, roughness = 0.6f)
        fun brass(emissive: Boolean = false): Material =
            pbr(red = 0.85f, green = 0.73f, blue = 0.54f, roughness = 0.35f, metallic = 0.6f).apply {
                if (emissive) setEmissiveColor(Color4(red = 0.85f, green = 0.73f, blue = 0.54f, alpha = 1f))
            }

        private fun pbr(red: Float, green: Float, blue: Float, roughness: Float, metallic: Float = 0f): PhysicallyBasedMaterial =
            PhysicallyBasedMaterial.create(BlendingMode.OPAQUE).apply {
                setBaseColor(Color4(red = red, green = green, blue = blue, alpha = 1f))
                setMetallic(metallic)
                setRoughness(roughness)
            }
    }
}
