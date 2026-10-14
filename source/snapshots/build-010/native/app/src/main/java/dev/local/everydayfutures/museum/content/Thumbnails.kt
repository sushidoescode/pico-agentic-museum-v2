package dev.local.everydayfutures.museum.content

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "Museum"

/**
 * The catalogue's thumbnails: 256 px PNGs in the app's assets, decoded off the main thread at half size (the grid draws
 * them at about 44 dp) and kept in a small in-memory cache for the process. A missing or undecodable file is cached as
 * missing (logged once) and the cell shows its number and name instead; nothing here throws into the UI.
 */
object ThumbnailStore {
    private sealed class Entry {
        class Image(val bitmap: ImageBitmap) : Entry()

        object Missing : Entry()
    }

    /** Eighteen thumbnails at 128 px are about 1.2 MB; the cache holds them all. */
    private val cache = LruCache<String, Entry>(32)

    /** Decoded already (or known missing): the image, or null. Never touches storage. */
    fun peek(path: String): ImageBitmap? = (cache.get(path) as? Entry.Image)?.bitmap

    /** The thumbnail at [path] inside the assets, decoded on the IO dispatcher; null when missing or corrupt. */
    suspend fun load(context: Context, path: String): ImageBitmap? {
        cache.get(path)?.let { return (it as? Entry.Image)?.bitmap }
        val entry = withContext(Dispatchers.IO) { decode(context.applicationContext, path) }
        cache.put(path, entry)
        return (entry as? Entry.Image)?.bitmap
    }

    private fun decode(context: Context, path: String): Entry {
        val decoded = runCatching {
            context.assets.open(path).use { stream ->
                BitmapFactory.decodeStream(stream, null, BitmapFactory.Options().apply { inSampleSize = 2 })
            }
        }
        val bitmap = decoded.getOrNull()
        if (bitmap == null) {
            Log.w(TAG, "thumbnail-missing path=$path reason=${decoded.exceptionOrNull()?.javaClass?.simpleName ?: "undecodable"}")
            return Entry.Missing
        }
        return runCatching { Entry.Image(bitmap.asImageBitmap().also { it.prepareToDraw() }) }.getOrElse { Entry.Missing }
    }
}

/** The thumbnail at [path], or null while it loads and when it is missing (the caller shows a placeholder). */
@Composable
fun rememberThumbnail(path: String): ImageBitmap? {
    val context = LocalContext.current
    val image by produceState(initialValue = ThumbnailStore.peek(path), path) {
        value = ThumbnailStore.load(context, path)
    }
    return image
}
