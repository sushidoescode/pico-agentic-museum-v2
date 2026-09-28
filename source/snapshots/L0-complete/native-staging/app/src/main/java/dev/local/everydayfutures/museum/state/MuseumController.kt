package dev.local.everydayfutures.museum.state

import android.content.Context
import android.util.Log
import com.pico.spatial.core.platform.SpatialBuild
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.local.picopolish.PolishOptions

/**
 * Compose-facing owner of [MuseumState]. Every user action goes through one reducer, is logged with a
 * stable marker (tag [TAG]) so emulator evidence can correlate a counted action with a frame, and
 * persists only the parts that outlive a launch.
 */
class MuseumController(context: Context) {
    private val preferences = MuseumPreferences(context)
    private var actionCount = 0

    var state: MuseumState by mutableStateOf(preferences.load())
        private set

    init {
        // Off PICO OS the SDK renders SpatialView as a stub and never runs its lambdas; make that visible in logcat.
        Log.i(TAG, "startup spatialPlatform=${SpatialBuild.isSpatialPlatform()} selected=${state.selectedId} saved=${state.savedIds.size}")
    }

    /** The comfort preferences in the polish kit's own option type; the kit ships no animations, so reduceMotion is app policy. */
    val polishOptions: PolishOptions
        get() = PolishOptions(
            reduceMotion = state.options.reduceMotion,
            reduceTransparency = state.options.reduceTransparency,
            increaseContrast = state.options.increaseContrast,
        )

    fun select(id: String) = commit("select id=$id") { MuseumReducer.select(it, id) }

    fun turn(direction: Turn) = commit("turn direction=${direction.name.lowercase()}") { MuseumReducer.turn(it, direction) }

    fun toggleInspect() = commit("inspect-toggle") { MuseumReducer.toggleInspect(it) }

    fun reset() = commit("reset") { MuseumReducer.reset(it) }

    fun keep() = commit("keep id=${state.selectedId}", persist = true) { MuseumReducer.keep(it) }

    fun setReduceMotion(on: Boolean) = setOptions(state.options.copy(reduceMotion = on), "reduceMotion=$on")

    fun setReduceTransparency(on: Boolean) = setOptions(state.options.copy(reduceTransparency = on), "reduceTransparency=$on")

    fun setIncreaseContrast(on: Boolean) = setOptions(state.options.copy(increaseContrast = on), "increaseContrast=$on")

    private fun setOptions(options: ComfortOptions, marker: String) =
        commit("option $marker", persist = true) { MuseumReducer.setOptions(it, options) }

    private fun commit(marker: String, persist: Boolean = false, reduce: (MuseumState) -> MuseumState) {
        val next = reduce(state)
        actionCount += 1
        state = next
        if (persist) preferences.save(next)
        Log.i(TAG, "$marker steps=${next.turnSteps} selected=${next.selectedId} view=${next.view.name.lowercase()} action=#$actionCount")
    }

    companion object {
        /** Logcat tag; acceptance cases grep `Museum: select id=…`, `Museum: turn …`, `Museum: reset`. */
        const val TAG = "Museum"
    }
}
