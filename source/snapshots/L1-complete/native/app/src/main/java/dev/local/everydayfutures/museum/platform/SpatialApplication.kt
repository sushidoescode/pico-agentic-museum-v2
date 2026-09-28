package dev.local.everydayfutures.museum.platform

import android.app.Application
import com.pico.spatial.ui.foundation.dsl.launch
import dev.local.everydayfutures.museum.mainApp

/** Template-shaped entry point: `launch` must run once, on the main thread, from Application.onCreate. */
class SpatialApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        launch(::mainApp)
    }
}
