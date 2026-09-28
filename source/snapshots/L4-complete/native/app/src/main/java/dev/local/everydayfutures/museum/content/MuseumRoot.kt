package dev.local.everydayfutures.museum.content

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.pico.spatial.ui.design.systemColorScheme
import com.pico.spatial.ui.platform.containers.LocalSpatialNavigator
import com.pico.spatial.ui.platform.containers.OpenStageResult
import com.pico.spatial.ui.platform.containers.SpatialNavigator
import com.pico.spatial.ui.platform.containers.StageStyle
import dev.local.everydayfutures.museum.state.ComfortOptions
import dev.local.everydayfutures.museum.state.MuseumController
import dev.local.everydayfutures.museum.state.StageNavigator
import dev.local.everydayfutures.museum.state.StageOpenResult
import dev.local.everydayfutures.museum.state.Surface
import dev.local.everydayfutures.museum.state.WindowNavigator
import dev.local.picopolish.PolishOptions
import dev.local.picopolish.PolishTheme
import dev.local.picopolish.PolishWindowSurface

/**
 * The shared shell of every composition root: reports the root's composition entering and leaving to the app-owned
 * [controller], logs what the root shows whenever the shared logical state changes, and applies the one set of comfort
 * preferences through the polish kit. [windowSurface] states who owns the container's background: `SystemGlass`
 * (the default: the primary's manifest glass and the companion's DSL default) or `AppOpaque` (the inspector, declared
 * with its material background off, whose root paints nothing and spends no glass layer). Nothing paints the root.
 */
@Composable
fun MuseumRoot(
    controller: MuseumController,
    surface: Surface,
    windowSurface: PolishWindowSurface = PolishWindowSurface.SystemGlass,
    content: @Composable () -> Unit,
) {
    DisposableEffect(controller, surface) {
        controller.windowAttached(surface)
        onDispose { controller.windowDetached(surface) }
    }
    val state = controller.state
    // Keyed on every shared field a window displays, so each change is correlated by one marker per window.
    LaunchedEffect(
        state.selectedId, state.turnSteps, state.view, state.options, state.savedIds, state.story,
        state.filter, state.inspector, state.archive,
    ) {
        controller.windowShows(surface)
    }
    PolishTheme(
        options = state.options.toPolishOptions(),
        windowSurface = windowSurface,
        // Mirrors PicoTheme's own default so the vibrant scheme applies on PICO OS and the static fallback elsewhere.
        colorScheme = systemColorScheme(LocalContext.current),
    ) {
        content()
    }
}

/** The comfort preferences in the polish kit's own option type; the kit ships no animations, so reduceMotion is app policy. */
fun ComfortOptions.toPolishOptions(): PolishOptions =
    PolishOptions(reduceMotion = reduceMotion, reduceTransparency = reduceTransparency, increaseContrast = increaseContrast)

/**
 * Adapts this window's SDK navigator to the controller's two calls. No bundle is ever sent. A pending auxiliary window
 * is opened later, when the other one's detach is observed, through the navigator of the window that asked for it; if
 * that window has gone meanwhile the call is logged rather than allowed to throw into the detach.
 */
private class SdkWindowNavigator(private val navigator: SpatialNavigator) : WindowNavigator {
    override fun open(id: String, tag: String) {
        runCatching { navigator.openWindowContainer(id = id, tag = tag, bundle = null) }
            .onFailure { Log.e(MuseumController.TAG, "window-open-failed id=$id tag=$tag", it) }
    }

    override fun close(id: String, tag: String) {
        runCatching { navigator.closeWindowContainer(id = id, tag = tag) }
            .onFailure { Log.e(MuseumController.TAG, "window-close-failed id=$id tag=$tag", it) }
    }
}

/** The navigator of the window this composable runs in (provided by the SDK in every window root). */
@Composable
fun rememberWindowNavigator(): WindowNavigator {
    val navigator = LocalSpatialNavigator.current
    return remember(navigator) { SdkWindowNavigator(navigator) }
}

/**
 * Adapts the SDK navigator's Stage calls to [StageNavigator]. The archive is always opened with the Full style: a
 * DSL-declared Stage stores AUTOMATIC (currently Mixed), so the style has to be passed here. `closeStage` takes no id
 * and is a no-op when no Stage is alive; minimize/restore work only while a Stage is open (they return false otherwise).
 */
private class SdkStageNavigator(private val navigator: SpatialNavigator) : StageNavigator {
    override suspend fun open(id: String): StageOpenResult =
        when (val result = navigator.openStage(id = id, style = StageStyle.Full)) {
            is OpenStageResult.Error -> StageOpenResult.Error(result.code, result.reason)
            OpenStageResult.Allowed -> StageOpenResult.Allowed
            else -> StageOpenResult.NotAllowed
        }

    override suspend fun close() = navigator.closeStage()

    override fun minimize(id: String, tag: String?): Boolean = navigator.minimizeWindowContainer(id = id, tag = tag)

    override fun restore(id: String, tag: String?): Boolean = navigator.restoreWindowContainer(id = id, tag = tag)
}

/** The Stage calls of the window this composable runs in. */
@Composable
fun rememberStageNavigator(): StageNavigator {
    val navigator = LocalSpatialNavigator.current
    return remember(navigator) { SdkStageNavigator(navigator) }
}
