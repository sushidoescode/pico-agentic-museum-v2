package dev.local.everydayfutures.museum.platform

import android.app.Application
import com.pico.spatial.ui.foundation.dsl.launch
import dev.local.everydayfutures.museum.mainApp
import dev.local.everydayfutures.museum.state.MuseumController

/**
 * Template-shaped entry point: `launch` must run once, on the main thread, from Application.onCreate. The app-owned
 * state owner is created here, once per process, before any window root exists, and every root receives this instance.
 */
class SpatialApplication : Application() {
    lateinit var museum: MuseumController
        private set

    override fun onCreate() {
        super.onCreate()
        museum = createMuseumController(this)
        val owner = museum
        launch { mainApp(this, owner) }
    }
}
