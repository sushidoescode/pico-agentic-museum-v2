package dev.local.everydayfutures.museum.catalogue

import dev.local.everydayfutures.museum.content.HorizontalAnchor
import dev.local.everydayfutures.museum.content.ModelBounds

/**
 * One interpretive chapter of an object's tabletop story (L3). Only the three original objects have chapters; the
 * story actions are shown only for them ([Artifact.hasStory]).
 */
data class Chapter(val title: String, val text: String)

/** The three curatorial bays of the Earth Collection. [label] is the visitor-facing name. */
enum class Section(val label: String) {
    DAILY_RITUALS("Daily rituals"),
    CONNECTED_LIVES("Connected lives"),
    DOMESTIC_SYSTEMS("Domestic systems and movement"),
    ;

    /** Short stable id for markers and filters: `daily`, `connected`, `domestic`. */
    val key: String get() = name.substringBefore('_').lowercase()
}

/** A real-world size in metres: height (y), width (x) and depth (z) of the object as it is displayed. */
data class Size3(val height: Float, val width: Float, val depth: Float)

/**
 * Where an exhibit's model comes from and how it is placed. [uri] is an `asset://` path inside the app; [realSize] is
 * the object's real size in metres (shown to the visitor; the study and the inspector fit the model into their own
 * boxes, so the displayed scale differs by object). [anchor] says which x/z point stands on the box centre;
 * [authoredBounds] (in the model's own frame) is used only when the runtime reports an empty box for a loaded model.
 * [proceduralFallback] is true only for the three original objects, which keep their procedural stand-ins
 * (ExhibitFactory) when their model cannot be loaded; every other object shows an explicit error instead.
 */
data class ModelSpec(
    val uri: String,
    val realSize: Size3,
    val anchor: HorizontalAnchor = HorizontalAnchor.BOUNDS_CENTRE,
    val authoredBounds: ModelBounds? = null,
    val yawOffsetDegrees: Float = 0f,
    val proceduralFallback: Boolean = false,
) {
    /** The path inside the APK's assets (`models/exhibits/<id>.glb`), or null when [uri] is not an asset URI. */
    val assetPath: String?
        get() = ASSET_SCHEMES.firstOrNull { uri.startsWith(it) }?.let { uri.removePrefix(it) }?.takeIf { it.isNotBlank() }

    private companion object {
        val ASSET_SCHEMES = listOf("asset://", "assets://")
    }
}

/**
 * One catalogue entry. [identification] is factual; [interpretation] is the fictional curators' note and is always
 * labelled as such in the UI. Copy is newly authored draft text and still needs editorial review.
 */
data class Artifact(
    val id: String,
    val number: String,
    val section: Section,
    val name: String,
    val shortName: String,
    val title: String,
    val subtitle: String,
    val category: String,
    val era: String,
    val identification: String,
    val interpretation: String,
    val question: String,
    val model: ModelSpec,
    val chapters: List<Chapter> = emptyList(),
    /** The label the first slice used for the three original objects; the name for every later one. */
    val selectLabel: String = name,
    /** A 256 px PNG in the app's assets, rendered from the runtime model; a missing file shows the number and name. */
    val thumbnail: String = "thumbnails/$id.png",
) {
    /** Only objects with chapters offer "Reveal its story" (no dead-end story buttons). */
    val hasStory: Boolean get() = chapters.isNotEmpty()
}

/**
 * The Earth Collection: eighteen ordinary objects in accession order (001–018), six per section. The one declarative
 * registry the catalogue, the study, the inspector, the companion, the stories and the archive all read. Pure Kotlin
 * (host-tested). Ids gate persistence: `quencher`, `vacuum` and `smartspeaker` keep their accepted copy and stories.
 */
object Catalogue {
    const val QUENCHER = "quencher"
    const val VACUUM = "vacuum"
    const val SMART_SPEAKER = "smartspeaker"

    /** Where every exhibit model other than the vessel is packaged: `asset://models/exhibits/<id>.glb`. */
    fun exhibitModel(id: String, realSize: Size3): ModelSpec =
        ModelSpec(uri = "asset://models/exhibits/$id.glb", realSize = realSize)

    /** The accepted polish-001 vessel (Tripo a3ebdbe0), kept as the tumbler's study and inspector model. */
    const val VESSEL_URI = "asset://models/vessel_study.glb"

    /**
     * The vessel asset's bounds as authored, in its own frame: the GLB's POSITION min/max (±0.1787, ±0.4990, ±0.2627)
     * under its node transform (-90° about +Y, then +0.073 on x), as derived in evidence/polish-001/ASSET-RECORD.md and
     * matching the headless Blender inspection. Used only if the runtime reports an empty box for the loaded model.
     */
    val VESSEL_AUTHORED_BOUNDS = ModelBounds(-0.190f, -0.499f, -0.179f, 0.336f, 0.499f, 0.179f)

    val artifacts: List<Artifact> = listOf(
        Artifact(
            id = QUENCHER,
            number = "001",
            section = Section.DAILY_RITUALS,
            name = "Handled tumbler",
            shortName = "The vessel",
            selectLabel = "Quencher cup",
            title = "Holding more.",
            subtitle = "A cup that carried more than water.",
            category = "Ritual",
            era = "The 2020s",
            identification = "A large vacuum-insulated stainless-steel tumbler with a handle, a lid and a reusable straw. " +
                "Insulated drinkware like this became a widespread everyday accessory, and something people " +
                "collected, in the early 2020s.",
            interpretation = "A familiar companion for the commute, the desk and the long way home. " +
                "An everyday vessel became a small declaration of how we wanted to live.",
            question = "When did staying hydrated become a way of belonging?",
            // Authored with the body's axis at the origin and the handle toward +x (scripts/postprocess-vessel-glb.py),
            // so the origin, not the bounds' centre, goes on the box centre and the yaw turns the body about its axis.
            model = ModelSpec(
                uri = VESSEL_URI,
                realSize = Size3(height = 0.3f, width = 0.14f, depth = 0.1f),
                anchor = HorizontalAnchor.MODEL_ORIGIN,
                authoredBounds = VESSEL_AUTHORED_BOUNDS,
                proceduralFallback = true,
            ),
            chapters = listOf(
                Chapter("Ritual", "A refill marks a pause. Small repeated gestures give a day its shape."),
                Chapter("Signal", "An ordinary object becomes a recognizable badge. What we carry can say who we hope to be."),
                Chapter("Afterlife", "The ritual ends; the object remains. What would you want the next visitor to understand?"),
            ),
        ),
        Artifact(
            id = VACUUM,
            number = "002",
            section = Section.DOMESTIC_SYSTEMS,
            name = "Robot vacuum",
            shortName = "The helper",
            selectLabel = "Robot vacuum",
            title = "A quieter chore.",
            subtitle = "A machine learning the shape of a home.",
            category = "Convenience",
            era = "The 2020s",
            identification = "A battery-powered vacuum cleaner that steers itself around a floor using bump and cliff sensors " +
                "and, in many models, laser or camera mapping, then returns to a dock to recharge. Robot vacuums " +
                "became common household appliances during the 2010s.",
            interpretation = "It traced the same chair legs and thresholds every day. Behind a promise of " +
                "effortless cleaning was a map of the routines that made a room a home.",
            question = "What could a floor plan tell someone about your life?",
            model = exhibitModel(VACUUM, Size3(height = 0.092f, width = 0.347f, depth = 0.344f)).copy(proceduralFallback = true),
            chapters = listOf(
                Chapter("Routine", "The morning route begins beneath a chair. A crumb becomes a record of breakfast."),
                Chapter("Boundary", "Furniture makes islands and doorways make borders. The machine travels through a geography we barely notice."),
                Chapter("Human trace", "The route becomes a portrait. The most ordinary movements leave a pattern worth preserving."),
            ),
        ),
        Artifact(
            id = SMART_SPEAKER,
            number = "003",
            section = Section.CONNECTED_LIVES,
            name = "Smart speaker",
            shortName = "The listener",
            selectLabel = "Smart speaker",
            title = "A room, listening.",
            subtitle = "A small object with an enormous outside world.",
            category = "Connection",
            era = "The 2020s",
            identification = "A voice-controlled speaker whose microphones listen for a wake word and pass requests to an " +
                "online assistant for music, timers, questions and home control. Smart speakers entered millions " +
                "of homes from the mid-2010s.",
            interpretation = "Music, a timer, a question spoken into the room. The simplest requests connected " +
                "domestic space to an infrastructure far beyond its walls.",
            question = "What changes when speaking becomes an interface?",
            model = exhibitModel(SMART_SPEAKER, Size3(height = 0.169f, width = 0.14f, depth = 0.14f)).copy(proceduralFallback = true),
            chapters = listOf(
                Chapter("Invitation", "A voice crosses a room. The object waits at the edge of attention."),
                Chapter("Connection", "One small request opens into a network of distant systems. Convenience makes that distance feel invisible."),
                Chapter("Silence", "When the connection falls quiet, the room returns. Which parts of the ritual did the object really own?"),
            ),
        ),
        Artifact(
            id = "coffeepod",
            number = "004",
            section = Section.DAILY_RITUALS,
            name = "Coffee pod",
            shortName = "The measure",
            title = "One cup at a time.",
            subtitle = "A sealed dose of a morning ritual.",
            category = "Ritual",
            era = "2000s–2020s",
            identification = "A single-serve capsule of ground coffee sealed with foil, brewed by a machine that pierces it. " +
                "Pod brewers spread through homes and offices in the 2000s and 2010s; most capsules were used " +
                "once and thrown away.",
            interpretation = "Our archivists were struck by the precision: each morning measured into an identical sealed " +
                "vessel and opened once. Convenience here was a portion, not a place.",
            question = "What does a ritual lose, or gain, when it arrives pre-measured?",
            model = exhibitModel("coffeepod", Size3(height = 0.03f, width = 0.04f, depth = 0.04f)),
        ),
        Artifact(
            id = "deliverybox",
            number = "005",
            section = Section.DAILY_RITUALS,
            name = "Delivery box",
            shortName = "The parcel",
            title = "Arrived at the door.",
            subtitle = "Corrugated card that carried the marketplace home.",
            category = "Convenience",
            era = "2010s–2020s",
            identification = "A corrugated cardboard shipping carton sealed with tape. Online shopping made home parcel " +
                "deliveries routine in the 2010s and 2020s; billions of boxes like this were shipped every year, " +
                "and most were recycled or discarded within days.",
            interpretation = "The box was the only part of the journey the buyer ever held. We keep it empty, as most of them " +
                "were within minutes of arriving.",
            question = "How far did an ordinary purchase travel to reach a doorstep?",
            model = exhibitModel("deliverybox", Size3(height = 0.307f, width = 0.546f, depth = 0.421f)),
        ),
        Artifact(
            id = "qrcode",
            number = "006",
            section = Section.DAILY_RITUALS,
            name = "QR menu stand",
            shortName = "The pattern",
            title = "Scan to read.",
            subtitle = "A table sign that turned the menu into a link.",
            category = "Connection",
            era = "Early 2020s",
            identification = "A small tabletop stand printed with a QR code, a two-dimensional barcode first developed in " +
                "1994. Restaurants adopted code-linked menus widely in 2020 and 2021 to avoid shared paper menus " +
                "during the COVID-19 pandemic. The code on this exhibit is decorative and links nowhere.",
            interpretation = "The stand holds a pattern that means nothing until a device reads it. We find it a fitting " +
                "emblem: a meal introduced through a screen in the diner's own hand.",
            question = "When did reading a menu begin to require a phone?",
            model = exhibitModel("qrcode", Size3(height = 0.149f, width = 0.1f, depth = 0.06f)),
        ),
        Artifact(
            id = "fidget",
            number = "007",
            section = Section.DAILY_RITUALS,
            name = "Fidget spinner",
            shortName = "The spinner",
            title = "Something to turn.",
            subtitle = "A pocket toy for restless hands.",
            category = "Ritual",
            era = "2017",
            identification = "A small three-lobed toy that spins on a central ball bearing, held between finger and thumb. It " +
                "became a worldwide craze in 2017 and was sold as a way to occupy restless hands.",
            interpretation = "It performs no task and produces nothing. Our curators consider it one of the most honest " +
                "objects in the collection: made only to be turned, and set down again.",
            question = "Why do idle hands look for something to hold?",
            model = exhibitModel("fidget", Size3(height = 0.013f, width = 0.075f, depth = 0.07f)),
        ),
        Artifact(
            id = "facemask",
            number = "008",
            section = Section.DAILY_RITUALS,
            name = "Disposable face mask",
            shortName = "The precaution",
            title = "A shared precaution.",
            subtitle = "Three pleated layers worn by millions at once.",
            category = "Care",
            era = "2020–2022",
            identification = "A pleated three-layer disposable mask with elastic ear loops. During the COVID-19 pandemic, from " +
                "2020, masks like this were worn daily by very large numbers of people and were required in many " +
                "public places.",
            interpretation = "Light enough to forget, worn for the sake of others as much as oneself. Few objects in the " +
                "archive record so many people agreeing, for a time, to the same small inconvenience.",
            question = "What does it take for a whole society to change a daily habit?",
            model = exhibitModel("facemask", Size3(height = 0.095f, width = 0.197f, depth = 0.086f)),
        ),
        Artifact(
            id = "smartphone",
            number = "009",
            section = Section.CONNECTED_LIVES,
            name = "Smartphone",
            shortName = "The window",
            title = "Everything, in one hand.",
            subtitle = "A glass slab that became camera, map, wallet and voice.",
            category = "Connection",
            era = "2007–2020s",
            identification = "A touchscreen mobile phone that combines calls, camera, maps, messaging, payments and " +
                "installable apps. Touchscreen smartphones spread rapidly after 2007; by the 2020s billions of " +
                "people carried one.",
            interpretation = "Our records show its owners glancing at it many times an hour. The screen is dark now; we " +
                "present it as the window it was, rather than any view it showed.",
            question = "Which of its many jobs would you give back first?",
            model = exhibitModel("smartphone", Size3(height = 0.15f, width = 0.072f, depth = 0.008f)),
        ),
        Artifact(
            id = "earbuds",
            number = "010",
            section = Section.CONNECTED_LIVES,
            name = "Wireless earbuds and case",
            shortName = "The private ear",
            title = "A private soundtrack.",
            subtitle = "Two small speakers and the case that charged them.",
            category = "Connection",
            era = "Late 2010s–2020s",
            identification = "A pair of cordless in-ear headphones that connect by Bluetooth, kept in a pocket case whose " +
                "battery recharges them. Fully wireless earbuds became common from the late 2010s.",
            interpretation = "Worn in public, they let a person stand in a crowd and hear something else entirely. The case, " +
                "never far away, shows how carefully that small privacy was kept charged.",
            question = "When is listening a way of being alone together?",
            model = exhibitModel("earbuds", Size3(height = 0.052f, width = 0.06f, depth = 0.055f)),
        ),
        Artifact(
            id = "smartwatch",
            number = "011",
            section = Section.CONNECTED_LIVES,
            name = "Smartwatch",
            shortName = "The counter",
            title = "Counting the body.",
            subtitle = "A wrist screen for time, messages and steps.",
            category = "Connection",
            era = "2010s–2020s",
            identification = "A wrist-worn computer with a touchscreen that shows the time and notifications and records " +
                "movement and heart rate with built-in sensors. Smartwatches and fitness trackers became " +
                "widespread in the 2010s.",
            interpretation = "Earlier watches measured the day; this one also measured its wearer. Our curators note how " +
                "readily people let a small device count their steps and their sleep.",
            question = "What would you want a device to know about you, and what not?",
            model = exhibitModel("smartwatch", Size3(height = 0.076f, width = 0.045f, depth = 0.077f)),
        ),
        Artifact(
            id = "cryptowallet",
            number = "012",
            section = Section.CONNECTED_LIVES,
            name = "Hardware crypto wallet",
            shortName = "The key",
            title = "A key in the pocket.",
            subtitle = "A tiny device guarding a secret number.",
            category = "Value",
            era = "Mid-2010s–2020s",
            identification = "A small USB device that keeps the private keys for cryptocurrency accounts offline and signs " +
                "transactions when connected to a computer. Hardware wallets appeared in the mid-2010s as digital " +
                "currencies such as Bitcoin, launched in 2009, grew in value.",
            interpretation = "Its whole purpose is to keep one number secret. We find it remarkable that an object this small " +
                "could stand between an owner and a fortune that existed only as records on distant machines.",
            question = "What makes something valuable if you can never touch it?",
            model = exhibitModel("cryptowallet", Size3(height = 0.01f, width = 0.059f, depth = 0.018f)),
        ),
        Artifact(
            id = "remote",
            number = "013",
            section = Section.CONNECTED_LIVES,
            name = "Streaming remote",
            shortName = "The chooser",
            title = "Choosing what's next.",
            subtitle = "A handful of buttons for an endless library.",
            category = "Leisure",
            era = "2010s–2020s",
            identification = "A compact remote for a television streaming device, with a direction pad, a few playback buttons " +
                "and often a microphone for voice search. On-demand streaming became one of the main ways to " +
                "watch television in the 2010s.",
            interpretation = "Earlier remotes moved between a few channels; this one browsed a catalogue too large to finish. " +
                "Our archivists noted how much of an evening could be spent choosing.",
            question = "When a library is endless, how do you decide?",
            model = exhibitModel("remote", Size3(height = 0.018f, width = 0.038f, depth = 0.152f)),
        ),
        Artifact(
            id = "router",
            number = "014",
            section = Section.DOMESTIC_SYSTEMS,
            name = "Wi-Fi router",
            shortName = "The quiet box",
            title = "The invisible room.",
            subtitle = "A small box that filled a home with network.",
            category = "Infrastructure",
            era = "2000s–2020s",
            identification = "A home networking device that connects a household to the internet and shares the connection by " +
                "radio using the Wi-Fi standards, first published in 1997. By the 2010s most connected homes " +
                "relied on one.",
            interpretation = "Nothing in a room seems to change when it is switched on, yet every other connected object " +
                "depends on it. Our curators display it as the home's quiet infrastructure.",
            question = "Which parts of your home would stop working without it?",
            model = exhibitModel("router", Size3(height = 0.172f, width = 0.232f, depth = 0.155f)),
        ),
        Artifact(
            id = "doorbell",
            number = "015",
            section = Section.DOMESTIC_SYSTEMS,
            name = "Doorbell camera",
            shortName = "The watcher",
            title = "Who's at the door?",
            subtitle = "A doorbell that watched as well as rang.",
            category = "Security",
            era = "Mid-2010s–2020s",
            identification = "A doorbell with a built-in camera, microphone and speaker that sends video to a phone when " +
                "someone approaches or presses the button. Video doorbells became popular from the mid-2010s.",
            interpretation = "The threshold of a home gained a lens. Our archive holds the object, not its recordings; it once " +
                "kept a record of every visitor, courier and passer-by.",
            question = "Who else saw the people who came to your door?",
            model = exhibitModel("doorbell", Size3(height = 0.13f, width = 0.046f, depth = 0.027f)),
        ),
        Artifact(
            id = "scooter",
            number = "016",
            section = Section.DOMESTIC_SYSTEMS,
            name = "E-scooter",
            shortName = "The last mile",
            title = "The last mile.",
            subtitle = "A folding electric ride for short trips.",
            category = "Movement",
            era = "Late 2010s–2020s",
            identification = "A standing electric kick scooter with a battery in its deck and a motor in one wheel. Shared " +
                "rental scooters unlocked by phone appeared in many cities from 2017, alongside privately owned " +
                "folding models.",
            interpretation = "It filled the gap between the station and the door. We display it at full size, on the floor, " +
                "because it was ridden rather than held.",
            question = "What distance feels too short to drive and too long to walk?",
            model = exhibitModel("scooter", Size3(height = 1.15f, width = 0.534f, depth = 0.95f)),
        ),
        Artifact(
            id = "gascan",
            number = "017",
            section = Section.DOMESTIC_SYSTEMS,
            name = "Plastic gas can",
            shortName = "The reserve",
            title = "A portable reserve.",
            subtitle = "Fuel kept in the shed, just in case.",
            category = "Movement",
            era = "20th–21st century",
            identification = "A moulded plastic container with a handle and pouring spout for carrying and storing petrol " +
                "(gasoline); red was a common colour for fuel cans. Households kept one for mowers, generators " +
                "and stranded cars.",
            interpretation = "Much of the period's movement depended on a liquid that had to be carried like this. Our " +
                "curators place it among the domestic systems because so many homes kept one in reserve.",
            question = "What did a household keep in reserve, and why?",
            model = exhibitModel("gascan", Size3(height = 0.33f, width = 0.268f, depth = 0.131f)),
        ),
        Artifact(
            id = "rocket",
            number = "018",
            section = Section.DOMESTIC_SYSTEMS,
            name = "Saturn V desk replica",
            shortName = "The old ambition",
            title = "An old ambition on the desk.",
            subtitle = "A scale model of a rocket from the previous century.",
            category = "Aspiration",
            era = "Replica of a 1967–1973 rocket",
            identification = "A desk-sized scale model of the Saturn V, the rocket that launched the Apollo missions, " +
                "including the first crewed Moon landing in 1969; it last flew in 1973. Replicas like this were " +
                "made and collected long after the real rockets retired.",
            interpretation = "This is not a 21st-century machine. We collected it because people of the period kept it close: " +
                "a reminder of a journey an earlier generation had already made.",
            question = "Why keep a model of a journey that has already happened?",
            model = exhibitModel("rocket", Size3(height = 0.545f, width = 0.12f, depth = 0.121f)),
        ),
    )

    fun byId(id: String): Artifact? = artifacts.firstOrNull { it.id == id }

    fun contains(id: String): Boolean = artifacts.any { it.id == id }

    /** The section's six objects in accession order. */
    fun bySection(section: Section): List<Artifact> = artifacts.filter { it.section == section }
}
