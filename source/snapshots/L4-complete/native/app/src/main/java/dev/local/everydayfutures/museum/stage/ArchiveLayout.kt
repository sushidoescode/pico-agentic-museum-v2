package dev.local.everydayfutures.museum.stage

/**
 * The room's layout as exported with it from Blender (`assets/room/room-layout.json`): the room model(s), the eighteen
 * exhibit anchors and the named viewpoints, all in the archive's runtime frame (see [Point3]). Parsed on the device by
 * [RoomLayoutParser]; built directly in host tests.
 */
data class RoomLayout(
    val roomModels: List<String>,
    val anchors: List<Anchor>,
    val viewpoints: Map<String, StandPoint>,
    /** Where each bay's title is shown, by bay id, when the room provides a sign position. */
    val signs: Map<String, Placard> = emptyMap(),
) {
    fun anchor(id: String): Anchor? = anchors.firstOrNull { it.id == id }

    fun anchorsInBay(bay: String): List<Anchor> = anchors.filter { it.bay == bay }

    companion object {
        const val ARRIVAL = "arrival"
    }
}

/** What happened to one exhibit while the archive was loading. */
enum class ExhibitLoad { PENDING, READY, FAILED, NO_ANCHOR }

/** The room model's own state. */
enum class RoomLoad { LOADING, READY, FAILED }

/**
 * Bookkeeping for one visit's loading, shown on the archive panel so loading stays observable. Pure data: every
 * transition returns a new value; the presenter records into it on the main thread as loads finish.
 */
data class ArchiveLoad(
    val room: RoomLoad = RoomLoad.LOADING,
    val exhibits: Map<String, ExhibitLoad> = emptyMap(),
    val startedAtMillis: Long = 0L,
    val roomMillis: Long? = null,
    val allMillis: Long? = null,
) {
    val total: Int get() = exhibits.count { it.value != ExhibitLoad.NO_ANCHOR }
    val ready: Int get() = exhibits.count { it.value == ExhibitLoad.READY }
    val failed: Int get() = exhibits.count { it.value == ExhibitLoad.FAILED }
    val pending: Int get() = exhibits.count { it.value == ExhibitLoad.PENDING }

    /** Loading has finished (every exhibit with an anchor is ready or failed, and the room is ready or failed). */
    val settled: Boolean get() = room != RoomLoad.LOADING && pending == 0

    fun roomDone(ok: Boolean, now: Long): ArchiveLoad =
        copy(room = if (ok) RoomLoad.READY else RoomLoad.FAILED, roomMillis = now - startedAtMillis).stamp(now)

    fun exhibitDone(id: String, ok: Boolean, now: Long): ArchiveLoad =
        if (exhibits[id] != ExhibitLoad.PENDING) this
        else copy(exhibits = exhibits + (id to if (ok) ExhibitLoad.READY else ExhibitLoad.FAILED)).stamp(now)

    private fun stamp(now: Long): ArchiveLoad = if (settled && allMillis == null) copy(allMillis = now - startedAtMillis) else this

    companion object {
        /** A new visit: every catalogue id with an anchor is pending; ids the layout does not place are recorded as such. */
        fun start(catalogueIds: List<String>, layout: RoomLayout?, now: Long): ArchiveLoad = ArchiveLoad(
            room = if (layout == null) RoomLoad.FAILED else RoomLoad.LOADING,
            exhibits = catalogueIds.associateWith { id ->
                if (layout?.anchor(id) != null) ExhibitLoad.PENDING else ExhibitLoad.NO_ANCHOR
            },
            startedAtMillis = now,
        )
    }
}
