package dev.local.picopolish

import androidx.compose.animation.core.SnapSpec
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pico.spatial.ui.design.PicoTheme
import com.pico.spatial.ui.design.vibrantColorScheme
import com.pico.spatial.ui.design.Text
import com.pico.spatial.ui.design.tokens.MotionTokens
import com.pico.spatial.ui.design.tokens.SpringEasingArgs
import com.pico.spatial.ui.foundation.vibrant.Vibrant
import com.pico.spatial.ui.foundation.vibrant.containsVibrant
import com.pico.spatial.ui.foundation.vibrant.obtainVibrant
import com.pico.spatial.ui.foundation.vibrant.withVibrant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The design contract, pinned.
 *
 * Everything here is a *rule* the kit states in prose somewhere, turned into something that fails.
 * Same honesty scope as `PolishBehaviourTest`: Robolectric, `isSpatialPlatform()` false, the
 * non-adaptive colour scheme, `backgroundMaterial` a no-op. **No test here claims anything about how
 * glass looks** — the glass-budget tests assert which *decision* the kit made, not what it rendered.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PolishDesignSystemTest {

    @get:Rule val rule = createComposeRule()

    // ---- The type ladder -------------------------------------------------------------------------

    @Test
    fun theLadderNeverStepsUpInSize() {
        // Rank order must be size-monotonic downwards: a rung may equal the one above it (weight then
        // does the work) but may never be larger, or the ladder is not a ladder.
        var sizes: List<Pair<String, Float>> = emptyList()
        rule.setContent {
            PolishTheme {
                sizes = PolishTypography.ladder().map { (name, style) -> name to style.fontSize.value }
            }
        }
        rule.runOnIdle {
            assertTrue("the ladder is empty", sizes.isNotEmpty())
            sizes.zipWithNext { (aName, a), (bName, b) ->
                assertTrue("$bName ($b sp) is larger than $aName ($a sp), which is above it", b <= a)
            }
        }
    }

    @Test
    fun everyAdjacentRungDiffersBySizeOrWeight() {
        // The defect this pins: the kit's first version put an object title at 14 sp / W600 directly
        // above body at 14 sp / W500. Identical size, one weight step — the weakest distinction the
        // SDK can express. Two adjacent rungs that differ in neither would be no distinction at all.
        var rungs: List<Pair<String, TextStyle>> = emptyList()
        rule.setContent { PolishTheme { rungs = PolishTypography.ladder() } }
        rule.runOnIdle {
            rungs.zipWithNext { (aName, a), (bName, b) ->
                val sameSize = a.fontSize == b.fontSize
                val sameWeight = a.fontWeight == b.fontWeight
                val sameLineHeight = a.lineHeight == b.lineHeight
                assertFalse(
                    "$aName and $bName are the same size, weight and line height — no rank between them",
                    sameSize && sameWeight && sameLineHeight,
                )
            }
        }
    }

    @Test
    fun theLadderSpansAtLeastAFactorOfTwo() {
        var top = 0f
        var bottom = 0f
        rule.setContent {
            PolishTheme {
                top = PolishTypography.display.fontSize.value
                bottom = PolishTypography.overline.fontSize.value
            }
        }
        rule.runOnIdle {
            assertTrue("display $top sp / overline $bottom sp is less than 2x", top >= bottom * 2f)
        }
    }

    @Test
    fun multilineRolesOpenTheLineHeightWithoutChangingTheSize() {
        var tight: TextStyle? = null
        var loose: TextStyle? = null
        var leadTight: TextStyle? = null
        var leadLoose: TextStyle? = null
        rule.setContent {
            PolishTheme {
                tight = PolishTypography.body()
                loose = PolishTypography.body(multiline = true)
                leadTight = PolishTypography.bodyLead()
                leadLoose = PolishTypography.bodyLead(multiline = true)
            }
        }
        rule.runOnIdle {
            assertEquals(tight!!.fontSize, loose!!.fontSize)
            assertEquals(tight!!.fontWeight, loose!!.fontWeight)
            assertTrue(loose!!.lineHeight.value > tight!!.lineHeight.value)
            assertEquals(leadTight!!.fontSize, leadLoose!!.fontSize)
            assertTrue(leadLoose!!.lineHeight.value > leadTight!!.lineHeight.value)
        }
    }

    // ---- Which rungs get the 3:1 allowance -------------------------------------------------------

    @Test
    fun onlyTheBoldHeaderRungsQualifyAsLargeText() {
        // Three of nine. Everything else is held to 4.5:1 — including the W600 title and control
        // rungs, because the kit does not count SemiBold as bold.
        val qualifies = mutableMapOf<String, Boolean>()
        rule.setContent {
            PolishTheme {
                qualifies["display"] = PolishContrast.isLargeText(PolishTypography.display)
                qualifies["windowTitle"] = PolishContrast.isLargeText(PolishTypography.windowTitle)
                qualifies["sectionTitle"] = PolishContrast.isLargeText(PolishTypography.sectionTitle)
                qualifies["objectTitle"] = PolishContrast.isLargeText(PolishTypography.objectTitle)
                qualifies["bodyLead"] = PolishContrast.isLargeText(PolishTypography.bodyLead())
                qualifies["body"] = PolishContrast.isLargeText(PolishTypography.body())
                qualifies["control"] = PolishContrast.isLargeText(PolishTypography.control)
                qualifies["caption"] = PolishContrast.isLargeText(PolishTypography.caption)
                qualifies["overline"] = PolishContrast.isLargeText(PolishTypography.overline)
            }
        }
        rule.runOnIdle {
            assertEquals(true, qualifies["display"])
            assertEquals(true, qualifies["windowTitle"])
            assertEquals(true, qualifies["sectionTitle"])
            assertEquals(false, qualifies["objectTitle"])
            assertEquals(false, qualifies["bodyLead"])
            assertEquals(false, qualifies["body"])
            assertEquals(false, qualifies["control"])
            assertEquals(false, qualifies["caption"])
            assertEquals(false, qualifies["overline"])
        }
    }

    @Test
    fun semiBoldIsNotCountedAsBold() {
        val semi = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        val bold = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold)
        assertFalse(PolishContrast.isLargeText(semi))
        assertTrue(PolishContrast.isLargeText(bold))
        assertEquals(PolishContrast.NormalText, PolishContrast.targetFor(semi))
        assertEquals(PolishContrast.LargeText, PolishContrast.targetFor(bold))
    }

    @Test
    fun anUnjudgeableSizeTakesTheStricterTarget() {
        assertFalse(PolishContrast.isLargeText(TextStyle(fontWeight = FontWeight.Bold)))
        assertEquals(
            PolishContrast.NormalText,
            PolishContrast.targetFor(TextStyle(fontWeight = FontWeight.Bold)),
        )
    }

    // ---- Contrast maths, and its refusals --------------------------------------------------------

    @Test
    fun blackOnWhiteIsTwentyOne() {
        val result = PolishContrast.ratio(Color.Black, Color.White)
        assertTrue(result is PolishContrastResult.Measured)
        val measured = result as PolishContrastResult.Measured
        assertEquals(21.0, measured.ratio.toDouble(), 0.01)
        assertTrue(measured.meets)
    }

    @Test
    fun aRatioExactlyOnTargetCounts() {
        // Guards the float tolerance: a pairing that computes to the target must not fail on the
        // last bit. 4.5 against itself is the degenerate case; use a constructed one instead.
        val measured = PolishContrastResult.Measured(ratio = 4.5f, target = 4.5f)
        assertTrue(measured.meets)
        assertFalse(PolishContrastResult.Measured(ratio = 4.49f, target = 4.5f).meets)
    }

    @Test
    fun aVibrantRoleIsRefusedRatherThanMeasured() {
        // The whole point. On device these read back as #FF000000, so a ratio would say 1.00:1 for a
        // pairing the compositor renders perfectly legibly.
        val sentinel = Color.Black.withVibrant(Vibrant.Darker)
        val asForeground = PolishContrast.ratio(sentinel, Color.White)
        val asBackground = PolishContrast.ratio(Color.White, sentinel)
        assertEquals(
            PolishContrastResult.Undecidable(PolishContrastLimit.VibrantForeground),
            asForeground,
        )
        assertEquals(
            PolishContrastResult.Undecidable(PolishContrastLimit.VibrantBackground),
            asBackground,
        )
    }

    @Test
    fun theSixSupposedlyLiteralRolesAreActuallyReadable() {
        // A reviewer raised this against the kit's own documentation. `vibrantColorScheme()` builds
        // `labelPrimaryLight` and `dividerLine` as `<a real colour>.withVibrant(Vibrant.None)`
        // (`ColorScheme.kt:235, :246`), so they carry a vibrancy ENCODING even though `Vibrant.None`
        // is the level that *disables* the effect (`Vibrant.kt:102`). The kit's first predicate asked
        // only `containsVibrant()` and therefore refused exactly the two roles its own documentation
        // promised were measurable — on a device, silently, and invisibly to the JVM where the scheme
        // falls back to literals.
        //
        // Both halves are asserted, so the concern is shown to be real and shown to be handled.
        val scheme = vibrantColorScheme()
        val roles =
            mapOf(
                "labelPrimaryLight" to scheme.labelPrimaryLight,
                "dividerLine" to scheme.dividerLine,
                "error" to scheme.error,
                "alert" to scheme.alert,
                "passable" to scheme.passable,
                "interaction" to scheme.interaction,
            )

        assertEquals(
            "which of the six carry a vibrancy encoding at all",
            EXPECTED_ENCODED_ROLES,
            roles.filterValues { it.containsVibrant() }.keys,
        )

        // No role may be refused for a VIBRANCY reason any more. That was the bug.
        val refusedForVibrancy =
            roles
                .filterValues { role ->
                    val r = PolishContrast.ratio(role, Color.White)
                    r is PolishContrastResult.Undecidable && r.limit in VIBRANCY_LIMITS
                }
                .keys
        assertEquals(
            "no role whose channels survive the compositor may be refused for vibrancy",
            emptySet<String>(),
            refusedForVibrancy,
        )

        // `dividerLine` is still refused, and correctly: it is `SubBlack12`, 12.2 % alpha, and a
        // ratio against a translucent foreground with no stated backdrop is undefined. That is the
        // translucency rule doing its job, not the vibrancy bug coming back — which is exactly the
        // distinction this test exists to keep. Composite it first and it measures.
        val dividerDirect = PolishContrast.ratio(scheme.dividerLine, Color.White)
        assertEquals(
            PolishContrastResult.Undecidable(PolishContrastLimit.TranslucentForeground),
            dividerDirect,
        )
        assertTrue(
            "compositing the divider over a stated backdrop must produce a measurement",
            PolishContrast.ratioOver(scheme.dividerLine, Color.White, Color.White)
                is PolishContrastResult.Measured,
        )
    }

    @Test
    fun theVibrancyDiscriminatorActuallyDiscriminates() {
        // Guards the assertion above: if `containsVibrant()` answered the same for everything, the
        // test that matters would be vacuous.
        assertTrue(Color.Black.withVibrant(Vibrant.Darker).containsVibrant())
        assertFalse(PolishTokens.OpaqueGround.containsVibrant())
    }

    @Test
    fun aTranslucentBackgroundIsRefusedRatherThanMeasured() {
        val result = PolishContrast.ratio(Color.Black, Color.White.copy(alpha = 0.4f))
        assertEquals(
            PolishContrastResult.Undecidable(PolishContrastLimit.TranslucentBackground),
            result,
        )
    }

    @Test
    fun aTranslucentForegroundIsRefusedRatherThanMeasured() {
        val result = PolishContrast.ratio(Color.Black.copy(alpha = 0.65f), Color.White)
        assertEquals(
            PolishContrastResult.Undecidable(PolishContrastLimit.TranslucentForeground),
            result,
        )
    }

    @Test
    fun ratioOverCompositesBothLayersBeforeMeasuring() {
        // 65 % black label on a 15 % white fill over a white backdrop. Both layers are translucent,
        // so the direct call refuses and the composited call answers.
        val label = Color.Black.copy(alpha = 0.65f)
        val fill = Color.White.copy(alpha = 0.15f)
        assertTrue(PolishContrast.ratio(label, fill) is PolishContrastResult.Undecidable)

        val over = PolishContrast.ratioOver(label, fill, Color.White)
        assertTrue(over is PolishContrastResult.Measured)
        val measured = over as PolishContrastResult.Measured

        // Independent hand computation of the same stack.
        val surface = PolishContrast.composite(fill, Color.White)
        val text = PolishContrast.composite(label, surface)
        val expected = PolishContrast.ratio(text, surface) as PolishContrastResult.Measured
        assertEquals(expected.ratio.toDouble(), measured.ratio.toDouble(), 1e-3)
    }

    @Test
    fun compositeMatchesSourceOver() {
        val out = PolishContrast.composite(Color.Black.copy(alpha = 0.5f), Color.White)
        // One 8-bit step. A Compose `Color` in the sRGB space is packed with 8 bits per channel, so
        // a computed 0.5 reads back as 127/255 = 0.498039 — the arithmetic is exact and the storage
        // is not. Every comparison of a composited colour in this file carries the same tolerance.
        assertEquals(0.5, out.red.toDouble(), EightBitStep)
        assertEquals(1f, out.alpha)
        assertEquals(Color.Red.copy(alpha = 1f), PolishContrast.composite(Color.Red, Color.White))
        assertEquals(Color.White, PolishContrast.composite(Color.Red.copy(alpha = 0f), Color.White))
    }

    // ---- The glass budget ------------------------------------------------------------------------

    @Test
    fun aSurfaceWithinTheBudgetTakesItsMaterial() {
        var fill: PolishFill? = null
        rule.setContent {
            PolishTheme {
                fill =
                    PolishSurfaceDefaults.fillFor(
                        kind = PolishSurfaceKind.Card,
                        options = PolishOptions(),
                        interactive = false,
                        glassDepth = PolishTokens.MaxGlassDepth - 1,
                    )
            }
        }
        rule.runOnIdle { assertTrue("expected glass, got $fill", fill is PolishFill.Glass) }
    }

    @Test
    fun aSurfaceOverTheBudgetDrawsItsFillInstead() {
        var fill: PolishFill? = null
        rule.setContent {
            PolishTheme {
                fill =
                    PolishSurfaceDefaults.fillFor(
                        kind = PolishSurfaceKind.Card,
                        options = PolishOptions(),
                        interactive = false,
                        glassDepth = PolishTokens.MaxGlassDepth,
                    )
            }
        }
        rule.runOnIdle { assertTrue("expected a drawn fill, got $fill", fill is PolishFill.Solid) }
    }

    @Test
    fun theBudgetCountsTheWindowsOwnMaterial() {
        assertEquals(1, PolishWindowSurface.SystemGlass.baseGlassDepth)
        assertEquals(1, PolishWindowSurface.AppGlass.baseGlassDepth)
        assertEquals(0, PolishWindowSurface.AppOpaque.baseGlassDepth)
    }

    @Test
    fun aThemeSeedsTheDepthFromTheWindowItWasToldAbout() {
        var systemGlass = -1
        var appOpaque = -1
        var reduced = -1
        rule.setContent {
            PolishTheme(windowSurface = PolishWindowSurface.SystemGlass) {
                systemGlass = LocalPolishGlassDepth.current
            }
            PolishTheme(windowSurface = PolishWindowSurface.AppOpaque) {
                appOpaque = LocalPolishGlassDepth.current
            }
            PolishTheme(options = PolishOptions(reduceTransparency = true)) {
                reduced = LocalPolishGlassDepth.current
            }
        }
        rule.runOnIdle {
            assertEquals(1, systemGlass)
            assertEquals(0, appOpaque)
            // Reduced transparency spends nothing: every app surface goes opaque anyway.
            assertEquals(0, reduced)
        }
    }

    @Test
    fun aNestedSurfaceIsToldItsParentSpentTheBudget() {
        var outer = -1
        var inner = -1
        rule.setContent {
            PolishTheme(windowSurface = PolishWindowSurface.AppOpaque) {
                PolishSurface(kind = PolishSurfaceKind.Panel) {
                    outer = LocalPolishGlassDepth.current
                    PolishSurface(kind = PolishSurfaceKind.Card) {
                        inner = LocalPolishGlassDepth.current
                        Text("x")
                    }
                }
            }
        }
        rule.runOnIdle {
            // The opaque window spends 0; the panel takes a material and spends 1; the card sees 1.
            assertEquals(1, outer)
            assertEquals(2, inner)
        }
    }

    @Test
    fun aDrawnSurfaceDoesNotSpendTheBudget() {
        var inner = -1
        rule.setContent {
            PolishTheme(options = PolishOptions(reduceTransparency = true)) {
                PolishSurface(kind = PolishSurfaceKind.Panel) {
                    PolishSurface(kind = PolishSurfaceKind.Card) {
                        inner = LocalPolishGlassDepth.current
                        Text("x")
                    }
                }
            }
        }
        // Nothing took a material, so the depth never moved off the theme's base.
        rule.runOnIdle { assertEquals(0, inner) }
    }

    @Test
    fun anInteractiveSurfaceStillNeverTakesAMaterialWhateverTheDepth() {
        var atZero: PolishFill? = null
        rule.setContent {
            PolishTheme {
                atZero =
                    PolishSurfaceDefaults.fillFor(
                        kind = PolishSurfaceKind.Card,
                        options = PolishOptions(),
                        interactive = true,
                        glassDepth = 0,
                    )
            }
        }
        rule.runOnIdle { assertTrue(atZero is PolishFill.Solid) }
    }

    @Test
    fun aStaticSurfaceThatGaveUpItsMaterialDrawsAnEdge() {
        // Without this it is a 4 %-black wash over its parent and reads as a smudge, not a surface.
        var overBudget: PolishEdge? = null
        var withinBudget: PolishEdge? = null
        rule.setContent {
            PolishTheme {
                val options = PolishOptions()
                overBudget =
                    PolishSurfaceDefaults.edgeFor(
                        kind = PolishSurfaceKind.Card,
                        options = options,
                        selected = false,
                        tookGlass = false,
                        interactive = false,
                    )
                withinBudget =
                    PolishSurfaceDefaults.edgeFor(
                        kind = PolishSurfaceKind.Card,
                        options = options,
                        selected = false,
                        tookGlass = true,
                        interactive = false,
                    )
            }
        }
        rule.runOnIdle {
            assertNotNull("a surface that dropped its material needs an edge", overBudget)
            assertNull("a surface that kept its material does not", withinBudget)
        }
    }

    @Test
    fun anInteractiveDrawnSurfaceDoesNotGainAnExtraEdge() {
        // Its feedback is the compositor hover highlight on the drawn fill; an edge on every row
        // would be noise.
        var edge: PolishEdge? = null
        rule.setContent {
            PolishTheme {
                edge =
                    PolishSurfaceDefaults.edgeFor(
                        kind = PolishSurfaceKind.Card,
                        options = PolishOptions(),
                        selected = false,
                        tookGlass = false,
                        interactive = true,
                    )
            }
        }
        rule.runOnIdle { assertNull(edge) }
    }

    // ---- Error and focus -------------------------------------------------------------------------

    @Test
    fun aSurfaceInErrorReportsTheErrorToAccessibility() {
        rule.setContent {
            PolishTheme {
                PolishSurface(
                    modifier = Modifier.testTag(TAG),
                    errorMessage = "Pick a date before saving",
                ) {
                    Text("Schedule")
                }
            }
        }
        rule.onNodeWithTag(TAG).assert(
            SemanticsMatcher.expectValue(SemanticsProperties.Error, "Pick a date before saving")
        )
    }

    @Test
    fun errorOutranksSelection() {
        var errorEdge: PolishEdge? = null
        var selectedEdge: PolishEdge? = null
        var errorScheme = Color.Unspecified
        var accent = Color.Unspecified
        rule.setContent {
            PolishTheme {
                val options = PolishOptions()
                errorScheme = PicoTheme.colorScheme.error
                accent = PicoTheme.colorScheme.interaction
                errorEdge =
                    PolishSurfaceDefaults.edgeFor(
                        kind = PolishSurfaceKind.Card,
                        options = options,
                        selected = true,
                        isError = true,
                    )
                selectedEdge =
                    PolishSurfaceDefaults.edgeFor(
                        kind = PolishSurfaceKind.Card,
                        options = options,
                        selected = true,
                        isError = false,
                    )
            }
        }
        rule.runOnIdle {
            assertEquals(errorScheme, errorEdge!!.color)
            assertEquals(accent, selectedEdge!!.color)
        }
    }

    @Test
    fun theFocusRingIsThinnerThanTheSelectionEdge() {
        // Same accent, two weights: one accent per screen rather than a second hue nobody was taught.
        var focus: PolishEdge? = null
        var selected: PolishEdge? = null
        rule.setContent {
            PolishTheme {
                val options = PolishOptions()
                focus =
                    PolishSurfaceDefaults.edgeFor(
                        kind = PolishSurfaceKind.Card,
                        options = options,
                        selected = false,
                        focused = true,
                    )
                selected =
                    PolishSurfaceDefaults.edgeFor(
                        kind = PolishSurfaceKind.Card,
                        options = options,
                        selected = true,
                    )
            }
        }
        rule.runOnIdle {
            assertNotNull(focus)
            assertEquals(selected!!.color, focus!!.color)
            assertTrue(selected!!.width > focus!!.width)
        }
    }

    // ---- Supporting text ---------------------------------------------------------------------

    @Test
    fun supportingTextTakesTheSecondLabelRungNotTheThird() {
        // `labelTertiary` measured 3.18 / 3.26 / 3.49 : 1 on the Panel, Card and Control surfaces of
        // an admitted frame — below 4.5:1 for 12 sp normal text. The JVM cannot reproduce those
        // numbers (the fallback scheme's literals compute fine), so this test pins the DECISION the
        // frame forced, not the measurement.
        var supporting: Color? = null
        var labelSecondary: Color? = null
        var labelTertiary: Color? = null
        rule.setContent {
            PolishTheme {
                labelSecondary = PicoTheme.colorScheme.labelSecondary
                labelTertiary = PicoTheme.colorScheme.labelTertiary
                supporting =
                    PolishSurfaceDefaults.secondaryForegroundFor(
                        PolishSurfaceKind.Card,
                        PolishOptions(),
                    )
            }
        }
        rule.runOnIdle {
            assertEquals(labelSecondary, supporting)
            assertNotEquals(labelTertiary, supporting)
        }
    }

    @Test
    fun increasedContrastTakesSupportingTextAllTheWayToThePrimaryLabel() {
        var supporting: Color? = null
        var primary: Color? = null
        rule.setContent {
            PolishTheme(options = PolishOptions(increaseContrast = true)) {
                primary = PicoTheme.colorScheme.labelPrimary
                supporting =
                    PolishSurfaceDefaults.secondaryForegroundFor(
                        PolishSurfaceKind.Card,
                        PolishOptions(increaseContrast = true),
                    )
            }
        }
        rule.runOnIdle { assertEquals(primary, supporting) }
    }

    @Test
    fun theSupportingRungIsStrictlyDarkerInTheFallbackScheme() {
        // Guards the direction of the change on the one platform where the values are readable.
        var secondaryLum = -1f
        var tertiaryLum = -1f
        rule.setContent {
            PolishTheme {
                val white = Color.White
                secondaryLum =
                    PolishContrast.relativeLuminance(
                        PolishContrast.composite(PicoTheme.colorScheme.labelSecondary, white)
                    )
                tertiaryLum =
                    PolishContrast.relativeLuminance(
                        PolishContrast.composite(PicoTheme.colorScheme.labelTertiary, white)
                    )
            }
        }
        rule.runOnIdle {
            assertTrue(
                "labelSecondary ($secondaryLum) must be darker than labelTertiary ($tertiaryLum)",
                secondaryLum < tertiaryLum,
            )
        }
    }

    // ---- Motion ----------------------------------------------------------------------------------

    @Test
    fun theKitAnimatesOnThePlatformCurveRatherThanComposesDefault() {
        val spec = PolishTokens.motion<Float>(reduceMotion = false)
        assertTrue(spec is TweenSpec)
        assertEquals(MotionTokens.bezierEasingStandard, (spec as TweenSpec).easing)
    }

    @Test
    fun theKitsDurationsAreTheSdksDurations() {
        assertEquals(MotionTokens.durationShort1, PolishTokens.DurationFeedbackMillis)
        assertEquals(MotionTokens.durationShort2, PolishTokens.DurationStateMillis)
        assertEquals(MotionTokens.durationShort3, PolishTokens.DurationPanelMillis)
    }

    @Test
    fun reducedMotionStillCollapsesEveryHelper() {
        assertTrue(PolishTokens.motion<Float>(reduceMotion = true) is SnapSpec)
        assertTrue(PolishTokens.motionSpec<Float>(reduceMotion = true) is SnapSpec)
        assertTrue(PolishTokens.spring<Float>(reduceMotion = true) is SnapSpec)
    }

    @Test
    fun aSpringTokenThatReadsBackAsNaNIsReplacedRatherThanUsed() {
        // Durations fall back to their documented value; springs fall back to (NaN, NaN), and a NaN
        // spring does not animate — it produces an undefined value for the whole run.
        val spec = PolishTokens.spring<Float>(reduceMotion = false, args = SpringEasingArgs.Unspecific)
        assertTrue(spec is SpringSpec)
        val spring = spec as SpringSpec
        assertFalse("damping ratio is NaN", spring.dampingRatio.isNaN())
        assertFalse("stiffness is NaN", spring.stiffness.isNaN())
    }

    @Test
    fun aValidSpringTokenIsUsedUnchanged() {
        val args = SpringEasingArgs(dampingRatio = 0.75f, stiffness = 150f)
        val spec = PolishTokens.spring<Float>(reduceMotion = false, args = args) as SpringSpec
        assertEquals(0.75f, spec.dampingRatio)
        assertEquals(150f, spec.stiffness)
    }

    // ---- Anchors -----------------------------------------------------------------------------------

    @Test
    fun theTargetSizeIsTheFrameworksMinimumTouchTarget() {
        assertEquals(48.dp, PolishTokens.MinTargetSize)
    }

    @Test
    fun theSpacingScaleKeepsTheSdksSteps() {
        // 8 / 12 / 16 / 24 / 32 are DimensionTokens Gap{Small,Medium,Large} and
        // Padding{Small,Regular,Medium,Large}. 4 dp is the kit's own and has no SDK anchor.
        assertEquals(4.dp, PolishTokens.SpaceTiny)
        assertEquals(8.dp, PolishTokens.SpaceSmall)
        assertEquals(12.dp, PolishTokens.SpaceSnug)
        assertEquals(16.dp, PolishTokens.SpaceMedium)
        assertEquals(24.dp, PolishTokens.SpaceLarge)
        assertEquals(32.dp, PolishTokens.SpaceHuge)
    }

    @Test
    fun theRadiusScaleLandsOnTheSdksLadder() {
        assertEquals(32.dp, PolishTokens.RadiusCompanion)
        assertEquals(24.dp, PolishTokens.RadiusPanel)
        assertEquals(16.dp, PolishTokens.RadiusCard)
        assertEquals(12.dp, PolishTokens.RadiusControl)
    }

    // ---------------------------------------------------------------------------------------
    // The derived light-secondary tone. Nine checks, because the defect it fixes was invisible to
    // eight passing contrast ratios: `contrast-design-015-rt-on` reported all four surfaces, body
    // and supporting alike, at an identical 14.74:1, which is what a COLLAPSED tier looks like when
    // you only ask whether each pairing passes.
    // ---------------------------------------------------------------------------------------

    @Test
    fun designSystem_lightSecondaryIsAVisiblyDistinctToneFromTheLightPrimary() {
        val primary = vibrantColorScheme().labelPrimaryLight
        val secondary = PolishSurfaceDefaults.lightSecondaryForeground(primary)

        assertNotEquals("the derived tone must not be the role it steps down from", primary, secondary)
        // "Visibly distinct" is not "not equal": one 8-bit step is not equal either. Require a step
        // of at least a tenth of the full range, which at 0.70 over the kit's ground is ~0.25.
        val step = primary.red - secondary.red
        assertTrue(
            "channel step $step should be a visible one, not a rounding difference",
            step > 0.10f,
        )
        // It steps DOWN. A lighter-than-white secondary is not possible, but a sign error would
        // otherwise pass every other assertion here.
        assertTrue("the secondary must be darker than the primary on a dark ground", step > 0f)
    }

    @Test
    fun designSystem_lightSecondaryComposesToTheExpectedToneOverTheOpaqueGround() {
        val primary = vibrantColorScheme().labelPrimaryLight
        val secondary = PolishSurfaceDefaults.lightSecondaryForeground(primary)
        val ground = PolishTokens.OpaqueGround

        // 0.70 of the way from the ground to the label, computed independently of the implementation.
        val expected = ground.red + (primary.red - ground.red) * PolishSurfaceDefaults.LightSecondaryStep
        assertEquals(
            "composed channel",
            expected.toDouble(),
            secondary.red.toDouble(),
            EightBitStep,
        )
        // Neutral in, neutral out: a tint here would be a bug, not a style.
        assertEquals("stays neutral (r=g)", secondary.red.toDouble(), secondary.green.toDouble(), EightBitStep)
        assertEquals("stays neutral (g=b)", secondary.green.toDouble(), secondary.blue.toDouble(), EightBitStep)
    }

    @Test
    fun designSystem_lightSecondaryMeetsNormalTextContrastAndIsMeasurableAtAll() {
        val primary = vibrantColorScheme().labelPrimaryLight
        val secondary = PolishSurfaceDefaults.lightSecondaryForeground(primary)

        // Opaque by construction. This is the reason it is composed rather than given an alpha:
        // `PolishContrast.ratio` refuses a translucent foreground, so an alpha-based tone would be
        // undecidable by this kit's own API and could never be held to a target.
        assertEquals("the derived tone must be opaque", 1f, secondary.alpha)

        val result = PolishContrast.ratio(secondary, PolishTokens.OpaqueGround)
        assertTrue(
            "the derived tone must be measurable, not $result",
            result is PolishContrastResult.Measured,
        )
        val measured = result as PolishContrastResult.Measured
        assertTrue(
            "supporting text is normal text: ${measured.ratio} must clear ${PolishContrast.NormalText}",
            measured.ratio >= PolishContrast.NormalText,
        )
        // And the primary it steps down from must still clear it too — the point is a second tier,
        // not a trade of one unreadable tier for another.
        val primaryResult = PolishContrast.ratio(primary, PolishTokens.OpaqueGround)
        assertTrue(primaryResult is PolishContrastResult.Measured)
        assertTrue(
            "the primary must remain readable",
            (primaryResult as PolishContrastResult.Measured).ratio >= PolishContrast.NormalText,
        )
    }

    @Test
    fun designSystem_lightSecondaryKeepsTheVibrancyTagOfTheRoleItCameFrom() {
        // The trap: the level lives in bits 24-31, which a fresh `Color(r, g, b, a)` zeroes. Dropping
        // `Vibrant.None` (31, "terminate the effect") to `Vibrant.Unspecified` (0, "inherit it")
        // changes behaviour inside a vibrant context, and nothing on a JVM frame would show it.
        val tagged = Color.White.withVibrant(Vibrant.None)
        val derived = PolishSurfaceDefaults.lightSecondaryForeground(tagged)
        assertTrue("the tag must survive the arithmetic", derived.containsVibrant())
        assertEquals(Vibrant.None, derived.obtainVibrant())

        // And an untagged source must stay untagged: off the spatial platform the scheme falls back
        // to literals, and inventing a level there would change what the compositor is asked for.
        val plain = Color.White
        val fromPlain = PolishSurfaceDefaults.lightSecondaryForeground(plain)
        assertEquals(plain.obtainVibrant(), fromPlain.obtainVibrant())
    }

    @Test
    fun designSystem_everySurfaceKeepsASecondTierUnderReducedTransparency() {
        // The regression this whole change exists for. `usesLightForeground` is
        // `reduceTransparency || kind == Emphasis`, so with the setting on it is EVERY kind.
        val primaries = mutableMapOf<PolishSurfaceKind, Color>()
        val secondaries = mutableMapOf<PolishSurfaceKind, Color>()
        rule.setContent {
            PolishTheme(options = PolishOptions(reduceTransparency = true)) {
                PolishSurfaceKind.values().forEach { kind ->
                    primaries[kind] =
                        PolishSurfaceDefaults.foregroundFor(kind, LocalPolishOptions.current)
                    secondaries[kind] =
                        PolishSurfaceDefaults.secondaryForegroundFor(kind, LocalPolishOptions.current)
                }
            }
        }
        rule.runOnIdle {
            PolishSurfaceKind.values().forEach { kind ->
                assertNotEquals(
                    "$kind lost its second text tier under reduceTransparency",
                    primaries[kind],
                    secondaries[kind],
                )
            }
        }
    }

    @Test
    fun designSystem_emphasisKeepsASecondTierUnderTheDefaultSettings() {
        var primary: Color? = null
        var secondary: Color? = null
        rule.setContent {
            PolishTheme {
                primary =
                    PolishSurfaceDefaults.foregroundFor(
                        PolishSurfaceKind.Emphasis,
                        LocalPolishOptions.current,
                    )
                secondary =
                    PolishSurfaceDefaults.secondaryForegroundFor(
                        PolishSurfaceKind.Emphasis,
                        LocalPolishOptions.current,
                    )
            }
        }
        rule.runOnIdle {
            assertNotNull(primary)
            assertNotEquals("Emphasis lost its second text tier", primary, secondary)
        }
    }

    @Test
    fun designSystem_increaseContrastStepsTheLightSecondaryBackUpToTheFullLabel() {
        // Symmetry with the dark-foreground branch, which steps up to `labelPrimary`. At that point
        // rank is carried by size and weight alone, which is what the setting is for.
        var primary: Color? = null
        var secondary: Color? = null
        rule.setContent {
            PolishTheme(
                options = PolishOptions(reduceTransparency = true, increaseContrast = true),
            ) {
                primary =
                    PolishSurfaceDefaults.foregroundFor(
                        PolishSurfaceKind.Panel,
                        LocalPolishOptions.current,
                    )
                secondary =
                    PolishSurfaceDefaults.secondaryForegroundFor(
                        PolishSurfaceKind.Panel,
                        LocalPolishOptions.current,
                    )
            }
        }
        rule.runOnIdle { assertEquals(primary, secondary) }
    }

    @Test
    fun designSystem_primaryForegroundIsUnchangedByTheSecondaryRefinement() {
        // The roles are read from the scheme THIS composition provides, not from
        // `vibrantColorScheme()`. Off the spatial platform `PicoTheme` falls back through
        // `adaptVibrantColor` to `defaultColorScheme()`, whose roles are literal ARGB with no
        // vibrancy tag — comparing against the vibrant scheme here fails for a reason that has
        // nothing to do with what is being tested.
        val results = mutableMapOf<String, Color>()
        rule.setContent {
            PolishTheme {
                results["labelPrimaryLight"] = PicoTheme.colorScheme.labelPrimaryLight
                results["labelPrimary"] = PicoTheme.colorScheme.labelPrimary
                results["emphasis"] =
                    PolishSurfaceDefaults.foregroundFor(
                        PolishSurfaceKind.Emphasis,
                        LocalPolishOptions.current,
                    )
                results["panel"] =
                    PolishSurfaceDefaults.foregroundFor(
                        PolishSurfaceKind.Panel,
                        LocalPolishOptions.current,
                    )
            }
        }
        rule.runOnIdle {
            assertEquals(results["labelPrimaryLight"], results["emphasis"])
            assertEquals(results["labelPrimary"], results["panel"])
        }
    }

    @Test
    fun designSystem_darkForegroundSurfacesKeepTheirExistingSecondaryRoles() {
        var plain: Color? = null
        var contrasted: Color? = null
        var labelSecondary: Color? = null
        var labelPrimary: Color? = null
        rule.setContent {
            PolishTheme {
                labelSecondary = PicoTheme.colorScheme.labelSecondary
                labelPrimary = PicoTheme.colorScheme.labelPrimary
                plain =
                    PolishSurfaceDefaults.secondaryForegroundFor(
                        PolishSurfaceKind.Panel,
                        LocalPolishOptions.current,
                    )
            }
            PolishTheme(options = PolishOptions(increaseContrast = true)) {
                contrasted =
                    PolishSurfaceDefaults.secondaryForegroundFor(
                        PolishSurfaceKind.Panel,
                        LocalPolishOptions.current,
                    )
            }
        }
        rule.runOnIdle {
            assertEquals(labelSecondary, plain)
            assertEquals(labelPrimary, contrasted)
        }
    }

    @Test
    fun designSystem_lightForegroundAlwaysSitsOnTheOpaqueGround() {
        // The invariant the composed tone depends on. If a future fill breaks it, the composed value
        // would be measured against a ground it is not drawn on, and this fails instead of a frame.
        rule.setContent {
            PolishTheme {
                PolishSurfaceKind.values().forEach { kind ->
                    listOf(
                        PolishOptions(),
                        PolishOptions(reduceTransparency = true),
                        PolishOptions(increaseContrast = true),
                        PolishOptions(reduceTransparency = true, increaseContrast = true),
                    ).forEach { options ->
                        if (PolishSurfaceDefaults.usesLightForeground(kind, options)) {
                            listOf(false, true).forEach { interactive ->
                                listOf(0, 1, 2, 3).forEach { depth ->
                                    val fill =
                                        PolishSurfaceDefaults.fillFor(
                                            kind = kind,
                                            options = options,
                                            interactive = interactive,
                                            glassDepth = depth,
                                        )
                                    assertEquals(
                                        "$kind / $options / interactive=$interactive / depth=$depth " +
                                            "uses the light foreground, so its fill must be the opaque ground",
                                        PolishFill.Solid(PolishTokens.OpaqueGround),
                                        fill,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        rule.runOnIdle {}
    }

    private companion object {
        const val TAG = "polish-design-target"

        /** One 8-bit channel step: the quantisation an sRGB Compose `Color` round-trips through. */
        const val EightBitStep = 1.0 / 255.0

        /**
         * Which of the six "literal" roles actually carry a vibrancy encoding in the vibrant scheme.
         *
         * Filled in from what the SDK reports, not from what the kit's documentation assumed. See
         * [theSixSupposedlyLiteralRolesAreActuallyReadable].
         */
        val EXPECTED_ENCODED_ROLES: Set<String> = setOf("labelPrimaryLight", "dividerLine")

        /** The refusals that mean "the compositor owns this colour", as opposed to alpha refusals. */
        val VIBRANCY_LIMITS =
            setOf(
                PolishContrastLimit.VibrantForeground,
                PolishContrastLimit.VibrantBackground,
                PolishContrastLimit.VibrantBackdrop,
            )
    }
}
