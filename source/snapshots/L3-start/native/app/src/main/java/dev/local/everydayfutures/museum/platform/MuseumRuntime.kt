package dev.local.everydayfutures.museum.platform

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.pico.spatial.core.platform.SpatialBuild
import dev.local.everydayfutures.museum.state.MuseumController
import dev.local.everydayfutures.museum.state.MuseumPreferences
import dev.local.everydayfutures.museum.state.MuseumState
import dev.local.everydayfutures.museum.state.StateCell

/**
 * Compose snapshot state behind the controller. Every window root runs on the main thread of one process, so a write
 * made from either window's event handler is applied to the global snapshot and recomposes every root that reads it.
 */
private class SnapshotCell<T>(initial: T) : StateCell<T> {
    override var value: T by mutableStateOf(initial)
}

/** Builds the process's single [MuseumController]: app-private persistence, snapshot state and logcat markers. */
fun createMuseumController(context: Context): MuseumController {
    val controller = MuseumController(
        persistence = MuseumPreferences(context.applicationContext),
        cellFactory = { initial: MuseumState -> SnapshotCell(initial) },
        log = { Log.i(MuseumController.TAG, it) },
    )
    // Off PICO OS the SDK renders SpatialView as a stub and never runs its lambdas; make that visible in logcat.
    // Logged once per process: two lines in one process would mean two owners.
    val s = controller.state
    Log.i(MuseumController.TAG, "startup spatialPlatform=${SpatialBuild.isSpatialPlatform()} selected=${s.selectedId} saved=${s.savedIds.size} owner=app")
    return controller
}
