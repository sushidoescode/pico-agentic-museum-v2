package dev.local.lunamuseum.reproduction.content

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
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
import com.pico.spatial.ui.platform.containers.OpenStageResult
import com.pico.spatial.ui.platform.containers.SpatialNavigator
import com.pico.spatial.ui.platform.containers.StageStyle
import kotlinx.coroutines.launch

object MuseumRoomStage {
    const val ID = "luna-museum-room"

    @Composable
    fun Content() {
        val navigator = LocalSpatialNavigator.current
        val scope = rememberCoroutineScope()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
        ) {
            Text("Luna Museum room")
            Text("18 exhibits on plinths")
            Spacer(modifier = Modifier.height(8.dp))
            SpatialView(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                update = { _, _ -> },
                initial = { content, _ ->
                    content.addEntity(createRoomScene())
                },
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { scope.launch { close(navigator) } }) {
                Text("Leave museum room")
            }
        }
    }

    suspend fun open(navigator: SpatialNavigator) {
        MuseumRoomStore.clearMessage()
        when (val result = navigator.openStage(ID, style = StageStyle.Full)) {
            OpenStageResult.Allowed -> Unit
            OpenStageResult.NotAllowed -> {
                MuseumRoomStore.showMessage("The museum room cannot open on this device right now.")
            }
            is OpenStageResult.Error -> {
                MuseumRoomStore.showMessage("The museum room could not open: ${result.reason}")
            }
        }
    }

    suspend fun close(navigator: SpatialNavigator) {
        navigator.closeStage()
    }
}

private fun createRoomScene(): Entity {
    val room = Entity()
    MuseumObject.values().forEachIndexed { index, _ ->
        val exhibit = Entity()
        exhibit.components[TransformComponent::class.java] = TransformComponent(
            position = roomPosition(index),
            rotation = EulerAngles(0f, 0f, 0f),
            scale = Vector3(1f, 1f, 1f),
        )
        exhibit.addChild(model(
            MeshResource.createBox(Vector3(0.8f, 0.35f, 0.8f), 0.06f),
            Color4(0.18f, 0.2f, 0.25f, 1f),
            Vector3(0f, -0.3f, 0f),
        ))
        exhibit.addChild(model(
            exhibitMesh(index),
            exhibitColor(index),
            Vector3(0f, 0.15f, 0f),
        ))
        room.addChild(exhibit)
    }
    return room
}

private fun roomPosition(index: Int): Vector3 {
    val column = index % 6
    val row = index / 6
    return Vector3((column - 2.5f) * 1.25f, 0f, (row - 1f) * 1.25f)
}

private fun exhibitMesh(index: Int): MeshResource =
    if (index % 2 == 0) {
        MeshResource.createBox(Vector3(0.55f, 0.55f, 0.55f), 0.08f)
    } else {
        MeshResource.createCylinder(0.28f, 0.6f)
    }

private fun exhibitColor(index: Int): Color4 = Color4(
    0.25f + (index % 4) * 0.12f,
    0.4f + (index % 3) * 0.12f,
    0.7f,
    1f,
)

private fun model(mesh: MeshResource, color: Color4, position: Vector3): ModelEntity {
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

object MuseumRoomStore {
    private val messageState = mutableStateOf<String?>(null)

    val message: String?
        @Synchronized get() = messageState.value

    @Synchronized
    fun showMessage(value: String) {
        messageState.value = value
    }

    @Synchronized
    fun clearMessage() {
        messageState.value = null
    }
}
