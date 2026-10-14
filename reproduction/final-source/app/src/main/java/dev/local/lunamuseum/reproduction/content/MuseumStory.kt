package dev.local.lunamuseum.reproduction.content

import androidx.compose.runtime.mutableStateOf

class MuseumStory(val objectType: MuseumObject, val steps: List<String>) {
    val title: String get() = objectType.title
}

object MuseumStories {
    fun forObject(objectType: MuseumObject): MuseumStory = MuseumStory(
        objectType,
        when (objectType) {
            MuseumObject.HANDLED_CUP -> listOf(
                "The cup waits on the table.",
                "A warm drink is poured in.",
                "The handle makes it easy to lift.",
            )
            MuseumObject.ROBOT_VACUUM -> listOf(
                "The robot vacuum starts at home.",
                "It rolls out to patrol the floor.",
                "It returns when the room is clean.",
            )
            MuseumObject.SMART_SPEAKER -> listOf(
                "The speaker is listening quietly.",
                "A question wakes its glowing light.",
                "It answers and settles back down.",
            )
            else -> listOf(
                "${objectType.title} is displayed as a primitive placeholder.",
                "The simple shape marks where the museum exhibit belongs.",
                "A finished asset can replace this placeholder later.",
            )
        },
    )
}

data class MuseumStoryState(
    val selected: MuseumObject = MuseumObject.HANDLED_CUP,
    val stepIndex: Int = 0,
    val isPlaying: Boolean = false,
    val reducedMotion: Boolean = false,
) {
    val story: MuseumStory get() = MuseumStories.forObject(selected)
    val stepText: String get() = story.steps[stepIndex]
    fun select(objectType: MuseumObject) = copy(selected = objectType, stepIndex = 0, isPlaying = false)
    fun play() = if (stepIndex == story.steps.lastIndex) copy(stepIndex = 0, isPlaying = true) else copy(isPlaying = true)
    fun pause() = copy(isPlaying = false)
    fun restart() = copy(stepIndex = 0, isPlaying = true)
    fun reset(objectType: MuseumObject = selected) = copy(selected = objectType, stepIndex = 0, isPlaying = false)
    fun advance() = if (stepIndex < story.steps.lastIndex) copy(stepIndex = stepIndex + 1) else copy(isPlaying = false)
    fun toggleReducedMotion() = copy(reducedMotion = !reducedMotion)
}

object MuseumStoryStore {
    private val state = mutableStateOf(MuseumStoryState())
    val current: MuseumStoryState @Synchronized get() = state.value
    @Synchronized fun update(next: MuseumStoryState) { state.value = next }
    @Synchronized fun select(objectType: MuseumObject) { state.value = state.value.select(objectType) }
    @Synchronized fun reset(objectType: MuseumObject = state.value.selected) { state.value = state.value.reset(objectType) }
}

data class MuseumStoryMotionValues(
    val positionX: Float = 0f,
    val positionY: Float = 0f,
    val positionZ: Float = 0f,
    val pitch: Float = 0f,
    val yaw: Float = 0f,
    val roll: Float = 0f,
)

fun museumStoryMotion(objectType: MuseumObject, stepIndex: Int, reducedMotion: Boolean): MuseumStoryMotionValues {
    if (reducedMotion) return MuseumStoryMotionValues()
    return when (objectType) {
        MuseumObject.HANDLED_CUP -> when (stepIndex) {
            1 -> MuseumStoryMotionValues(positionY = 0.12f, pitch = -8f)
            2 -> MuseumStoryMotionValues(roll = 12f)
            else -> MuseumStoryMotionValues()
        }
        MuseumObject.ROBOT_VACUUM -> when (stepIndex) {
            1 -> MuseumStoryMotionValues(positionX = 0.16f)
            2 -> MuseumStoryMotionValues(positionX = -0.16f, yaw = 18f)
            else -> MuseumStoryMotionValues()
        }
        MuseumObject.SMART_SPEAKER -> when (stepIndex) {
            1 -> MuseumStoryMotionValues(positionY = 0.08f, pitch = -6f)
            2 -> MuseumStoryMotionValues(positionY = 0.03f, yaw = 10f)
            else -> MuseumStoryMotionValues()
        }
        else -> MuseumStoryMotionValues()
    }
}
