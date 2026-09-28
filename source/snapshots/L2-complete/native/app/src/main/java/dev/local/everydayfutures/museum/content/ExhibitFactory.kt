package dev.local.everydayfutures.museum.content

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
import com.pico.spatial.core.ecs.resource.SphereOption
import com.pico.spatial.core.math.Color4
import com.pico.spatial.core.math.EulerAngles
import com.pico.spatial.core.math.Vector3
import dev.local.everydayfutures.museum.catalogue.Catalogue

/**
 * Original procedural stand-ins for the three objects, built from SDK primitive meshes (metres) and
 * physically based materials. They carry known provenance (authored here) and replace nothing imported.
 * Every function must run on the main thread: Entity(), ModelComponent() and TransformComponent setters
 * enforce it at runtime. Materials and meshes are owned by their ModelComponents and are never closed here.
 */
object ExhibitFactory {
    // 2000 (the SDK sample value) left the slate bodies nearly black in the first opened frames (2026-09-23/24); raised from an opened frame.
    private const val KEY_LIGHT_LUX = 3200f

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
