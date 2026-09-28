package dev.local.picopolish

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.animation.core.SnapSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.pico.spatial.ui.design.LocalContentColor
import com.pico.spatial.ui.design.PicoTheme
import com.pico.spatial.ui.design.Text
import com.pico.spatial.ui.foundation.vibrant.Vibrant
import com.pico.spatial.ui.foundation.vibrant.containsVibrant
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
 * Behaviour tests for the polish kit.
 *
 * Scope and honesty: these run on the JVM under Robolectric, where `SpatialBuild.isSpatialPlatform()`
 * is false. That has two consequences worth stating rather than hiding.
 *
 *  1. The colour scheme resolves through the **non-adaptive fallback** (`defaultColorScheme()`), so
 *     the alpha assertions below describe that fallback, not the Vibrant-encoded values a headset
 *     renders. They are still the right regression: they prove the kit picks a role the scheme
 *     itself treats as opaque, and they would fail immediately if someone swapped in `fillSecondary`.
 *  2. `backgroundMaterial` and `spatialHoverEffect` no-op here, so **no test in this file claims
 *     anything about how glass looks**. Appearance is a rendered-frame question; see the skill's
 *     acceptance reference.
 *
 * What these tests do own: activation and suppression, semantics, foreground provision, the option
 * fallback, and the full pairwise option matrix.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PolishBehaviourTest {

    @get:Rule val rule = createComposeRule()

    // ---- Option resolution (pure) ---------------------------------------------------------------

    @Test
    fun enhancedFallsBackToNativeWhenUnsupported() {
        val requested = PolishOptions(quality = PolishQuality.Enhanced)
        val resolved = requested.resolve(PolishCapabilities(enhancedSupported = false))
        assertEquals(PolishQuality.Native, resolved.quality)
    }

    @Test
    fun enhancedSurvivesWhenSupported() {
        val requested = PolishOptions(quality = PolishQuality.Enhanced)
        val resolved = requested.resolve(PolishCapabilities(enhancedSupported = true))
        assertEquals(PolishQuality.Enhanced, resolved.quality)
    }

    @Test
    fun capabilityFallbackNeverAltersAccessibilityFlags() {
        val requested =
            PolishOptions(
                quality = PolishQuality.Enhanced,
                reduceMotion = true,
                reduceTransparency = true,
                increaseContrast = true,
            )
        val resolved = requested.resolve(PolishCapabilities(enhancedSupported = false))
        assertEquals(PolishQuality.Native, resolved.quality)
        assertTrue(resolved.reduceMotion)
        assertTrue(resolved.reduceTransparency)
        assertTrue(resolved.increaseContrast)
    }

    @Test
    fun reducedMotionCollapsesTheKitsAnimationDuration() {
        // Assert the direction, not merely that the two specs differ: an earlier version compared
        // the classes, which stayed true with the mapping inverted — the one guard on the kit's
        // motion contract was blind to the failure it existed to catch.
        val reduced = PolishTokens.motion<Float>(reduceMotion = true)
        assertTrue("reduced motion must be instantaneous, was $reduced", reduced is SnapSpec)
        assertEquals(0, (reduced as SnapSpec).delay)

        val normal = PolishTokens.motion<Float>(reduceMotion = false)
        assertTrue("unreduced motion must animate, was $normal", normal is TweenSpec)
        assertEquals(PolishTokens.DurationStateMillis, (normal as TweenSpec).durationMillis)
        assertEquals(
            PolishTokens.DurationPanelMillis,
            (PolishTokens.motion<Float>(false, PolishTokens.DurationPanelMillis) as TweenSpec).durationMillis,
        )
    }

    @Test
    fun percentLabelSpansTheRange() {
        assertEquals("0%", formatPercent(0f, 0f..1f))
        assertEquals("50%", formatPercent(0.5f, 0f..1f))
        assertEquals("100%", formatPercent(1f, 0f..1f))
        assertEquals("50%", formatPercent(15f, 10f..20f))
        assertEquals("0%", formatPercent(-4f, 0f..1f)) // clamped, never negative
    }

    // ---- Activation and suppression --------------------------------------------------------------

    @Test
    fun enabledSurfaceActivates() {
        var clicks = 0
        rule.setContent {
            PolishTheme {
                PolishSurface(modifier = Modifier.testTag(TAG), onClick = { clicks++ }) {
                    Text("card")
                }
            }
        }
        rule.onNodeWithTag(TAG).performClick()
        rule.runOnIdle { assertEquals(1, clicks) }
    }

    @Test
    fun disabledSurfaceDoesNotActivate() {
        var clicks = 0
        rule.setContent {
            PolishTheme {
                PolishSurface(
                    modifier = Modifier.testTag(TAG),
                    onClick = { clicks++ },
                    enabled = false,
                ) {
                    Text("card")
                }
            }
        }
        rule.onNodeWithTag(TAG).performClick()
        rule.runOnIdle { assertEquals(0, clicks) }
    }

    @Test
    fun enabledButtonActivates() {
        var clicks = 0
        rule.setContent {
            PolishTheme { PolishButton(onClick = { clicks++ }, modifier = Modifier.testTag(TAG)) { Text("go") } }
        }
        rule.onNodeWithTag(TAG).performClick()
        rule.runOnIdle { assertEquals(1, clicks) }
    }

    @Test
    fun disabledButtonDoesNotActivate() {
        var clicks = 0
        rule.setContent {
            PolishTheme {
                PolishButton(onClick = { clicks++ }, modifier = Modifier.testTag(TAG), enabled = false) {
                    Text("go")
                }
            }
        }
        rule.onNodeWithTag(TAG).performClick()
        rule.runOnIdle { assertEquals(0, clicks) }
    }

    @Test
    fun loadingButtonSuppressesActivation() {
        var clicks = 0
        rule.setContent {
            PolishTheme {
                PolishButton(onClick = { clicks++ }, modifier = Modifier.testTag(TAG), loading = true) {
                    Text("go")
                }
            }
        }
        rule.onNodeWithTag(TAG).performClick()
        rule.runOnIdle { assertEquals(0, clicks) }
    }

    @Test
    fun loadingButtonStaysVisuallyEnabled() {
        // Routing `loading` through the built-in's `enabled` flag was wrong: `enabled = false` is
        // the SDK's DISABLED PRESENTATION, and it multiplies the whole node — spinner included — by
        // LocalDisableAlpha. A busy button then rendered pixel-identical to a disabled one.
        rule.setContent {
            PolishTheme {
                PolishButton(onClick = {}, modifier = Modifier.testTag(TAG), loading = true) {
                    Text("go")
                }
            }
        }
        rule.onNodeWithTag(TAG).assertIsEnabled()
    }

    @Test
    fun disabledButtonIsReportedDisabled() {
        rule.setContent {
            PolishTheme {
                PolishButton(onClick = {}, modifier = Modifier.testTag(TAG), enabled = false) {
                    Text("go")
                }
            }
        }
        rule.onNodeWithTag(TAG).assertIsNotEnabled()
    }

    @Test
    fun disabledSwitchKeepsItsRoleAndState() {
        // A null onCheckedChange makes the built-in skip its whole toggleable modifier, taking the
        // role and the toggle state out of the accessibility tree with it.
        rule.setContent {
            PolishTheme {
                PolishSurface {
                    PolishSwitchRow(
                        title = "Auto-save",
                        checked = true,
                        onCheckedChange = {},
                        enabled = false,
                    )
                }
            }
        }
        rule
            .onNode(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch), useUnmergedTree = true)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.ToggleableState, ToggleableState.On))
    }

    @Test
    fun toggleIconButtonReportsItsToggleState() {
        rule.setContent {
            PolishTheme {
                PolishToggleIconButton(
                    checked = true,
                    onCheckedChange = {},
                    contentDescription = "Settings",
                    modifier = Modifier.testTag(TAG),
                ) {
                    Text("S")
                }
            }
        }
        rule
            .onNodeWithTag(TAG, useUnmergedTree = true)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.ToggleableState, ToggleableState.On))
    }

    @Test
    fun controlsInheritTheEnclosingSurfaceKind() {
        // Every control defaulted `kind` to Card, so dropping one into an Emphasis surface — the
        // natural call, since kind is optional — derived the dark foreground over the dark fill.
        var insideEmphasis: Color? = null
        var insideCard: Color? = null
        var labelPrimaryLight: Color? = null
        rule.setContent {
            PolishTheme {
                labelPrimaryLight = PicoTheme.colorScheme.labelPrimaryLight
                PolishSurface(kind = PolishSurfaceKind.Emphasis) {
                    insideEmphasis =
                        PolishSurfaceDefaults.secondaryForegroundFor(
                            LocalPolishSurfaceKind.current,
                            LocalPolishOptions.current,
                        )
                }
                PolishSurface(kind = PolishSurfaceKind.Card) {
                    insideCard =
                        PolishSurfaceDefaults.secondaryForegroundFor(
                            LocalPolishSurfaceKind.current,
                            LocalPolishOptions.current,
                        )
                }
            }
        }
        rule.runOnIdle {
            // This used to assert `insideEmphasis == labelPrimaryLight`. That was only ever a PROXY
            // for "it took the light branch", and the proxy held solely because the light branch
            // returned the primary role verbatim — which is the collapsed second tier that
            // `PolishSurfaceDefaults.lightSecondaryForeground` now fixes. The intent of this test is
            // unchanged and still checked: a control inside Emphasis derives the LIGHT pairing, and a
            // control inside a Card does not.
            assertEquals(
                PolishSurfaceDefaults.lightSecondaryForeground(labelPrimaryLight!!),
                insideEmphasis,
            )
            assertNotNull(insideCard)
            assertFalse("a card must not take the light-on-dark pairing", insideCard == labelPrimaryLight)
            assertNotEquals("a card must not take the light secondary either", insideEmphasis, insideCard)
        }
    }

    @Test
    fun aNestedThemeKeepsTheAccentTheAppApplied() {
        // A nested PolishTheme used to re-resolve the scheme from scratch, dropping the accent for
        // exactly the content a comparison column exists to judge.
        val accent = Color(0xFFFF6B3D)
        var outer: Color? = null
        var nested: Color? = null
        rule.setContent {
            PolishTheme(accent = accent) {
                outer = PicoTheme.colorScheme.interaction
                PolishTheme(options = PolishOptions(quality = PolishQuality.Enhanced)) {
                    nested = PicoTheme.colorScheme.interaction
                }
            }
        }
        rule.runOnIdle {
            assertEquals(accent, outer)
            assertEquals(accent, nested)
        }
    }

    @Test
    fun aFieldStaysReadableOnTheOpaqueSurfaceTheKitOwns() {
        // `TextArea` ignores LocalContentColor and paints with `colors.textColor`, so the field
        // needs its own palette — judged by contrast, not by which role name reads well. The first
        // attempt paired the light shell with the light label and measured 4.06:1 on a rendered
        // frame, under the 4.5:1 target for normal text.
        //
        // Scope, stated rather than assumed: this asserts only the surfaces whose fill the kit
        // makes opaque, because only there is the composite known. On a glass surface the shell is
        // translucent over a compositor material, so a ratio computed from role values is
        // meaningless — that case is a rendered-frame question and its measurement lives with the
        // retained frames. Even here the numbers are the non-adaptive fallback scheme's; on a
        // headset these roles are Vibrant-encoded and the compositor resolves them.
        val measured = mutableListOf<Pair<String, Float>>()
        rule.setContent {
            listOf(
                "reduceTransparency" to PolishOptions(reduceTransparency = true),
                "reduceTransparency+contrast" to
                    PolishOptions(reduceTransparency = true, increaseContrast = true),
            ).forEach { (name, options) ->
                PolishTheme(options = options) {
                    val colors = PolishFieldDefaults.colorsFor(PolishSurfaceKind.Card, options)
                    val surfaceFill = PicoTheme.colorScheme.fillPrimary
                    val shell = composite(colors.backgroundColor, over = surfaceFill)
                    measured += name to contrastRatio(colors.textColor, shell)
                }
            }
        }
        rule.runOnIdle {
            assertTrue("expected both opaque cases to be measured", measured.size == 2)
            measured.forEach { (name, ratio) ->
                assertTrue(
                    "field text on its own shell must clear 4.5:1 for normal text; $name measured $ratio",
                    ratio >= 4.5f,
                )
            }
        }
    }

    @Test
    fun anErrorMessageStaysReadableOnAnOpaqueSurface() {
        // The error role carries the state on the field's border, where 3:1 governs. The message is
        // normal text at 4.5:1, and the same red measured 4.10:1 against the opaque fill.
        var ratio = 0f
        rule.setContent {
            val options = PolishOptions(reduceTransparency = true)
            PolishTheme(options = options) {
                ratio =
                    contrastRatio(
                        PolishFieldDefaults.errorMessageColor(PolishSurfaceKind.Card, options),
                        PicoTheme.colorScheme.fillPrimary,
                    )
            }
        }
        rule.runOnIdle {
            assertTrue("error message on an opaque surface measured $ratio", ratio >= 4.5f)
        }
    }

    @Test
    fun loadingButtonAnnouncesItsState() {
        rule.setContent {
            PolishTheme {
                PolishButton(
                    onClick = {},
                    modifier = Modifier.testTag(TAG),
                    loading = true,
                    loadingDescription = "Saving",
                ) {
                    Text("go")
                }
            }
        }
        rule
            .onNodeWithTag(TAG)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Saving"))
    }

    @Test
    fun switchRowAnnouncesOnAndOff() {
        rule.setContent {
            PolishTheme {
                PolishSurface {
                    PolishSwitchRow(
                        title = "Reduce motion",
                        checked = true,
                        onCheckedChange = {},
                        modifier = Modifier.testTag(TAG),
                        onDescription = "On",
                        offDescription = "Off",
                    )
                }
            }
        }
        rule
            .onNodeWithTag(TAG)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "On"))
    }

    // ---- Semantics --------------------------------------------------------------------------------

    @Test
    fun selectedSurfaceReportsSelection() {
        rule.setContent {
            PolishTheme {
                PolishSurface(modifier = Modifier.testTag(TAG), onClick = {}, selected = true) {
                    Text("card")
                }
            }
        }
        rule
            .onNodeWithTag(TAG)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Selected, true))
    }

    @Test
    fun staticSurfaceCarriesNoButtonRole() {
        rule.setContent {
            PolishTheme { PolishSurface(modifier = Modifier.testTag(TAG)) { Text("card") } }
        }
        rule
            .onNodeWithTag(TAG)
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Role).not())
    }

    @Test
    fun fieldInErrorStateReportsTheError() {
        rule.setContent {
            PolishTheme {
                PolishSurface {
                    PolishField(
                        label = "Name",
                        value = "",
                        onValueChange = {},
                        modifier = Modifier.testTag(TAG),
                        errorMessage = "Name is required",
                    )
                }
            }
        }
        // The error belongs to the input node, not to the label/input/helper group that carries the
        // caller's tag — that is what a screen reader focuses. Assert it lands exactly once there,
        // and that the message is also on screen, so the state is not announced-only.
        rule
            .onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.Error), useUnmergedTree = true)
            .assertCountEquals(1)
        rule.onNodeWithText("Name is required", useUnmergedTree = true).assertExists()
    }

    @Test
    fun fieldWithoutAnErrorReportsNone() {
        rule.setContent {
            PolishTheme {
                PolishSurface {
                    PolishField(
                        label = "Name",
                        value = "Ada",
                        onValueChange = {},
                        modifier = Modifier.testTag(TAG),
                        supportingText = "Shown on the shared list",
                    )
                }
            }
        }
        rule
            .onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.Error), useUnmergedTree = true)
            .assertCountEquals(0)
        rule.onNodeWithText("Shown on the shared list", useUnmergedTree = true).assertExists()
    }

    // ---- Foreground provision (the black-on-dark regression) --------------------------------------

    @Test
    fun picoThemeAloneProvidesNoContentColor() {
        // The regression this kit exists to prevent: PicoTheme does not provide LocalContentColor,
        // so an ordinary Text on an app-drawn surface falls through to the framework default.
        var inside: Color? = null
        rule.setContent { PicoTheme { CaptureContentColor { inside = it } } }
        rule.runOnIdle {
            assertNotNull(inside)
            assertFalse("PicoTheme unexpectedly provides a content colour", inside!!.isSpecified)
        }
    }

    @Test
    fun polishThemeProvidesAContentColor() {
        var inside: Color? = null
        var expected: Color? = null
        rule.setContent {
            PolishTheme {
                expected = PicoTheme.colorScheme.labelPrimary
                CaptureContentColor { inside = it }
            }
        }
        rule.runOnIdle {
            assertTrue(inside!!.isSpecified)
            assertEquals(expected, inside)
        }
    }

    @Test
    fun surfaceForegroundMatchesTheFillItChose() {
        var glassForeground: Color? = null
        var opaqueForeground: Color? = null
        var labelPrimary: Color? = null
        var labelPrimaryLight: Color? = null
        rule.setContent {
            PolishTheme {
                labelPrimary = PicoTheme.colorScheme.labelPrimary
                labelPrimaryLight = PicoTheme.colorScheme.labelPrimaryLight
                PolishSurface { CaptureContentColor { glassForeground = it } }
            }
            PolishTheme(options = PolishOptions(reduceTransparency = true)) {
                PolishSurface { CaptureContentColor { opaqueForeground = it } }
            }
        }
        rule.runOnIdle {
            assertEquals(labelPrimary, glassForeground)
            assertEquals(labelPrimaryLight, opaqueForeground)
        }
    }

    @Test
    fun emphasisSurfaceUsesTheLightLabelEvenWithoutReducedTransparency() {
        var foreground: Color? = null
        var labelPrimaryLight: Color? = null
        rule.setContent {
            PolishTheme {
                labelPrimaryLight = PicoTheme.colorScheme.labelPrimaryLight
                PolishSurface(kind = PolishSurfaceKind.Emphasis) { CaptureContentColor { foreground = it } }
            }
        }
        rule.runOnIdle { assertEquals(labelPrimaryLight, foreground) }
    }

    // ---- Reduced transparency owns an opaque fill ------------------------------------------------

    @Test
    fun reducedTransparencyTakesTheLiteralGroundNotAVibrancyRole() {
        // This test replaces `reducedTransparencyPicksARoleTheSchemeTreatsAsOpaque`, which asserted
        // that the surface took `ColorScheme.fillPrimary` and that the role was opaque. Both halves
        // passed here and the second was false on a device: off the spatial platform the scheme
        // resolves to `defaultColorScheme()`, where `fillPrimary` really is the literal 0xFF282828,
        // so the JVM could never have caught it. Four admitted frames did — see
        // `PolishTokens.OpaqueGround`.
        //
        // Note what this means for the assertion below: on this JVM the chosen literal and the
        // fallback `fillPrimary` are the SAME VALUE. That is exactly why the defect was invisible
        // here, and it is why the load-bearing assertion is the one about vibrancy, not the one
        // about equality.
        var chosen: Color? = null
        rule.setContent {
            PolishTheme(options = PolishOptions(reduceTransparency = true)) {
                val fill =
                    PolishSurfaceDefaults.fillFor(
                        PolishSurfaceKind.Card,
                        PolishOptions(reduceTransparency = true),
                    )
                chosen = (fill as PolishFill.Solid).color
            }
        }
        rule.runOnIdle {
            assertEquals(PolishTokens.OpaqueGround, chosen)
            assertEquals(1f, chosen!!.alpha, 0.001f)
            // The property that holds on every platform: the kit's ground carries no vibrancy
            // encoding, so there is nothing for a compositor to resolve it against.
            assertFalse(
                "the reduced-transparency ground must not be a vibrancy sentinel",
                chosen!!.containsVibrant(),
            )
        }
    }

    @Test
    fun theVibrancyDiscriminatorActuallyDiscriminates() {
        // Guards the assertion above: if `containsVibrant()` returned false for everything, the test
        // that matters would be vacuous.
        assertTrue(Color.Black.withVibrant(Vibrant.Darker).containsVibrant())
        assertFalse(PolishTokens.OpaqueGround.containsVibrant())
    }

    @Test
    fun everyKindIsOpaqueUnderReducedTransparency() {
        var results: List<Pair<PolishSurfaceKind, PolishFill>>? = null
        rule.setContent {
            PolishTheme(options = PolishOptions(reduceTransparency = true)) {
                results =
                    PolishSurfaceKind.values().map { kind ->
                        kind to
                            PolishSurfaceDefaults.fillFor(
                                kind,
                                PolishOptions(reduceTransparency = true),
                            )
                    }
            }
        }
        rule.runOnIdle {
            results!!.forEach { (kind, fill) ->
                assertTrue("$kind must take a solid fill", fill is PolishFill.Solid)
                assertEquals("$kind", 1f, (fill as PolishFill.Solid).color.alpha, 0.001f)
            }
        }
    }

    @Test
    fun reducedTransparencyAlwaysDrawsAnEdge() {
        // With glass gone, the edge is what keeps stacked opaque surfaces apart.
        var edges: List<PolishEdge?>? = null
        rule.setContent {
            PolishTheme(options = PolishOptions(reduceTransparency = true)) {
                edges =
                    PolishSurfaceKind.values().map { kind ->
                        PolishSurfaceDefaults.edgeFor(
                            kind,
                            PolishOptions(reduceTransparency = true),
                            selected = false,
                        )
                    }
            }
        }
        rule.runOnIdle { edges!!.forEach { assertNotNull(it) } }
    }

    @Test
    fun nativeUnreducedCardDrawsNoEdge() {
        var edge: PolishEdge? = null
        rule.setContent {
            PolishTheme {
                edge =
                    PolishSurfaceDefaults.edgeFor(
                        PolishSurfaceKind.Card,
                        PolishOptions(),
                        selected = false,
                    )
            }
        }
        rule.runOnIdle { assertNull(edge) }
    }

    // ---- The full option matrix ------------------------------------------------------------------

    @Test
    fun everyOptionCombinationStaysUsable() {
        val combos =
            PolishQuality.values().flatMap { quality ->
                listOf(false, true).flatMap { motion ->
                    listOf(false, true).flatMap { transparency ->
                        listOf(false, true).map { contrast ->
                            PolishOptions(quality, motion, transparency, contrast)
                        }
                    }
                }
            }
        assertEquals(16, combos.size)

        var report: List<String>? = null
        rule.setContent {
            PolishTheme {
                val failures = mutableListOf<String>()
                combos.forEach { options ->
                    PolishSurfaceKind.values().forEach { kind ->
                        val fill = PolishSurfaceDefaults.fillFor(kind, options)
                        val foreground = PolishSurfaceDefaults.foregroundFor(kind, options)
                        val secondary = PolishSurfaceDefaults.secondaryForegroundFor(kind, options)
                        if (!foreground.isSpecified) failures += "$options/$kind: no foreground"
                        if (!secondary.isSpecified) failures += "$options/$kind: no secondary"
                        if (options.reduceTransparency && fill !is PolishFill.Solid) {
                            failures += "$options/$kind: glass under reduced transparency"
                        }
                        if (options.reduceTransparency && fill is PolishFill.Solid &&
                            fill.color.alpha < 1f
                        ) {
                            failures += "$options/$kind: translucent 'opaque' fill"
                        }
                        if (options.increaseContrast &&
                            PolishSurfaceDefaults.edgeFor(kind, options, selected = false) == null
                        ) {
                            failures += "$options/$kind: no edge under increased contrast"
                        }
                    }
                }
                report = failures
            }
        }
        rule.runOnIdle { assertEquals(emptyList<String>(), report) }
    }

    @Test
    fun interactiveSurfacesNeverTakeAMaterialFill() {
        // The emulator found this one, and no host test could have. Hovering a clickable,
        // material-filled surface produced a capture byte-identical to the frame before it: zero
        // change in 1962x2018 px. The same surface with an app-drawn fill moved +16.48 mean gray
        // while three sibling controls moved 0.00. The SDK sources agree — PicoIndication draws
        // only the press colour on the spatial platform ("hover layer is rendered by spatial hover
        // effect modifier"), the material-aware hover modifier is internal with TabBar as its only
        // caller, and every interactive built-in fills with a drawn role colour.
        //
        // So: a surface the user can click must draw its own fill, and a surface they cannot click
        // must keep the material it had. Both halves are asserted, because substituting the fill
        // everywhere would have quietly thrown away the glass this kit exists to show.
        val combos =
            PolishQuality.values().flatMap { quality ->
                listOf(false, true).flatMap { motion ->
                    listOf(false, true).flatMap { transparency ->
                        listOf(false, true).map { contrast ->
                            PolishOptions(quality, motion, transparency, contrast)
                        }
                    }
                }
            }

        var report: List<String>? = null
        rule.setContent {
            PolishTheme {
                val failures = mutableListOf<String>()
                combos.forEach { options ->
                    PolishSurfaceKind.values().forEach { kind ->
                        val inert = PolishSurfaceDefaults.fillFor(kind, options, interactive = false)
                        val live = PolishSurfaceDefaults.fillFor(kind, options, interactive = true)
                        if (live !is PolishFill.Solid) {
                            failures += "$options/$kind: interactive surface kept a material fill"
                        }
                        if (live is PolishFill.Solid && !live.color.isSpecified) {
                            failures += "$options/$kind: interactive fill is unspecified"
                        }
                        if (inert is PolishFill.Glass && live is PolishFill.Solid &&
                            live.color.alpha <= 0f
                        ) {
                            failures += "$options/$kind: interactive fill is fully transparent"
                        }
                        if (inert is PolishFill.Solid && inert != live) {
                            failures += "$options/$kind: an already-drawn fill was substituted"
                        }
                    }
                }
                // The non-interactive glass this kit exists to show must survive the rule.
                val stillGlass =
                    PolishSurfaceDefaults.fillFor(
                        PolishSurfaceKind.Card,
                        PolishOptions(),
                        interactive = false,
                    )
                if (stillGlass !is PolishFill.Glass) {
                    failures += "a non-interactive Card lost its material"
                }
                report = failures
            }
        }
        rule.runOnIdle { assertEquals(emptyList<String>(), report) }
    }

    @Test
    fun selectionEdgeIsThickerThanTheOrdinaryEdge() {
        // Selection must not be signalled by colour alone.
        var selectedEdge: PolishEdge? = null
        var plainEdge: PolishEdge? = null
        rule.setContent {
            PolishTheme(options = PolishOptions(reduceTransparency = true)) {
                val options = PolishOptions(reduceTransparency = true)
                selectedEdge = PolishSurfaceDefaults.edgeFor(PolishSurfaceKind.Card, options, true)
                plainEdge = PolishSurfaceDefaults.edgeFor(PolishSurfaceKind.Card, options, false)
            }
        }
        rule.runOnIdle {
            assertTrue(selectedEdge!!.width > plainEdge!!.width)
        }
    }

    /** Source-over composite, so a translucent role can be measured against what it sits on. */
    private fun composite(top: Color, over: Color): Color {
        val a = top.alpha
        return Color(
            red = top.red * a + over.red * (1f - a),
            green = top.green * a + over.green * (1f - a),
            blue = top.blue * a + over.blue * (1f - a),
            alpha = 1f,
        )
    }

    /** WCAG relative-luminance contrast, for colours that are already resolved to fixed values. */
    private fun contrastRatio(a: Color, b: Color): Float {
        fun channel(v: Float) = if (v <= 0.03928f) v / 12.92f else Math.pow(((v + 0.055f) / 1.055f).toDouble(), 2.4).toFloat()
        fun luminance(c: Color) = 0.2126f * channel(c.red) + 0.7152f * channel(c.green) + 0.0722f * channel(c.blue)
        val la = luminance(a)
        val lb = luminance(b)
        val hi = maxOf(la, lb)
        val lo = minOf(la, lb)
        return (hi + 0.05f) / (lo + 0.05f)
    }

    @Composable
    private fun CaptureContentColor(onColor: (Color) -> Unit) {
        onColor(LocalContentColor.current)
        Box(Modifier.size(1.dp))
    }

    private companion object {
        const val TAG = "polish-target"
    }
}
