package dev.local.everydayfutures.museum

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.pico.spatial.ui.design.systemColorScheme
import com.pico.spatial.ui.foundation.dsl.DefaultWindowContainer
import com.pico.spatial.ui.foundation.dsl.SpatialAppScope
import dev.local.everydayfutures.museum.content.MuseumWindow
import dev.local.everydayfutures.museum.state.rememberMuseumController
import dev.local.picopolish.PolishTheme
import dev.local.picopolish.PolishWindowSurface

/**
 * App entry. The launcher window is the DefaultWindowContainer declared by manifest meta-data
 * (volumetric form, system Material.Regular glass ON). Nothing paints the window root: PolishTheme sits at
 * the container's root and is told the container owns its glass (`windowSurface = SystemGlass`), so the
 * kit's depth budget starts at one. The window's content bounds go on the first composable inside the theme.
 */
fun mainApp(scope: SpatialAppScope) = with(scope) {
    DefaultWindowContainer {
        val controller = rememberMuseumController()
        PolishTheme(
            options = controller.polishOptions,
            windowSurface = PolishWindowSurface.SystemGlass,
            // Mirrors PicoTheme's own default so the vibrant scheme applies on PICO OS and the static fallback elsewhere.
            colorScheme = systemColorScheme(LocalContext.current),
        ) {
            MuseumWindow(
                controller = controller,
                modifier = Modifier.windowConstraints(minWidth = 1120.dp, minHeight = 720.dp, minDepth = 480.dp),
            )
        }
    }
}
