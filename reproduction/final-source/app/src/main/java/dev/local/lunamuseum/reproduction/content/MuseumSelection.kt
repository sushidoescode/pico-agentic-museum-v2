package dev.local.lunamuseum.reproduction.content

enum class MuseumObject(
    val title: String,
    val description: String,
    val isPrimitivePlaceholder: Boolean = false,
) {
    HANDLED_CUP("Handled cup", "A simple ceramic cup with a sturdy handle."),
    ROBOT_VACUUM("Robot vacuum", "A compact robot vacuum that quietly patrols the floor."),
    SMART_SPEAKER("Smart speaker", "A small connected speaker ready to answer a question."),
    COFFEE_POD("Coffee pod", "Primitive placeholder: a simple capsule-shaped exhibit.", true),
    DELIVERY_BOX("Delivery box", "Primitive placeholder: a simple box-shaped exhibit.", true),
    QR_CODE("QR code", "Primitive placeholder: a simple marked-box exhibit.", true),
    FIDGET_TOY("Fidget toy", "Primitive placeholder: a simple rounded exhibit.", true),
    FACE_MASK("Face mask", "Primitive placeholder: a simple thin-box exhibit.", true),
    SMARTPHONE("Smartphone", "Primitive placeholder: a simple thin-box exhibit.", true),
    EARBUDS("Earbuds", "Primitive placeholder: a pair-like simple exhibit.", true),
    SMARTWATCH("Smartwatch", "Primitive placeholder: a simple rounded exhibit.", true),
    CRYPTO_WALLET("Crypto wallet", "Primitive placeholder: a simple wallet-shaped exhibit.", true),
    REMOTE_CONTROL("Remote control", "Primitive placeholder: a simple long-box exhibit.", true),
    ROUTER("Router", "Primitive placeholder: a simple box-shaped exhibit.", true),
    DOORBELL("Doorbell", "Primitive placeholder: a simple wall-box exhibit.", true),
    SCOOTER("Scooter", "Primitive placeholder: a simple bar-and-box exhibit.", true),
    GAS_CAN("Gas can", "Primitive placeholder: a simple can-shaped exhibit.", true),
    ROCKET("Rocket", "Primitive placeholder: a simple cone-and-box exhibit.", true),
}

data class MuseumSelection(
    val selected: MuseumObject = MuseumObject.HANDLED_CUP,
    val turnQuarterTurns: Int = 0,
) {
    fun select(item: MuseumObject): MuseumSelection = copy(selected = item, turnQuarterTurns = 0)
    fun turn(): MuseumSelection = copy(turnQuarterTurns = (turnQuarterTurns + 1) % 4)
    fun reset(): MuseumSelection = copy(turnQuarterTurns = 0)
}
