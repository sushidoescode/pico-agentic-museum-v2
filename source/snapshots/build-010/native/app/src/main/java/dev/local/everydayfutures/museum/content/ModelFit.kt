package dev.local.everydayfutures.museum.content

/** Axis-aligned bounds of a loaded model in the frame it is placed in (metres, or the model's own units). */
data class ModelBounds(
    val minX: Float,
    val minY: Float,
    val minZ: Float,
    val maxX: Float,
    val maxY: Float,
    val maxZ: Float,
)

/** Where a fitted model must stand: its [height], the y of its base and the x/z point it stands on (metres). */
data class FitTarget(val height: Float, val baseY: Float, val centreX: Float = 0f, val centreZ: Float = 0f)

/** Which x/z point of the model is put on the target's centre. */
enum class HorizontalAnchor {
    /** The centre of the model's bounds, for a model whose origin carries no meaning. */
    BOUNDS_CENTRE,

    /** The model's own origin, for a model authored with its turning axis there (an off-centre handle stays off-centre). */
    MODEL_ORIGIN,
}

/** A uniform [scale] followed by a translation ([x], [y], [z]): a model point p lands at scale * p + (x, y, z). */
data class ModelPlacement(val scale: Float, val x: Float, val y: Float, val z: Float)

/**
 * The space a fitted model may occupy (metres): at most [width] (x), [height] (y) and [depth] (z), standing with its
 * base on [baseY] and its anchor on ([centreX], [centreZ]).
 */
data class FitBox(
    val width: Float,
    val height: Float,
    val depth: Float,
    val baseY: Float,
    val centreX: Float = 0f,
    val centreZ: Float = 0f,
) {
    /** The whole box as bounds (a stand-in collider when a model's placed bounds are unknown). */
    val bounds: ModelBounds
        get() = ModelBounds(
            minX = centreX - width / 2f, minY = baseY, minZ = centreZ - depth / 2f,
            maxX = centreX + width / 2f, maxY = baseY + height, maxZ = centreZ + depth / 2f,
        )
}

/** A placement found by [ModelFit.fitWithFallback]: whether the authored bounds had to stand in for measured ones. */
data class FitResult(val placement: ModelPlacement, val bounds: ModelBounds, val authoredBounds: Boolean) {
    /** Where the model sits after [placement], in the frame the placement is applied in (the collider uses this). */
    val placed: ModelBounds get() = ModelFit.placed(bounds, placement)
}

/**
 * Fit arithmetic for a loaded model, kept free of the SDK so it is host-testable. The placement makes the fitted model's
 * base sit at the target's base y, its height equal the target height, and its anchor point sit on the target's x/z
 * centre; the scale is uniform, so proportions are kept.
 */
object ModelFit {
    /** Heights at or below this are degenerate (a flat or empty model; the SDK reports an empty box as all zeros). */
    const val MIN_HEIGHT = 1e-6f

    /** The placement for [bounds], or null for non-finite, inverted or flat bounds, or a target that cannot be met. */
    fun fit(bounds: ModelBounds, target: FitTarget, anchor: HorizontalAnchor = HorizontalAnchor.BOUNDS_CENTRE): ModelPlacement? {
        val inputs = listOf(
            bounds.minX, bounds.minY, bounds.minZ, bounds.maxX, bounds.maxY, bounds.maxZ,
            target.height, target.baseY, target.centreX, target.centreZ,
        )
        if (inputs.any { !it.isFinite() }) return null
        if (bounds.maxX < bounds.minX || bounds.maxZ < bounds.minZ) return null
        val height = bounds.maxY - bounds.minY
        if (!height.isFinite() || height <= MIN_HEIGHT || target.height <= 0f) return null
        val scale = target.height / height
        val (anchorX, anchorZ) = when (anchor) {
            HorizontalAnchor.BOUNDS_CENTRE -> (bounds.minX + bounds.maxX) / 2f to (bounds.minZ + bounds.maxZ) / 2f
            HorizontalAnchor.MODEL_ORIGIN -> 0f to 0f
        }
        val placement = ModelPlacement(
            scale = scale,
            x = target.centreX - scale * anchorX,
            y = target.baseY - scale * bounds.minY,
            z = target.centreZ - scale * anchorZ,
        )
        return placement.takeIf { p -> p.scale > 0f && listOf(p.scale, p.x, p.y, p.z).all { it.isFinite() } }
    }

    /**
     * The placement that makes [bounds] fit inside [box]: one uniform scale, the smallest of the three the axes allow
     * (height against [FitBox.height]; each horizontal half-extent measured from the [anchor] against half the box
     * width or depth, so an off-centre handle stays inside), the base on [FitBox.baseY] and the anchor on the box
     * centre. An axis with no extent (a flat card, a thin plate) sets no limit. Null for non-finite, overflowing or
     * inverted bounds, bounds with no extent at all (the SDK's empty box), or a box with a non-positive or non-finite size.
     */
    fun fitWithin(bounds: ModelBounds, box: FitBox, anchor: HorizontalAnchor = HorizontalAnchor.BOUNDS_CENTRE): ModelPlacement? {
        val inputs = listOf(
            bounds.minX, bounds.minY, bounds.minZ, bounds.maxX, bounds.maxY, bounds.maxZ,
            box.width, box.height, box.depth, box.baseY, box.centreX, box.centreZ,
        )
        if (inputs.any { !it.isFinite() }) return null
        if (bounds.maxX < bounds.minX || bounds.maxY < bounds.minY || bounds.maxZ < bounds.minZ) return null
        if (box.width <= 0f || box.height <= 0f || box.depth <= 0f) return null
        // Extents that overflow a float are not a model anyone can see; refuse rather than fit a sliver of them.
        if (listOf(bounds.maxX - bounds.minX, bounds.maxY - bounds.minY, bounds.maxZ - bounds.minZ).any { !it.isFinite() }) return null
        val (anchorX, anchorZ) = anchorOf(bounds, anchor)
        val halfX = maxOf(anchorX - bounds.minX, bounds.maxX - anchorX)
        val halfZ = maxOf(anchorZ - bounds.minZ, bounds.maxZ - anchorZ)
        val height = bounds.maxY - bounds.minY
        val limits = listOfNotNull(
            (box.height / height).takeIf { height.isFinite() && height > MIN_HEIGHT },
            (box.width / 2f / halfX).takeIf { halfX.isFinite() && halfX > MIN_HEIGHT },
            (box.depth / 2f / halfZ).takeIf { halfZ.isFinite() && halfZ > MIN_HEIGHT },
        )
        val scale = limits.minOrNull() ?: return null
        val placement = ModelPlacement(
            scale = scale,
            x = box.centreX - scale * anchorX,
            y = box.baseY - scale * bounds.minY,
            z = box.centreZ - scale * anchorZ,
        )
        return placement.takeIf { p -> p.scale > 0f && listOf(p.scale, p.x, p.y, p.z).all { it.isFinite() } }
    }

    /**
     * [fitWithin] for the [measured] bounds, or — when they cannot be fitted (the runtime reported an empty box) — for
     * the model's [authored] bounds, flagged so the marker can say `ok-authored-bounds`. Null when neither fits.
     */
    fun fitWithFallback(measured: ModelBounds?, authored: ModelBounds?, box: FitBox, anchor: HorizontalAnchor): FitResult? {
        measured?.let { m -> fitWithin(m, box, anchor)?.let { return FitResult(it, m, authoredBounds = false) } }
        authored?.let { a -> fitWithin(a, box, anchor)?.let { return FitResult(it, a, authoredBounds = true) } }
        return null
    }

    private fun anchorOf(bounds: ModelBounds, anchor: HorizontalAnchor): Pair<Float, Float> = when (anchor) {
        HorizontalAnchor.BOUNDS_CENTRE -> (bounds.minX + bounds.maxX) / 2f to (bounds.minZ + bounds.maxZ) / 2f
        HorizontalAnchor.MODEL_ORIGIN -> 0f to 0f
    }

    /** What [bounds] occupy after [placement]. */
    fun placed(bounds: ModelBounds, placement: ModelPlacement): ModelBounds = with(placement) {
        ModelBounds(
            minX = scale * bounds.minX + x,
            minY = scale * bounds.minY + y,
            minZ = scale * bounds.minZ + z,
            maxX = scale * bounds.maxX + x,
            maxY = scale * bounds.maxY + y,
            maxZ = scale * bounds.maxZ + z,
        )
    }
}
