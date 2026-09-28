package dev.local.everydayfutures.museum.content

import android.content.Context
import com.pico.spatial.core.ecs.Entity
import com.pico.spatial.core.ecs.ModelFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Loads a packaged model. An `asset://….glb` goes through a descriptor this app opens and closes around the load
 * (`Entity.loadSuspend(afd, ModelFormat.GLTF)`; the SDK's KDoc says the loaded entity does not depend on the descriptor
 * afterwards), because build-005's five archive visits showed the URI loader leaving one native asset open for every
 * model it had loaded, after that model's entities were destroyed (the app's asset count 29 → 105, 19 per visit: the
 * room and eighteen exhibits). `openFd` needs the entry stored uncompressed, which the `.glb` `noCompress` rule
 * guarantees (`scripts/check-apk-stored-models.py`). Any other URI goes to the SDK's URI loader unchanged. Suspends on
 * the calling (main) thread like the SDK loaders; cancelling the caller cancels the load and closes the descriptor.
 */
object ModelSource {
    private const val ASSET_SCHEME = "asset://"

    suspend fun load(context: Context, uri: String): Entity {
        val path = uri.takeIf { it.startsWith(ASSET_SCHEME) && it.endsWith(".glb") }?.removePrefix(ASSET_SCHEME)
            ?: return Entity.loadSuspend(uri)
        return context.assets.openFd(path).use { afd -> Entity.loadSuspend(afd, ModelFormat.GLTF) }
    }

    /**
     * Whether a packaged asset exists at [path] (opened as a stream, so a compressed entry counts as existing): tells a
     * missing file from one `openFd` cannot map because it is not stored.
     */
    suspend fun exists(context: Context, path: String): Boolean = withContext(Dispatchers.IO) {
        runCatching { context.assets.open(path).use { } }.isSuccess
    }
}
