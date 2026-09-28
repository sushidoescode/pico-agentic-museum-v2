package dev.local.everydayfutures.museum.catalogue

import dev.local.everydayfutures.museum.content.HorizontalAnchor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Host checks of the eighteen-object registry and the pure catalogue queries. They establish data and filtering, not
 * rendering: whether the thumbnails and models exist in the APK, load and look right is for the native lane.
 */
class CatalogueTest {
    private val all = Catalogue.artifacts

    @Test
    fun eighteenDistinctIdsNumberedZeroZeroOneToZeroOneEightInAccessionOrder() {
        assertEquals(18, all.size)
        assertEquals(18, all.map { it.id }.toSet().size)
        assertEquals((1..18).map { it.toString().padStart(3, '0') }, all.map { it.number })
        assertEquals(
            listOf(
                "quencher", "vacuum", "smartspeaker", "coffeepod", "deliverybox", "qrcode", "fidget", "facemask", "smartphone",
                "earbuds", "smartwatch", "cryptowallet", "remote", "router", "doorbell", "scooter", "gascan", "rocket",
            ),
            all.map { it.id },
        )
        all.forEach { a ->
            listOf(a.name, a.shortName, a.title, a.subtitle, a.category, a.era, a.identification, a.interpretation, a.question)
                .forEach { assertTrue("${a.id} has blank copy", it.isNotBlank()) }
            assertTrue(a.id, Catalogue.contains(a.id))
            assertEquals(a, Catalogue.byId(a.id))
        }
        assertNull(Catalogue.byId("astronaut"))
        assertFalse(Catalogue.contains("astronaut"))
    }

    @Test
    fun sixObjectsInEachSection() {
        Section.values().forEach { section -> assertEquals(section.name, 6, Catalogue.bySection(section).size) }
        assertEquals(
            listOf("quencher", "coffeepod", "deliverybox", "qrcode", "fidget", "facemask"),
            Catalogue.bySection(Section.DAILY_RITUALS).map { it.id },
        )
        assertEquals(
            listOf("smartspeaker", "smartphone", "earbuds", "smartwatch", "cryptowallet", "remote"),
            Catalogue.bySection(Section.CONNECTED_LIVES).map { it.id },
        )
        assertEquals(
            listOf("vacuum", "router", "doorbell", "scooter", "gascan", "rocket"),
            Catalogue.bySection(Section.DOMESTIC_SYSTEMS).map { it.id },
        )
        assertEquals(listOf("daily", "connected", "domestic"), Section.values().map { it.key })
        assertEquals("Domestic systems and movement", Section.DOMESTIC_SYSTEMS.label)
    }

    @Test
    fun everyModelAndThumbnailFollowsTheAssetConvention() {
        all.forEach { a ->
            assertEquals("thumbnails/${a.id}.png", a.thumbnail)
            val expected = if (a.id == Catalogue.QUENCHER) Catalogue.VESSEL_URI else "asset://models/exhibits/${a.id}.glb"
            assertEquals(a.id, expected, a.model.uri)
            assertEquals(expected.removePrefix("asset://"), a.model.assetPath)
            with(a.model.realSize) {
                assertTrue(a.id, height > 0f && width > 0f && depth > 0f)
                assertTrue(a.id, height <= 1.2f && width <= 1.2f && depth <= 1.2f)
            }
            assertEquals(0f, a.model.yawOffsetDegrees, 0f)
        }
        val vessel = Catalogue.byId(Catalogue.QUENCHER)!!.model
        assertEquals("asset://models/vessel_study.glb", vessel.uri)
        assertEquals(HorizontalAnchor.MODEL_ORIGIN, vessel.anchor)
        assertEquals(Catalogue.VESSEL_AUTHORED_BOUNDS, vessel.authoredBounds)
        assertTrue(all.filter { it.id != Catalogue.QUENCHER }.all { it.model.anchor == HorizontalAnchor.BOUNDS_CENTRE && it.model.authoredBounds == null })
        assertEquals(Size3(height = 0.545f, width = 0.12f, depth = 0.121f), Catalogue.byId("rocket")!!.model.realSize) // measured, assets/exhibit-assets.json
        assertEquals(Size3(height = 1.15f, width = 0.534f, depth = 0.95f), Catalogue.byId("scooter")!!.model.realSize) // measured
        assertNull(ModelSpec(uri = "file:///x.glb", realSize = Size3(1f, 1f, 1f)).assetPath)
        assertEquals("models/x.glb", ModelSpec(uri = "assets://models/x.glb", realSize = Size3(1f, 1f, 1f)).assetPath)
    }

    @Test
    fun onlyTheThreeOriginalsHaveStoriesAndAProceduralFallback() {
        val originals = listOf(Catalogue.QUENCHER, Catalogue.VACUUM, Catalogue.SMART_SPEAKER)
        assertEquals(originals, all.filter { it.hasStory }.map { it.id })
        assertEquals(originals, all.filter { it.model.proceduralFallback }.map { it.id })
        all.filter { it.id !in originals }.forEach { a ->
            assertTrue(a.id, a.chapters.isEmpty())
            assertEquals(a.id, a.name, a.selectLabel)
        }
    }

    /** The three accepted entries keep their copy verbatim (their `description` is now `interpretation`) and gain name, section and identification. */
    @Test
    fun theThreeAcceptedEntriesKeepTheirCopy() {
        val quencher = Catalogue.byId(Catalogue.QUENCHER)!!
        assertEquals("The vessel", quencher.shortName)
        assertEquals("Quencher cup", quencher.selectLabel)
        assertEquals("Holding more.", quencher.title)
        assertEquals(
            "A familiar companion for the commute, the desk and the long way home. " +
                "An everyday vessel became a small declaration of how we wanted to live.",
            quencher.interpretation,
        )
        assertEquals(listOf("Ritual", "Signal", "Afterlife"), quencher.chapters.map { it.title })
        assertEquals("Handled tumbler", quencher.name)
        assertEquals(Section.DAILY_RITUALS, quencher.section)

        val vacuum = Catalogue.byId(Catalogue.VACUUM)!!
        assertEquals("The helper", vacuum.shortName)
        assertEquals("A quieter chore.", vacuum.title)
        assertEquals("What could a floor plan tell someone about your life?", vacuum.question)
        assertEquals(listOf("Routine", "Boundary", "Human trace"), vacuum.chapters.map { it.title })
        assertEquals(Section.DOMESTIC_SYSTEMS, vacuum.section)

        val speaker = Catalogue.byId(Catalogue.SMART_SPEAKER)!!
        assertEquals("The listener", speaker.shortName)
        assertEquals("A room, listening.", speaker.title)
        assertEquals(listOf("Invitation", "Connection", "Silence"), speaker.chapters.map { it.title })
        assertEquals(Section.CONNECTED_LIVES, speaker.section)
        listOf(quencher, vacuum, speaker).forEach { assertEquals("The 2020s", it.era) }
    }

    @Test
    fun sectionSavedAndSearchFiltersCombine() {
        val none = CatalogueFilter()
        assertEquals(all, CatalogueQuery.visible(all, none, emptyList()))
        assertFalse(none.isActive)

        val daily = CatalogueFilter(section = Section.DAILY_RITUALS)
        assertEquals(Catalogue.bySection(Section.DAILY_RITUALS), CatalogueQuery.visible(all, daily, emptyList()))

        val saved = listOf("rocket", "quencher", "earbuds")
        assertEquals(listOf("quencher", "earbuds", "rocket"), CatalogueQuery.visible(all, CatalogueFilter(savedOnly = true), saved).map { it.id })
        assertEquals(listOf("quencher"), CatalogueQuery.visible(all, CatalogueFilter(Section.DAILY_RITUALS, savedOnly = true), saved).map { it.id })
        assertTrue(CatalogueFilter(savedOnly = true).isActive && CatalogueFilter(query = "x").isActive && daily.isActive)
        assertFalse(CatalogueFilter(query = "   ").isActive)
    }

    @Test
    fun searchMatchesNameNumberShortNameAndSectionIgnoringCase() {
        fun ids(q: String, section: Section? = null) = CatalogueQuery.visible(all, CatalogueFilter(section = section, query = q), emptyList()).map { it.id }
        assertEquals(listOf("coffeepod"), ids("COFFEE"))
        assertEquals(listOf("coffeepod"), ids("004"))
        assertEquals(listOf("earbuds"), ids("private ear")) // short name
        assertEquals(Catalogue.bySection(Section.CONNECTED_LIVES).map { it.id }, ids("connected")) // section label
        assertEquals(listOf("quencher"), ids("tumbler"))
        assertEquals(listOf("smartspeaker", "smartphone", "smartwatch"), ids("smart"))
        assertEquals(listOf("smartwatch", "doorbell"), ids("  WATCH  ")) // trimmed; the doorbell is "The watcher"
        assertEquals(all.map { it.id }, ids(""))
        assertEquals(listOf("smartspeaker", "smartphone", "smartwatch"), ids("smart", Section.CONNECTED_LIVES))
        assertTrue(ids("astronaut").isEmpty())
        assertTrue(ids("smart", Section.DAILY_RITUALS).isEmpty())
    }

    @Test
    fun countsAreDerivedFromTheData() {
        assertEquals(CatalogueCount(visible = 18, total = 18, saved = 0, filtered = false), CatalogueQuery.count(all, CatalogueFilter(), emptyList()))
        assertEquals(
            CatalogueCount(visible = 6, total = 18, saved = 3, filtered = true),
            CatalogueQuery.count(all, CatalogueFilter(section = Section.DOMESTIC_SYSTEMS), listOf("rocket", "quencher", "earbuds")),
        )
        // A saved id the registry no longer has is not counted.
        assertEquals(1, CatalogueQuery.count(all, CatalogueFilter(), listOf("rocket", "astronaut")).saved)
    }

    @Test
    fun emptyStatesSayWhyAndSearchIsNamedFirst() {
        assertNull(CatalogueQuery.emptyReason(all, CatalogueFilter(), emptyList()))
        assertEquals(EmptyReason.NO_MATCH, CatalogueQuery.emptyReason(all, CatalogueFilter(query = "astronaut"), emptyList()))
        assertEquals(EmptyReason.NOTHING_SAVED, CatalogueQuery.emptyReason(all, CatalogueFilter(savedOnly = true), emptyList()))
        assertEquals(
            EmptyReason.NONE_IN_VIEW,
            CatalogueQuery.emptyReason(all, CatalogueFilter(section = Section.DAILY_RITUALS, savedOnly = true), listOf("rocket")),
        )
        // The search is what empties it: without the query the saved view would show the rocket.
        assertEquals(EmptyReason.NO_MATCH, CatalogueQuery.emptyReason(all, CatalogueFilter(savedOnly = true, query = "zzz"), listOf("rocket")))
        // Nothing saved at all: the saved state wins over a query.
        assertEquals(EmptyReason.NOTHING_SAVED, CatalogueQuery.emptyReason(all, CatalogueFilter(savedOnly = true, query = "zzz"), emptyList()))
        assertEquals(EmptyReason.NO_MATCH, CatalogueQuery.emptyReason(all, CatalogueFilter(Section.DAILY_RITUALS, query = "smart"), emptyList()))
    }

    @Test
    fun aSelectionTheFilterHidesIsReportedNotDropped() {
        val daily = CatalogueFilter(section = Section.DAILY_RITUALS)
        assertTrue(CatalogueQuery.selectionHidden(all, daily, emptyList(), "rocket"))
        assertFalse(CatalogueQuery.selectionHidden(all, daily, emptyList(), "coffeepod"))
        assertFalse(CatalogueQuery.selectionHidden(all, CatalogueFilter(), emptyList(), "rocket"))
        assertTrue(CatalogueQuery.selectionHidden(all, CatalogueFilter(savedOnly = true), emptyList(), "quencher"))
        assertFalse(CatalogueQuery.selectionHidden(all, daily, emptyList(), "astronaut")) // unknown ids are not "hidden"
    }

    @Test
    fun theSearchTextIsOneBoundedLine() {
        assertEquals("coffee pod", CatalogueQuery.sanitize("coffee\npod"))
        assertEquals(CatalogueQuery.MAX_QUERY_LENGTH, CatalogueQuery.sanitize("x".repeat(500)).length)
        assertEquals("", CatalogueQuery.sanitize(""))
    }

    @Test
    fun realSizesReadAsCentimetresOrMetres() {
        assertEquals("3 cm", SizeText.length(0.03f))
        assertEquals("1.2 cm", SizeText.length(0.012f))
        assertEquals("0.9 cm", SizeText.length(0.009f))
        assertEquals("4.5 cm", SizeText.length(0.045f))
        assertEquals("30 cm", SizeText.length(0.30f))
        assertEquals("55 cm", SizeText.length(0.55f))
        assertEquals("1.15 m", SizeText.length(1.15f))
        assertEquals("1 m", SizeText.length(1.0f))
        assertEquals("—", SizeText.length(0f))
        assertEquals("—", SizeText.length(Float.NaN))
        all.forEach { a -> assertFalse(a.id, SizeText.length(a.model.realSize.height) == "—") }
    }

    @Test
    fun realSizeIsPhrasedByTheLargestDimension() {
        assertEquals(SizeText.Extent.LONG to 0.152f, SizeText.largest(Size3(height = 0.018f, width = 0.038f, depth = 0.152f)))
        assertEquals(SizeText.Extent.TALL to 1.15f, SizeText.largest(Size3(height = 1.15f, width = 0.534f, depth = 0.95f)))
        assertEquals(SizeText.Extent.WIDE to 0.075f, SizeText.largest(Size3(height = 0.013f, width = 0.075f, depth = 0.07f)))
        // Ties prefer height, then width.
        assertEquals(SizeText.Extent.TALL to 0.1f, SizeText.largest(Size3(height = 0.1f, width = 0.1f, depth = 0.1f)))
        // Every registry entry has a positive largest dimension, so no object reads "—".
        Catalogue.artifacts.forEach { assertTrue(it.id, SizeText.largest(it.model.realSize).second > 0f) }
    }

    @Test
    fun enterInTheSearchFieldSubmitsWithoutKeepingTheLineBreak() {
        assertTrue(CatalogueQuery.isSubmit("phone\n"))
        assertTrue(CatalogueQuery.isSubmit("pho\r\nne"))
        assertFalse(CatalogueQuery.isSubmit("phone"))
        assertEquals("phone", CatalogueQuery.withoutLineBreaks("phone\n"))
        assertEquals("phone", CatalogueQuery.withoutLineBreaks("pho\r\nne"))
    }
}
