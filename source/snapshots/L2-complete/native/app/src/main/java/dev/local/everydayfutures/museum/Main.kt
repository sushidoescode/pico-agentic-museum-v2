package dev.local.everydayfutures.museum

import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pico.spatial.ui.foundation.dsl.DefaultWindowContainer
import com.pico.spatial.ui.foundation.dsl.Form
import com.pico.spatial.ui.foundation.dsl.Placement
import com.pico.spatial.ui.foundation.dsl.SpatialAppScope
import com.pico.spatial.ui.foundation.dsl.WindowContainer
import com.pico.spatial.ui.foundation.dsl.WindowContainerSize
import com.pico.spatial.ui.foundation.dsl.WorldScale
import dev.local.everydayfutures.museum.content.CompanionWindow
import dev.local.everydayfutures.museum.content.MuseumRoot
import dev.local.everydayfutures.museum.content.MuseumWindow
import dev.local.everydayfutures.museum.state.MuseumController
import dev.local.everydayfutures.museum.state.Surface
import dev.local.everydayfutures.museum.state.WorkspaceReducer

/**
 * App entry: two window roots, one state owner.
 *
 * The launcher window is the DefaultWindowContainer declared by manifest meta-data (volumetric form, system
 * Material.Regular glass ON). The companion is registered here only (it runs on the SDK's own stub activity, so the
 * manifest does not change), keeps the DSL default `enableMaterialBackground = true`, and is opened with a fixed id and
 * tag so at most one instance exists. Neither root paints its window root; [MuseumRoot] tells the polish kit that the
 * container owns its glass. Both roots receive the same [museum] instance; nothing here creates state.
 */
fun mainApp(scope: SpatialAppScope, museum: MuseumController) = with(scope) {
    DefaultWindowContainer {
        MuseumRoot(controller = museum, surface = Surface.PRIMARY) {
            MuseumWindow(
                controller = museum,
                modifier = Modifier.windowConstraints(minWidth = 1120.dp, minHeight = 720.dp, minDepth = 480.dp),
            )
        }
    }
    WindowContainer(
        id = WorkspaceReducer.COMPANION_ID,
        // Volumetric so the L3 story miniature has depth in this window; in details mode it shows reading surfaces only.
        form = Form.Volumetric,
        // Smaller than the primary (1440x1040 dp) so, at the same dynamic scale, the primary's catalogue edge, header,
        // details edge and bottom controls stay visible around it and either window can be brought forward.
        defaultSize = WindowContainerSize(width = 880.dp, height = 480.dp, depth = 400.dp),
        worldScale = WorldScale.Dynamic,
        // The system default: centred just in front of the caller, where the emulator's fixed forward view can see it.
        // Anchored placement (builds 001, 003, 004) always put the new window in front of the primary's volume and
        // outside that view, and a negative offset is read as unsigned. Where the windows overlap, the platform draws
        // the focused one in front and fades the other (evidence/l2-workspace-001/PLACEMENT.md). No accuracy is claimed.
        placement = { Placement.none() },
    ) {
        MuseumRoot(controller = museum, surface = Surface.COMPANION) {
            CompanionWindow(
                controller = museum,
                modifier = Modifier.windowConstraints(minWidth = 800.dp, minHeight = 440.dp, minDepth = 320.dp),
            )
        }
    }
}
