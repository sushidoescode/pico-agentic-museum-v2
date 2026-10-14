package dev.local.lunamuseum.reproduction.content

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.pico.spatial.core.ecs.Entity
import com.pico.spatial.core.ecs.ModelEntity
import com.pico.spatial.core.ecs.TransformComponent
import com.pico.spatial.core.ecs.resource.MeshResource
import com.pico.spatial.core.ecs.resource.UnlitMaterial
import com.pico.spatial.core.math.Color4
import com.pico.spatial.core.math.EulerAngles
import com.pico.spatial.core.math.Vector3
import com.pico.spatial.ui.design.Button
import com.pico.spatial.ui.design.Text
import com.pico.spatial.ui.foundation.content.SpatialView
import com.pico.spatial.ui.platform.containers.LocalSpatialNavigator
import kotlinx.coroutines.delay

@Composable
fun HomeVolume(onOpenCompanion: () -> Unit) {
    val selection = MuseumSelectionStore.current
    val story = MuseumStoryStore.current
    var museumScene by remember { mutableStateOf<MuseumScene?>(null) }
    val navigator = LocalSpatialNavigator.current

    LaunchedEffect(story.isPlaying, story.selected) {
        while (MuseumStoryStore.current.isPlaying) {
            delay(STORY_STEP_DELAY_MS)
            MuseumStoryStore.update(MuseumStoryStore.current.advance())
        }
    }

    fun select(item: MuseumObject) {
        MuseumSelectionStore.update(selection.select(item))
        MuseumStoryStore.select(item)
    }

    Column(
        modifier = Modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Text("Luna Museum")
        Spacer(modifier = Modifier.height(8.dp))
        Text("Select one of 18 exhibits to inspect it in 3D.")
        Spacer(modifier = Modifier.height(12.dp))

        SpatialView(
            modifier = Modifier
                .fillMaxWidth()
                .size(260.dp)
                .clip(RoundedCornerShape(18.dp)),
            update = { _, _ -> museumScene?.apply(selection, story) },
            initial = { content, _ ->
                val scene = createMuseumScene()
                museumScene = scene
                content.addEntity(scene.root)
                scene.apply(selection, story)
            },
        )

        Spacer(modifier = Modifier.height(12.dp))
        Text("${selection.selected.title}${if (selection.selected.isPrimitivePlaceholder) " (primitive placeholder)" else ""}")
        Text(selection.selected.description)
        Spacer(modifier = Modifier.height(10.dp))
        Text("Story ${story.stepIndex + 1} of ${story.story.steps.size}: ${story.stepText}")
        Spacer(modifier = Modifier.height(12.dp))
        Text("Catalogue")
        Spacer(modifier = Modifier.height(6.dp))

        MuseumObject.values().forEachIndexed { index, item ->
            Button(
                onClick = { select(item) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("${index + 1}. ${item.title}${if (item.isPrimitivePlaceholder) " (primitive placeholder)" else ""}")
            }
            Spacer(modifier = Modifier.height(5.dp))
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                MuseumStoryStore.update(if (story.isPlaying) story.pause() else story.play())
            }) { Text(if (story.isPlaying) "Pause story" else "Play story") }
            Button(onClick = { MuseumStoryStore.update(story.restart()) }) { Text("Restart story") }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { MuseumStoryStore.update(story.toggleReducedMotion()) }) {
                Text("Reduced motion: ${if (story.reducedMotion) "On" else "Off"}")
            }
            Button(onClick = { MuseumSelectionStore.update(selection.turn()) }) { Text("Turn") }
            Button(onClick = {
                val resetSelection = MuseumSelectionStore.current.reset()
                MuseumSelectionStore.update(resetSelection)
                MuseumStoryStore.reset(resetSelection.selected)
            }) { Text("Reset") }
            Button(onClick = onOpenCompanion) { Text("Open companion") }
        }
    }
}

private const val STORY_STEP_DELAY_MS = 900L

private class MuseumScene(
    val root: Entity,
    private val objects: Map<MuseumObject, Entity>,
) {
    fun apply(selection: MuseumSelection, story: MuseumStoryState) {
        objects.forEach { (museumObject, entity) ->
            entity.enabled = museumObject == selection.selected
            if (museumObject == selection.selected) {
                val transform = entity.components[TransformComponent::class.java] ?: return@forEach
                val motion = museumStoryMotion(museumObject, story.stepIndex, story.reducedMotion)
                transform.position = Vector3(motion.positionX, motion.positionY, motion.positionZ)
                transform.eulerAngles = EulerAngles(
                    motion.pitch,
                    selection.turnQuarterTurns * 90f + motion.yaw,
                    motion.roll,
                )
            }
        }
    }
}

private fun createMuseumScene(): MuseumScene {
    val root = Entity()
    val objects = linkedMapOf<MuseumObject, Entity>()
    MuseumObject.values().forEachIndexed { index, museumObject ->
        val entity = when (museumObject) {
            MuseumObject.HANDLED_CUP -> createCup()
            MuseumObject.ROBOT_VACUUM -> createRobotVacuum()
            MuseumObject.SMART_SPEAKER -> createSmartSpeaker()
            else -> createPrimitivePlaceholder(index)
        }
        root.addChild(entity)
        objects[museumObject] = entity
    }
    return MuseumScene(root, objects)
}

private fun createCup(): Entity {
    val cup = baseEntity(Vector3(0.55f, 0.55f, 0.55f))
    cup.addChild(model(MeshResource.createCylinder(0.7f, 0.42f), Color4(0.9f, 0.35f, 0.25f, 1f)))
    cup.addChild(model(MeshResource.createCylinder(0.08f, 0.5f), Color4(1f, 0.75f, 0.35f, 1f), Vector3(0f, 0.38f, 0f)))
    cup.addChild(model(MeshResource.createBox(Vector3(0.12f, 0.42f, 0.12f), 0.04f), Color4(1f, 0.7f, 0.25f, 1f), Vector3(0.42f, 0f, 0f)))
    return cup
}

private fun createRobotVacuum(): Entity {
    val vacuum = baseEntity(Vector3(0.8f, 0.8f, 0.8f))
    vacuum.addChild(model(MeshResource.createCylinder(0.25f, 0.55f), Color4(0.18f, 0.25f, 0.32f, 1f)))
    vacuum.addChild(model(MeshResource.createCylinder(0.08f, 0.3f), Color4(0.25f, 0.75f, 0.8f, 1f), Vector3(0f, 0.28f, 0f)))
    vacuum.addChild(model(MeshResource.createSphere(0.1f), Color4(0.9f, 0.9f, 0.95f, 1f), Vector3(0f, 0.36f, 0f)))
    return vacuum
}

private fun createSmartSpeaker(): Entity {
    val speaker = baseEntity(Vector3(0.65f, 0.65f, 0.65f))
    speaker.addChild(model(MeshResource.createBox(Vector3(0.7f, 0.9f, 0.7f), 0.18f), Color4(0.35f, 0.28f, 0.55f, 1f)))
    speaker.addChild(model(MeshResource.createCylinder(0.08f, 0.2f), Color4(0.35f, 0.9f, 0.75f, 1f), Vector3(0f, 0.5f, 0f)))
    return speaker
}

private fun createPrimitivePlaceholder(index: Int): Entity {
    val entity = baseEntity(Vector3(0.65f, 0.65f, 0.65f))
    val color = Color4(0.2f + (index % 3) * 0.15f, 0.45f + (index % 2) * 0.2f, 0.75f, 1f)
    val mesh = if (index % 2 == 0) {
        MeshResource.createBox(Vector3(0.7f, 0.7f, 0.7f), 0.08f)
    } else {
        MeshResource.createCylinder(0.35f, 0.7f)
    }
    entity.addChild(model(mesh, color))
    return entity
}

private fun baseEntity(scale: Vector3): Entity = Entity().also {
    it.components[TransformComponent::class.java] = TransformComponent(
        position = Vector3(0f, 0f, 0f),
        rotation = EulerAngles(0f, 0f, 0f),
        scale = scale,
    )
}

private fun model(mesh: MeshResource, color: Color4, position: Vector3 = Vector3(0f, 0f, 0f)): ModelEntity {
    val material = UnlitMaterial.create()
    material.setBaseColor(color)
    return ModelEntity(mesh, material).also {
        it.components[TransformComponent::class.java] = TransformComponent(
            position = position,
            rotation = EulerAngles(0f, 0f, 0f),
            scale = Vector3(1f, 1f, 1f),
        )
    }
}
