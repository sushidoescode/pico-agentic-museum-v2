package dev.local.everydayfutures.museum

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.pico.spatial.ui.design.systemColorScheme
import com.pico.spatial.ui.foundation.dsl.DefaultWindowContainer
import com.pico.spatial.ui.foundation.dsl.SpatialAppScope
import dev.local.everydayfutures.museum.content.MuseumWindow
import dev.local.everydayfutures.museum.state.MuseumController
import dev.local.picopolish.PolishTheme
import dev.local.picopolish.PolishWindowSurface

/**
 * App entry. The launcher window is the DefaultWindowContainer declared by manifest meta-data
 * (volumetric form, system Material.Regular glass ON). Nothing paints the window root: the polish kit is
 * told the container owns its glass (`windowSurface = SystemGlass`) so its depth budget starts at one.
 */
fun mainApp(scope: SpatialAppScope) = with(scope) {
    DefaultWindowContainer {
        // First child under the root: the window's content bounds (dp), read as parent data by the root layout.
        Box(Modifier.windowConstraints(minWidth = 1120.dp, minHeight = 720.dp, minDepth = 480.dp)) {
            val context = LocalContext.current
            val controller = remember { MuseumController(context) }
            PolishTheme(
                options = controller.polishOptions,
                windowSurface = PolishWindowSurface.SystemGlass,
                // Mirrors PicoTheme's own default so the vibrant scheme applies on PICO OS and the static fallback elsewhere.
                colorScheme = systemColorScheme(context),
            ) {
                MuseumWindow(controller = controller)
            }
        }
    }
}
