package dev.local.everydayfutures.museum.content

import dev.local.everydayfutures.museum.catalogue.Catalogue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Host checks of the model fit. [ModelFit.fit] (height only, unchanged since polish-001): any usable bounds end up on
 * the vessel's footprint (0.28 m tall, base at y = -0.075 m, on x/z = 0) with proportions kept, and bounds that cannot
 * be fitted are refused. [ModelFit.fitWithin] (Earth Collection): one uniform scale, the tightest of height, width and
 * depth, base on the box's base, anchor on its centre; flat, tall, wide and tiny models; degenerate input refused.
 */
class ModelFitTest {
    private val vessel = FitTarget(height = 0.28f, baseY = -0.075f, centreX = 0f, centreZ = 0f)
    private val eps = 1e-6f

    private fun fitted(bounds: ModelBounds, target: FitTarget, anchor: HorizontalAnchor): Pair<ModelPlacement, ModelBounds> {
        val placement = ModelFit.fit(bounds, target, anchor)
        assertNotNull("bounds $bounds should fit", placement)
        return placement!! to ModelFit.placed(bounds, placement)
    }

    private fun assertBaseAndHeight(placed: ModelBounds, target: FitTarget) {
        assertEquals("base", target.baseY, placed.minY, eps)
        assertEquals("height", target.height, placed.maxY - placed.minY, eps)
    }

    @Test
    fun aUnitCubeAtTheOriginStandsOnTheVesselFootprint() {
        val (placement, placed) = fitted(ModelBounds(-0.5f, -0.5f, -0.5f, 0.5f, 0.5f, 0.5f), vessel, HorizontalAnchor.BOUNDS_CENTRE)
        assertEquals(0.28f, placement.scale, eps)
        assertBaseAndHeight(placed, vessel)
        assertEquals(0f, (placed.minX + placed.maxX) / 2f, eps)
        assertEquals(0f, (placed.minZ + placed.maxZ) / 2f, eps)
    }

    @Test
    fun anOffCentreModelInCentimetresIsScaledAndCentred() {
        val source = ModelBounds(minX = 10f, minY = 5f, minZ = -20f, maxX = 30f, maxY = 45f, maxZ = 0f)
        val (placement, placed) = fitted(source, vessel, HorizontalAnchor.BOUNDS_CENTRE)
        assertEquals(0.28f / 40f, placement.scale, eps)
        assertBaseAndHeight(placed, vessel)
        assertEquals(0f, (placed.minX + placed.maxX) / 2f, eps)
        assertEquals(0f, (placed.minZ + placed.maxZ) / 2f, eps)
    }

    @Test
    fun theFitIsUniformSoProportionsAreKept() {
        val source = ModelBounds(minX = -0.3f, minY = 0f, minZ = -0.2f, maxX = 0.5f, maxY = 1f, maxZ = 0.2f)
        val (_, placed) = fitted(source, vessel, HorizontalAnchor.BOUNDS_CENTRE)
        assertEquals((source.maxX - source.minX) / (source.maxY - source.minY), (placed.maxX - placed.minX) / (placed.maxY - placed.minY), eps)
        assertEquals((source.maxZ - source.minZ) / (source.maxY - source.minY), (placed.maxZ - placed.minZ) / (placed.maxY - placed.minY), eps)
    }

    @Test
    fun theModelOriginAnchorKeepsTheAuthoredAxisOnTheCentre() {
        // A body on the origin with a handle reaching to +x: the axis stays on x/z = 0 and the handle stays off to +x.
        val source = ModelBounds(minX = -0.4f, minY = -0.6f, minZ = -0.4f, maxX = 0.7f, maxY = 1.4f, maxZ = 0.4f)
        val (placement, placed) = fitted(source, vessel, HorizontalAnchor.MODEL_ORIGIN)
        assertEquals(0.28f / 2f, placement.scale, eps)
        assertEquals(0f, placement.x, eps)
        assertEquals(0f, placement.z, eps)
        assertBaseAndHeight(placed, vessel)
        assertEquals(0.7f * placement.scale, placed.maxX, eps)
        assertEquals(-0.4f * placement.scale, placed.minX, eps)
    }

    @Test
    fun aTargetCentreAwayFromTheOriginIsHonouredByBothAnchors() {
        val target = FitTarget(height = 0.1f, baseY = 0.2f, centreX = 0.05f, centreZ = -0.03f)
        val source = ModelBounds(minX = 1f, minY = 2f, minZ = 3f, maxX = 3f, maxY = 4f, maxZ = 5f)
        val (_, centred) = fitted(source, target, HorizontalAnchor.BOUNDS_CENTRE)
        assertBaseAndHeight(centred, target)
        assertEquals(0.05f, (centred.minX + centred.maxX) / 2f, eps)
        assertEquals(-0.03f, (centred.minZ + centred.maxZ) / 2f, eps)
        val (origin, _) = fitted(source, target, HorizontalAnchor.MODEL_ORIGIN)
        assertEquals(0.05f, origin.x, eps)
        assertEquals(-0.03f, origin.z, eps)
    }

    @Test
    fun emptyFlatInvertedOrNonFiniteBoundsAreRefused() {
        val refused = listOf(
            ModelBounds(0f, 0f, 0f, 0f, 0f, 0f), // the SDK's empty box
            ModelBounds(-1f, 0.5f, -1f, 1f, 0.5f, 1f), // flat: no height
            ModelBounds(-1f, 0f, -1f, 1f, 1e-7f, 1f), // below the minimum height
            ModelBounds(-1f, 1f, -1f, 1f, 0f, 1f), // inverted y
            ModelBounds(1f, 0f, -1f, -1f, 1f, 1f), // inverted x
            ModelBounds(-1f, 0f, 1f, 1f, 1f, -1f), // inverted z
            ModelBounds(Float.NaN, 0f, 0f, 1f, 1f, 1f),
            ModelBounds(0f, Float.NEGATIVE_INFINITY, 0f, 1f, 1f, 1f),
            ModelBounds(0f, 0f, 0f, 1f, Float.POSITIVE_INFINITY, 1f),
            ModelBounds(0f, -Float.MAX_VALUE, 0f, 1f, Float.MAX_VALUE, 1f), // height overflows
        )
        HorizontalAnchor.values().forEach { anchor ->
            refused.forEach { bounds -> assertNull("$bounds with $anchor", ModelFit.fit(bounds, vessel, anchor)) }
        }
    }

    @Test
    fun aTargetThatCannotBeMetIsRefused() {
        val cube = ModelBounds(-0.5f, -0.5f, -0.5f, 0.5f, 0.5f, 0.5f)
        assertNull(ModelFit.fit(cube, vessel.copy(height = 0f)))
        assertNull(ModelFit.fit(cube, vessel.copy(height = -0.28f)))
        assertNull(ModelFit.fit(cube, vessel.copy(height = Float.NaN)))
        assertNull(ModelFit.fit(cube, vessel.copy(baseY = Float.POSITIVE_INFINITY)))
        assertNull(ModelFit.fit(cube, vessel.copy(centreX = Float.NaN)))
    }

    // ---- fitWithin ----

    private val box = FitBox(width = 0.26f, height = 0.28f, depth = 0.26f, baseY = -0.075f)

    private fun within(bounds: ModelBounds, target: FitBox, anchor: HorizontalAnchor = HorizontalAnchor.BOUNDS_CENTRE): Pair<ModelPlacement, ModelBounds> {
        val placement = ModelFit.fitWithin(bounds, target, anchor)
        assertNotNull("bounds $bounds should fit $target", placement)
        return placement!! to ModelFit.placed(bounds, placement)
    }

    private fun assertInside(placed: ModelBounds, target: FitBox) {
        assertTrue("base $placed", kotlin.math.abs(placed.minY - target.baseY) <= eps)
        assertTrue("height $placed", placed.maxY - placed.minY <= target.height + eps)
        assertTrue("width $placed", placed.minX >= target.centreX - target.width / 2f - eps && placed.maxX <= target.centreX + target.width / 2f + eps)
        assertTrue("depth $placed", placed.minZ >= target.centreZ - target.depth / 2f - eps && placed.maxZ <= target.centreZ + target.depth / 2f + eps)
    }

    @Test
    fun aTallModelIsLimitedByHeight() {
        // A 0.55 m rocket, 0.12 m across: height decides.
        val (p, placed) = within(ModelBounds(-0.06f, 0f, -0.06f, 0.06f, 0.55f, 0.06f), box)
        assertEquals(0.28f / 0.55f, p.scale, eps)
        assertEquals(0.28f, placed.maxY - placed.minY, eps)
        assertInside(placed, box)
    }

    @Test
    fun aWideModelIsLimitedByWidth() {
        // A robot vacuum, 0.34 m across and 0.09 m tall: width decides and the height stays well under the box.
        val (p, placed) = within(ModelBounds(-0.17f, 0f, -0.17f, 0.17f, 0.09f, 0.17f), box)
        assertEquals(0.26f / 0.34f, p.scale, eps)
        assertEquals(0.26f, placed.maxX - placed.minX, eps)
        assertTrue(placed.maxY - placed.minY < 0.28f)
        assertInside(placed, box)
    }

    @Test
    fun aDeepModelIsLimitedByDepth() {
        val deepBox = box.copy(width = 0.4f, depth = 0.2f)
        val (p, placed) = within(ModelBounds(-0.05f, 0f, -0.5f, 0.05f, 0.2f, 0.5f), deepBox)
        assertEquals(0.2f / 1.0f, p.scale, eps)
        assertInside(placed, deepBox)
    }

    @Test
    fun aFlatModelStillFitsAndStandsOnTheBase() {
        // A fidget spinner lying flat (1.2 cm tall, 7.5 cm across), and a card with no height at all.
        val (spinner, placedSpinner) = within(ModelBounds(-0.0375f, 0f, -0.0375f, 0.0375f, 0.012f, 0.0375f), box)
        assertEquals(0.26f / 0.075f, spinner.scale, eps)
        assertInside(placedSpinner, box)
        val (card, placedCard) = within(ModelBounds(-0.05f, 0f, -0.07f, 0.05f, 0f, 0.07f), box)
        assertEquals(0.26f / 0.14f, card.scale, eps)
        assertEquals(box.baseY, placedCard.minY, eps)
    }

    @Test
    fun aTinyModelIsScaledUpToTheBox() {
        // A coffee pod in metres (3 cm tall, 4 cm across) fills the box's width; a millimetre model the same way.
        val (pod, placedPod) = within(ModelBounds(-0.02f, 0f, -0.02f, 0.02f, 0.03f, 0.02f), box)
        assertEquals(0.26f / 0.04f, pod.scale, eps)
        assertTrue(pod.scale > 1f)
        assertInside(placedPod, box)
        val (mm, _) = within(ModelBounds(-20f, 0f, -20f, 20f, 30f, 20f), box)
        assertEquals(0.26f / 40f, mm.scale, 1e-7f)
    }

    @Test
    fun theAnchorModesPutTheRightPointOnTheCentre() {
        // Body axis at the origin, handle reaching to +x: the origin anchor keeps the axis on the centre and the whole
        // handle inside the box (the half-width is measured from the anchor to the farther side).
        val handled = ModelBounds(minX = -0.19f, minY = -0.499f, minZ = -0.179f, maxX = 0.336f, maxY = 0.499f, maxZ = 0.179f)
        val (origin, placedOrigin) = within(handled, box, HorizontalAnchor.MODEL_ORIGIN)
        assertEquals(0f, origin.x, eps)
        assertEquals(0f, origin.z, eps)
        assertInside(placedOrigin, box)
        val (centred, placedCentred) = within(handled, box, HorizontalAnchor.BOUNDS_CENTRE)
        assertEquals(0f, (placedCentred.minX + placedCentred.maxX) / 2f, eps)
        assertInside(placedCentred, box)
        // An off-centre box centre is honoured by both.
        val shifted = box.copy(centreX = 0.05f, centreZ = -0.03f)
        val (o, _) = within(handled, shifted, HorizontalAnchor.MODEL_ORIGIN)
        assertEquals(0.05f, o.x, eps)
        assertEquals(-0.03f, o.z, eps)
    }

    @Test
    fun theVesselStillGetsItsAcceptedHeightFitInTheStudy() {
        // The accepted vessel's authored bounds, with the origin anchor, are limited by height in the new study box, so
        // its placement equals the polish-001 height-only fit (0.28 m tall, base at -0.075 m, axis on x/z = 0).
        val spec = Catalogue.byId(Catalogue.QUENCHER)!!.model
        val old = ModelFit.fit(spec.authoredBounds!!, vessel, HorizontalAnchor.MODEL_ORIGIN)
        val new = ModelFit.fitWithin(spec.authoredBounds!!, ExhibitBoxes.STUDY, spec.anchor)
        assertNotNull(old)
        assertNotNull(new)
        assertEquals(old!!.scale, new!!.scale, eps)
        assertEquals(old.y, new.y, eps)
        assertEquals(old.x, new.x, eps)
    }

    @Test
    fun everyObjectsRealSizeFitsBothSurfaceBoxes() {
        Catalogue.artifacts.forEach { a ->
            val s = a.model.realSize
            val bounds = ModelBounds(-s.width / 2f, 0f, -s.depth / 2f, s.width / 2f, s.height, s.depth / 2f)
            listOf(ExhibitBoxes.STUDY, ExhibitBoxes.INSPECTOR).forEach { target ->
                val placement = ModelFit.fitWithin(bounds, target)
                assertNotNull("${a.id} in $target", placement)
                assertInside(ModelFit.placed(bounds, placement!!), target)
            }
        }
    }

    @Test
    fun degenerateInputIsRefused() {
        val refused = listOf(
            ModelBounds(0f, 0f, 0f, 0f, 0f, 0f), // the SDK's empty box
            ModelBounds(1f, 0f, -1f, -1f, 1f, 1f), // inverted x
            ModelBounds(-1f, 1f, -1f, 1f, 0f, 1f), // inverted y
            ModelBounds(-1f, 0f, 1f, 1f, 1f, -1f), // inverted z
            ModelBounds(Float.NaN, 0f, 0f, 1f, 1f, 1f),
            ModelBounds(0f, Float.NEGATIVE_INFINITY, 0f, 1f, 1f, 1f),
            ModelBounds(0f, -Float.MAX_VALUE, 0f, 1f, Float.MAX_VALUE, 1f), // height overflows
            ModelBounds(-Float.MAX_VALUE, 0f, 0f, Float.MAX_VALUE, 1f, 1f), // width overflows
        )
        HorizontalAnchor.values().forEach { anchor ->
            refused.forEach { b -> assertNull("$b with $anchor", ModelFit.fitWithin(b, box, anchor)) }
        }
        val cube = ModelBounds(-0.5f, -0.5f, -0.5f, 0.5f, 0.5f, 0.5f)
        assertNull(ModelFit.fitWithin(cube, box.copy(width = 0f)))
        assertNull(ModelFit.fitWithin(cube, box.copy(height = -1f)))
        assertNull(ModelFit.fitWithin(cube, box.copy(depth = Float.NaN)))
        assertNull(ModelFit.fitWithin(cube, box.copy(baseY = Float.POSITIVE_INFINITY)))
    }

    @Test
    fun authoredBoundsStandInOnlyWhenTheMeasuredBoxCannotBeFitted() {
        val measured = ModelBounds(-0.1f, 0f, -0.1f, 0.1f, 0.4f, 0.1f)
        val authored = ModelBounds(-0.2f, 0f, -0.2f, 0.2f, 0.2f, 0.2f)
        val direct = ModelFit.fitWithFallback(measured, authored, box, HorizontalAnchor.BOUNDS_CENTRE)!!
        assertFalse(direct.authoredBounds)
        assertEquals(measured, direct.bounds)
        val empty = ModelFit.fitWithFallback(ModelBounds(0f, 0f, 0f, 0f, 0f, 0f), authored, box, HorizontalAnchor.BOUNDS_CENTRE)!!
        assertTrue(empty.authoredBounds)
        assertEquals(authored, empty.bounds)
        assertInside(empty.placed, box)
        assertTrue(ModelFit.fitWithFallback(null, authored, box, HorizontalAnchor.BOUNDS_CENTRE)!!.authoredBounds)
        assertNull(ModelFit.fitWithFallback(null, null, box, HorizontalAnchor.BOUNDS_CENTRE))
        assertNull(ModelFit.fitWithFallback(ModelBounds(0f, 0f, 0f, 0f, 0f, 0f), null, box, HorizontalAnchor.BOUNDS_CENTRE))
        assertEquals(ModelBounds(-0.13f, -0.075f, -0.13f, 0.13f, 0.205f, 0.13f), box.bounds)
    }
}
