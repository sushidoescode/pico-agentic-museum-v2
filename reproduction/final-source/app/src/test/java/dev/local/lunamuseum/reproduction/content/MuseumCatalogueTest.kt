package dev.local.lunamuseum.reproduction.content

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MuseumCatalogueTest {
    @Test
    fun catalogueContainsExactlyEighteenExhibits() {
        assertEquals(18, MuseumObject.values().size)
    }

    @Test
    fun everyExhibitCanBeSelected() {
        MuseumObject.values().forEach { objectType ->
            assertEquals(objectType, MuseumSelection().select(objectType).selected)
        }
    }

    @Test
    fun extraExhibitsAreIdentifiedAsPrimitivePlaceholders() {
        assertEquals(15, MuseumObject.values().count { it.isPrimitivePlaceholder })
        assertTrue(MuseumObject.COFFEE_POD.description.startsWith("Primitive placeholder:"))
    }
}
