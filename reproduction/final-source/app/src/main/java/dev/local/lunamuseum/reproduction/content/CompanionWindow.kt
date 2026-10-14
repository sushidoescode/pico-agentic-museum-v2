package dev.local.lunamuseum.reproduction.content

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pico.spatial.ui.design.Button
import com.pico.spatial.ui.design.Text
import com.pico.spatial.ui.platform.containers.LocalSpatialNavigator
import com.pico.spatial.ui.platform.containers.SpatialNavigator
import kotlinx.coroutines.launch

object CompanionWindow {
    const val ID = "luna-museum-companion"

    fun open(navigator: SpatialNavigator) {
        navigator.openWindowContainer(ID, tag = TAG)
    }

    @Composable
    fun Content() {
        val navigator = LocalSpatialNavigator.current
        val scope = rememberCoroutineScope()
        val selection = MuseumSelectionStore.current
        val story = MuseumStoryStore.current
        val roomMessage = MuseumRoomStore.message

        fun select(item: MuseumObject) {
            MuseumSelectionStore.update(selection.select(item))
            MuseumStoryStore.select(item)
        }

        Column(
            modifier = Modifier
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Text("Companion inspector")
            Spacer(modifier = Modifier.height(8.dp))
            Text("18 exhibits")
            Spacer(modifier = Modifier.height(12.dp))
            Text("Selected: ${selection.selected.title}")
            Text(selection.selected.description)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Quarter turns: ${selection.turnQuarterTurns}")
            Text("Story ${story.stepIndex + 1} of ${story.story.steps.size}: ${story.stepText}")
            roomMessage?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(it)
            }
            Spacer(modifier = Modifier.height(16.dp))
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

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    MuseumStoryStore.update(if (story.isPlaying) story.pause() else story.play())
                }) { Text(if (story.isPlaying) "Pause story" else "Play story") }
                Button(onClick = { MuseumStoryStore.update(story.restart()) }) { Text("Restart story") }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { MuseumSelectionStore.update(selection.turn()) }) { Text("Turn") }
                Button(onClick = {
                    val resetSelection = MuseumSelectionStore.current.reset()
                    MuseumSelectionStore.update(resetSelection)
                    MuseumStoryStore.reset(resetSelection.selected)
                }) { Text("Reset") }
                Button(onClick = { MuseumStoryStore.update(story.toggleReducedMotion()) }) {
                    Text("Reduced motion: ${if (story.reducedMotion) "On" else "Off"}")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    scope.launch { MuseumRoomStage.open(navigator) }
                }) { Text("Enter museum room") }
                Button(onClick = { navigator.closeWindowContainer() }) { Text("Close") }
            }
        }
    }

    private const val TAG = "museum-companion"
}
