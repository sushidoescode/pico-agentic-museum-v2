package dev.local.everydayfutures.museum.stage

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject

/**
 * Reads the room layout exported with the Blender room (`assets/room/room-layout.json`, schema 1: `room.glb[].path`,
 * `room.entrance.marker[3]?`, `anchors[] {id, bay, position[3], yawDeg, displayScale, plaque?}`,
 * `viewpoints[] {name, position[3], yawDeg}`, `bays[] {id, sign?}`; a plaque/sign is
 * `{position[3], yawDeg, pitchDeg?, sizeMetres[2]?}`). Returns null, and logs why, when the file is missing or
 * unreadable; the archive then says the room is unavailable and keeps Return.
 */
object RoomLayoutParser {
    const val PATH = "room/room-layout.json"
    private const val TAG = "Museum"

    fun load(context: Context): RoomLayout? = runCatching {
        context.assets.open(PATH).bufferedReader().use { parse(it.readText()) }
    }.onFailure { Log.e(TAG, "archive-layout failed path=$PATH reason=${it.javaClass.simpleName}", it) }.getOrNull()

    fun parse(text: String): RoomLayout {
        val root = JSONObject(text)
        require(root.getInt("schemaVersion") == 1) { "schemaVersion" }
        val glbs = root.getJSONObject("room").getJSONArray("glb")
        val models = (0 until glbs.length()).map { glbs.getJSONObject(it).getString("path") }
        val anchors = root.getJSONArray("anchors").objects().map { a ->
            Anchor(
                id = a.getString("id"),
                position = a.getJSONArray("position").point(),
                yawDegrees = a.getDouble("yawDeg").toFloat(),
                bay = a.getString("bay"),
                displayScale = a.optDouble("displayScale", 1.0).toFloat(),
                plaque = a.optJSONObject("plaque")?.placard(),
            )
        }
        val signs = root.optJSONArray("bays")?.objects().orEmpty().mapNotNull { b ->
            b.optJSONObject("sign")?.placard()?.let { b.getString("id") to it }
        }.toMap()
        val viewpoints = root.getJSONArray("viewpoints").objects().associate { v ->
            v.getString("name") to StandPoint(v.getJSONArray("position").point(), v.getDouble("yawDeg").toFloat())
        }
        // Optional: a layout without the entrance marker still opens, with no exit on the door.
        val exit = root.getJSONObject("room").optJSONObject("entrance")?.optJSONArray("marker")?.takeIf { it.length() == 3 }?.point()
        require(models.isNotEmpty()) { "no room model" }
        require(viewpoints.containsKey(RoomLayout.ARRIVAL)) { "no arrival viewpoint" }
        return RoomLayout(models, anchors, viewpoints, signs, exitMarker = exit)
    }

    private fun JSONArray.objects(): List<JSONObject> = (0 until length()).map { getJSONObject(it) }

    private fun JSONArray.point(): Point3 = Point3(getDouble(0).toFloat(), getDouble(1).toFloat(), getDouble(2).toFloat())

    private fun JSONObject.placard(): Placard {
        val size = optJSONArray("sizeMetres")?.takeIf { it.length() == 2 }
        return Placard(
            position = getJSONArray("position").point(),
            yawDegrees = getDouble("yawDeg").toFloat(),
            pitchDegrees = optDouble("pitchDeg", 0.0).toFloat(),
            widthMetres = size?.getDouble(0)?.toFloat(),
            heightMetres = size?.getDouble(1)?.toFloat(),
        )
    }
}
