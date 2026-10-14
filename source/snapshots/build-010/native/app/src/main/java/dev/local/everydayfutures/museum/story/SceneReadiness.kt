package dev.local.everydayfutures.museum.story

/** What the companion's miniature region shows for the story's object. */
enum class SceneStatus {
    /** The scene is still being created: the region shows a readable preparing state instead of an empty volume. */
    PREPARING,

    /** The object's miniature exists in the scene, every prop created: the preparing state is removed. */
    READY,

    /** The object's miniature could not be created completely: the region says so and offers Try again or Return. */
    FAILED,
}

/**
 * The record of one attempt at creating the companion's story scene: which miniatures exist completely and which could
 * not be created. The region's state is derived from it for whichever object the story shows, so the preparing state
 * clears only when that object's miniature exists, and an incomplete or failed miniature is never reported as ready.
 * Pure data; the presenter records into it as it builds.
 */
data class SceneBuild(val built: Set<String> = emptySet(), val failed: Set<String> = emptySet()) {
    fun status(objectId: String): SceneStatus = when (objectId) {
        in built -> SceneStatus.READY
        in failed -> SceneStatus.FAILED
        else -> SceneStatus.PREPARING
    }

    /** Records one miniature's result; [complete] is true only when its root and every prop were created. */
    fun record(objectId: String, complete: Boolean): SceneBuild =
        if (complete) copy(built = built + objectId, failed = failed - objectId) else copy(built = built - objectId, failed = failed + objectId)

    companion object {
        /** Attempts per story view: the first creation plus two Try agains; after that only Return and Close are offered. */
        const val MAX_ATTEMPTS = 3

        fun canRetry(attempt: Int): Boolean = attempt < MAX_ATTEMPTS
    }
}
