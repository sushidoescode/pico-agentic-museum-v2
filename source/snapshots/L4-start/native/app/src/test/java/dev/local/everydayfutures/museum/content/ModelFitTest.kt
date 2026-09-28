package dev.local.everydayfutures.museum.content

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Host checks of the generated model's fit: any usable bounds end up on the vessel's footprint (0.28 m tall, base at
 * y = -0.075 m, on x/z = 0) with proportions kept, and bounds that cannot be fitted are refused so the procedural model stays.
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
}
