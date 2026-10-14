package dev.local.everydayfutures.museum.content

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.pico.spatial.ui.design.windows.Subwindow
import com.pico.spatial.ui.design.windows.SubwindowPlacement
import com.pico.spatial.ui.foundation.geometry.NormalizedPoint3D
import com.pico.spatial.ui.foundation.geometry.Rotation3D
import com.pico.spatial.ui.foundation.geometry.RotationAxis3D
import dev.local.everydayfutures.museum.state.MuseumController
import dev.local.everydayfutures.museum.state.Surface

/**
 * Static tilt of the guide about its edge next to the primary, so its free edge can turn toward the visitor. The sign
 * is UNVERIFIED: the SDK documents no convention for [Rotation3D.degree] (Rotation3D.kt, Rotate3D.kt), so the
 * prototype's native check records which way it turns and flips this if it turns away.
 */
private const val GUIDE_TILT_DEGREES = -15f

/**
 * The companion as a SpatialUI Subwindow beside the primary (the CompanionHost.SUBWINDOW prototype, not seen natively;
 * the accepted host is the companion's own WindowContainer in Main.kt). Composed by the primary only while
 * `workspace.companionRequested` is true; it lives in the primary's composition, so it closes with the primary.
 *
 * What the SDK 6.1.9 source fixes (Subwindow.kt:55-84, Augments.kt:166-228): a dialog-backed window 360 dp wide
 * (internal SubwindowDefaults.FixWidth) at the container's full height, on system glass (Material.Regular), 24 dp
 * from the container's edge, created without animation, not touch-modal, and never dismissed by the system (its
 * dismiss request is a no-op), so the guide's own header keeps Close at the top. Placement is passed as Right
 * explicitly: the KDoc of Default says left for LTR, but the code maps Default to the right (Subwindow.kt:100-122).
 * The pivot's z is ignored (Subwindow.kt:44); the tilt is static, so Reduce motion is unaffected.
 *
 * The content is wrapped in [MuseumRoot] as [Surface.COMPANION], so attach and detach are counted exactly as for the
 * window and the one-auxiliary-window policy is unchanged.
 */
@Composable
fun CompanionSubwindow(controller: MuseumController) {
    Subwindow(
        rotation3D = Rotation3D(degree = GUIDE_TILT_DEGREES, axis = RotationAxis3D.Y, pivot = NormalizedPoint3D.Left),
        placement = SubwindowPlacement.Right,
    ) {
        MuseumRoot(controller = controller, surface = Surface.COMPANION) {
            CompanionWindow(controller = controller, modifier = Modifier.fillMaxSize(), compact = true)
        }
    }
}
