package dev.local.picopolish

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import com.pico.spatial.ui.design.ListItem
import com.pico.spatial.ui.design.PicoTheme
import com.pico.spatial.ui.design.Switch
import com.pico.spatial.ui.design.Text
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Renders the kit to real pixels on the JVM and writes them where a reviewer can open them.
 *
 * **What these frames are.** Host-rendered stills from Robolectric's native graphics pipeline. They
 * show layout, hierarchy, type scale, spacing, opacity policy, edge treatment and the foreground
 * pairing — the things a wrong decision breaks the same way everywhere.
 *
 * **What they are emphatically not.** Device evidence, or evidence about glass. Off a spatial
 * platform `backgroundMaterial` and `spatialHoverEffect` are no-ops by declaration, and the colour
 * scheme resolves through the non-adaptive fallback instead of the Vibrant-encoded roles the
 * compositor adapts. So a surface that shows the compositor material on a headset renders as
 * nothing here, and that absence is expected rather than a defect. Only an opened emulator or
 * headset frame can speak about the material itself.
 *
 * Output: PNG files under `build/render`, collected into the run's evidence directory. Set
 * `-Dpolish.render.out=<dir>` to write elsewhere.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w1280dp-h720dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PolishRenderTest {

    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    // One frame per test: setContent may be called only once per composition rule.

    @Test fun stockBaseline() = capture("01-stock-baseline") { StockSample() }

    @Test
    fun polishedNative() = capture("02-polished-native") {
        PolishTheme(options = PolishOptions(quality = PolishQuality.Native)) { PolishedSample() }
    }

    @Test
    fun polishedEnhanced() = capture("03-polished-enhanced") {
        PolishTheme(options = PolishOptions(quality = PolishQuality.Enhanced)) { PolishedSample() }
    }

    @Test
    fun reducedTransparency() = capture("04-reduced-transparency") {
        PolishTheme(options = PolishOptions(reduceTransparency = true)) { PolishedSample() }
    }

    @Test
    fun increasedContrast() = capture("05-increased-contrast") {
        PolishTheme(options = PolishOptions(increaseContrast = true)) { PolishedSample() }
    }

    @Test
    fun reducedTransparencyAndContrast() = capture("06-reduced-transparency-and-contrast") {
        PolishTheme(options = PolishOptions(reduceTransparency = true, increaseContrast = true)) {
            PolishedSample()
        }
    }

    @Test
    fun surfaceKinds() = capture("07-surface-kinds") {
        PolishTheme { SurfaceKindsSample(PolishOptions()) }
    }

    @Test
    fun surfaceKindsReducedTransparency() = capture("08-surface-kinds-reduced-transparency") {
        PolishTheme(options = PolishOptions(reduceTransparency = true)) {
            SurfaceKindsSample(PolishOptions(reduceTransparency = true))
        }
    }

    @Test
    fun buttonTonesAndStates() = capture("09-button-tones-and-states") {
        PolishTheme { ButtonMatrixSample() }
    }

    @Test
    fun fieldsOnAnOpaqueSurface() = capture("10-fields-opaque-surface") {
        PolishTheme(options = PolishOptions(reduceTransparency = true)) {
            PolishSurface(modifier = Modifier.fillMaxWidth(), kind = PolishSurfaceKind.Panel) {
                PolishField(label = "Curator note", value = "Typed text on an opaque surface", onValueChange = {})
                PolishField(
                    label = "In error",
                    value = "",
                    onValueChange = {},
                    placeholder = "Placeholder",
                    errorMessage = "This field is required",
                )
                PolishSwitchRow(title = "Reduce motion", checked = true, onCheckedChange = {})
                PolishSwitchRow(title = "Locked by policy", checked = false, onCheckedChange = {}, enabled = false)
                PolishFactRow(label = "Length", value = "4:20")
            }
        }
    }

    @Test
    fun fieldsOnAGlassSurface() = capture("11-fields-glass-surface") {
        PolishTheme {
            PolishSurface(modifier = Modifier.fillMaxWidth(), kind = PolishSurfaceKind.Panel) {
                PolishField(label = "Curator note", value = "Typed text on the default surface", onValueChange = {})
                PolishField(
                    label = "In error",
                    value = "",
                    onValueChange = {},
                    placeholder = "Placeholder",
                    errorMessage = "This field is required",
                )
                PolishSwitchRow(title = "Reduce motion", checked = true, onCheckedChange = {})
                PolishSwitchRow(title = "Locked by policy", checked = false, onCheckedChange = {}, enabled = false)
                PolishFactRow(label = "Length", value = "4:20")
            }
        }
    }

    private fun capture(name: String, content: @Composable () -> Unit) {
        rule.setContent {
            Box(modifier = Modifier.width(760.dp).testTag(CAPTURE_TAG).padding(24.dp)) { content() }
        }
        rule.waitForIdle()
        // Draw the view tree straight to a bitmap. Compose's captureToImage() goes through
        // PixelCopy, which never reports a redraw under Robolectric and times out after 2 s.
        val view = rule.activity.window.decorView
        val image = Bitmap.createBitmap(maxOf(view.width, 1), maxOf(view.height, 1), Bitmap.Config.ARGB_8888)
        view.draw(Canvas(image))
        val configured = System.getProperty("polish.render.out")
        val outDir = File(if (configured.isNullOrBlank()) "build/render" else configured)
        outDir.mkdirs()
        File(outDir, "$name.png").outputStream().use { stream ->
            image.compress(Bitmap.CompressFormat.PNG, 100, stream)
        }
    }

    /** The same content the showcase's comparison flow renders, without the kit. */
    @Composable
    private fun StockSample() {
        PicoTheme {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    TITLE,
                    style = PicoTheme.typography.titleSmall,
                    color = PicoTheme.colorScheme.labelPrimary,
                )
                Text(
                    SUBTITLE,
                    style = PicoTheme.typography.bodySmall,
                    color = PicoTheme.colorScheme.labelTertiary,
                )
                ListItem(
                    headlineContent = { Text("Reduce motion") },
                    trailingContent = { Switch(checked = true, onCheckedChange = {}) },
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    com.pico.spatial.ui.design.Button(onClick = {}) { Text("Open") }
                    com.pico.spatial.ui.design.Button(onClick = {}) { Text("Later") }
                }
            }
        }
    }

    /** The same content through the kit. */
    @Composable
    private fun PolishedSample() {
        PolishSurface(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(PolishTokens.SpaceTiny)) {
                Text(TITLE, style = PicoTheme.typography.titleSmall)
                Text(SUBTITLE, style = PicoTheme.typography.bodySmall)
            }
            PolishSwitchRow(title = "Reduce motion", checked = true, onCheckedChange = {})
            Row(horizontalArrangement = Arrangement.spacedBy(PolishTokens.SpaceSmall)) {
                PolishButton(onClick = {}, tone = PolishButtonTone.Emphasis) { Text("Open") }
                PolishButton(onClick = {}, tone = PolishButtonTone.Quiet) { Text("Later") }
            }
        }
    }

    @Composable
    private fun SurfaceKindsSample(options: PolishOptions) {
        Column(verticalArrangement = Arrangement.spacedBy(PolishTokens.SpaceSmall)) {
            PolishSurfaceKind.values().forEach { kind ->
                PolishSurface(
                    modifier = Modifier.fillMaxWidth(),
                    kind = kind,
                    verticalArrangement = Arrangement.spacedBy(PolishTokens.SpaceTiny),
                ) {
                    Text(kind.name, style = PicoTheme.typography.labelLarge)
                    Text(
                        "secondary line",
                        style = PicoTheme.typography.bodySmall,
                        color = PolishSurfaceDefaults.secondaryForegroundFor(kind, options),
                    )
                }
            }
        }
    }

    @Composable
    private fun ButtonMatrixSample() {
        PolishSurface(modifier = Modifier.fillMaxWidth()) {
            Row(horizontalArrangement = Arrangement.spacedBy(PolishTokens.SpaceSmall)) {
                PolishButton(onClick = {}, tone = PolishButtonTone.Standard) { Text("Standard") }
                PolishButton(onClick = {}, tone = PolishButtonTone.Emphasis) { Text("Emphasis") }
                PolishButton(onClick = {}, tone = PolishButtonTone.Quiet) { Text("Quiet") }
                PolishButton(onClick = {}, tone = PolishButtonTone.Destructive) { Text("Delete") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(PolishTokens.SpaceSmall)) {
                PolishButton(onClick = {}) { Text("Enabled") }
                PolishButton(onClick = {}, enabled = false) { Text("Disabled") }
                PolishButton(onClick = {}, loading = true) { Text("Loading") }
            }
        }
    }

    private companion object {
        const val CAPTURE_TAG = "polish-capture"
        const val TITLE = "Aurora over the ridge"
        const val SUBTITLE = "Field recording · 2026"
    }
}
