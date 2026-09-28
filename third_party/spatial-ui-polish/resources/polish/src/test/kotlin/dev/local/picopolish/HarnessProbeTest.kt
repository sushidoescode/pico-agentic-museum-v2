package dev.local.picopolish

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.pico.spatial.ui.design.PicoTheme
import com.pico.spatial.ui.design.Text
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Probe only: proves the host test harness can compose SpatialUI under Robolectric. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HarnessProbeTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun picoThemeComposesOnTheJvm() {
        rule.setContent { PicoTheme { Text("harness-ok") } }
        rule.onNodeWithText("harness-ok").assertIsDisplayed()
    }
}
