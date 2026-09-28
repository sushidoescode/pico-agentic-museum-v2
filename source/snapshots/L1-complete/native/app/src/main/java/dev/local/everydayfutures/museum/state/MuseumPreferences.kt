package dev.local.everydayfutures.museum.state

import android.content.Context
import android.content.SharedPreferences
import dev.local.everydayfutures.museum.catalogue.Catalogue

/**
 * App-private persistence for the parts of [MuseumState] that outlive a launch: saved objects and
 * comfort preferences. Selection, view and orientation always start fresh. No account, network or telemetry.
 */
class MuseumPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun load(base: MuseumState = MuseumState()): MuseumState {
        val saved = (prefs.getString(KEY_SAVED, null) ?: "")
            .split(SEPARATOR)
            .filter { it.isNotBlank() && Catalogue.contains(it) }
            .distinct()
        val options = ComfortOptions(
            reduceMotion = prefs.getBoolean(KEY_REDUCE_MOTION, false),
            reduceTransparency = prefs.getBoolean(KEY_REDUCE_TRANSPARENCY, false),
            increaseContrast = prefs.getBoolean(KEY_INCREASE_CONTRAST, false),
        )
        return base.copy(savedIds = saved, options = options)
    }

    fun save(state: MuseumState) {
        prefs.edit()
            .putString(KEY_SAVED, state.savedIds.joinToString(SEPARATOR))
            .putBoolean(KEY_REDUCE_MOTION, state.options.reduceMotion)
            .putBoolean(KEY_REDUCE_TRANSPARENCY, state.options.reduceTransparency)
            .putBoolean(KEY_INCREASE_CONTRAST, state.options.increaseContrast)
            .apply()
    }

    private companion object {
        const val FILE = "everyday-futures-museum-v1"
        const val KEY_SAVED = "savedIds"
        const val KEY_REDUCE_MOTION = "reduceMotion"
        const val KEY_REDUCE_TRANSPARENCY = "reduceTransparency"
        const val KEY_INCREASE_CONTRAST = "increaseContrast"
        const val SEPARATOR = ","
    }
}
