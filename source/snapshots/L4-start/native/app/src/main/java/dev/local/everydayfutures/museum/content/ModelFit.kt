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
