package dev.local.lunamuseum.reproduction.content

import androidx.compose.runtime.mutableStateOf

object MuseumSelectionStore {
    private val selectionState = mutableStateOf(MuseumSelection())

    val current: MuseumSelection
        @Synchronized get() = selectionState.value

    @Synchronized
    fun update(next: MuseumSelection) {
        selectionState.value = next
    }

    @Synchronized
    fun reset() {
        selectionState.value = MuseumSelection()
    }
}
