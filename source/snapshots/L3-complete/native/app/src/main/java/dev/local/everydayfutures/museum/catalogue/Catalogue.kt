package dev.local.everydayfutures.museum.catalogue

/**
 * One interpretive chapter of an object's later tabletop story (D2). Carried in the data model now
 * so the first slice and the story share one catalogue; the first slice does not render chapters.
 */
data class Chapter(val title: String, val text: String)

/** One catalogue entry. Copy is newly authored interpretive draft text and still needs editorial review. */
data class Artifact(
    val id: String,
    val number: String,
    val shortName: String,
    val selectLabel: String,
    val title: String,
    val subtitle: String,
    val category: String,
    val era: String,
    val description: String,
    val question: String,
    val chapters: List<Chapter>,
)

/** The permanent collection of the first slice: three ordinary objects. Order is presentation order. */
object Catalogue {
    const val QUENCHER = "quencher"
    const val VACUUM = "vacuum"
    const val SMART_SPEAKER = "smartspeaker"

    val artifacts: List<Artifact> = listOf(
        Artifact(
            id = QUENCHER,
            number = "001",
            shortName = "The vessel",
            selectLabel = "Quencher cup",
            title = "Holding more.",
            subtitle = "A cup that carried more than water.",
            category = "Ritual",
            era = "The 2020s",
            description = "A familiar companion for the commute, the desk and the long way home. " +
                "An everyday vessel became a small declaration of how we wanted to live.",
            question = "When did staying hydrated become a way of belonging?",
            chapters = listOf(
                Chapter("Ritual", "A refill marks a pause. Small repeated gestures give a day its shape."),
                Chapter("Signal", "An ordinary object becomes a recognizable badge. What we carry can say who we hope to be."),
                Chapter("Afterlife", "The ritual ends; the object remains. What would you want the next visitor to understand?"),
            ),
        ),
        Artifact(
            id = VACUUM,
            number = "002",
            shortName = "The helper",
            selectLabel = "Robot vacuum",
            title = "A quieter chore.",
            subtitle = "A machine learning the shape of a home.",
            category = "Convenience",
            era = "The 2020s",
            description = "It traced the same chair legs and thresholds every day. Behind a promise of " +
                "effortless cleaning was a map of the routines that made a room a home.",
            question = "What could a floor plan tell someone about your life?",
            chapters = listOf(
                Chapter("Routine", "The morning route begins beneath a chair. A crumb becomes a record of breakfast."),
                Chapter("Boundary", "Furniture makes islands and doorways make borders. The machine travels through a geography we barely notice."),
                Chapter("Human trace", "The route becomes a portrait. The most ordinary movements leave a pattern worth preserving."),
            ),
        ),
        Artifact(
            id = SMART_SPEAKER,
            number = "003",
            shortName = "The listener",
            selectLabel = "Smart speaker",
            title = "A room, listening.",
            subtitle = "A small object with an enormous outside world.",
            category = "Connection",
            era = "The 2020s",
            description = "Music, a timer, a question spoken into the room. The simplest requests connected " +
                "domestic space to an infrastructure far beyond its walls.",
            question = "What changes when speaking becomes an interface?",
            chapters = listOf(
                Chapter("Invitation", "A voice crosses a room. The object waits at the edge of attention."),
                Chapter("Connection", "One small request opens into a network of distant systems. Convenience makes that distance feel invisible."),
                Chapter("Silence", "When the connection falls quiet, the room returns. Which parts of the ritual did the object really own?"),
            ),
        ),
    )

    fun byId(id: String): Artifact? = artifacts.firstOrNull { it.id == id }

    fun contains(id: String): Boolean = artifacts.any { it.id == id }
}
