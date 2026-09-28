package dev.local.everydayfutures.museum

import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pico.spatial.ui.foundation.dsl.DefaultWindowContainer
import com.pico.spatial.ui.foundation.dsl.Form
import com.pico.spatial.ui.foundation.dsl.Placement
import com.pico.spatial.ui.foundation.dsl.SpatialAppScope
import com.pico.spatial.ui.foundation.dsl.Stage
import com.pico.spatial.ui.foundation.dsl.WindowContainer
import com.pico.spatial.ui.foundation.dsl.WindowContainerSize
import com.pico.spatial.ui.foundation.dsl.WorldScale
import dev.local.everydayfutures.museum.content.CompanionWindow
import dev.local.everydayfutures.museum.content.InspectorWindow
import dev.local.everydayfutures.museum.content.MuseumRoot
import dev.local.everydayfutures.museum.content.MuseumWindow
import dev.local.everydayfutures.museum.stage.ArchiveStage
import dev.local.everydayfutures.museum.state.MuseumController
import dev.local.everydayfutures.museum.state.Surface
import dev.local.everydayfutures.museum.state.WorkspaceReducer
import dev.local.picopolish.PolishWindowSurface

/**
 * App entry: three window roots, one state owner.
 *
 * The launcher window is the DefaultWindowContainer declared by manifest meta-data (volumetric form, system
 * Material.Regular glass ON). The companion and the Open in 3D inspector are registered here only (they run on the SDK's
 * own stub activity, so the manifest does not change) and are opened with fixed ids and tags, so at most one instance
 * of each exists, and the controller shows at most one of the two at a time. The companion keeps the DSL default
 * `enableMaterialBackground = true`; the inspector turns it off explicitly (a null would merge to true), so its model
 * stands free in the volume, and tells the polish kit so (`AppOpaque`: nothing painted, no glass spent). No root paints
 * its window root. Every root receives the same [museum] instance; nothing here creates state. The immersive archive is
 * a DSL-only Stage (`WorkspaceReducer.ARCHIVE_ID`; no manifest entry, the SDK's stub activity), opened from Enter archive
 * with `StageStyle.Full` and closed by its own Return to catalogue; its root paints nothing and spends no glass.
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
    WindowContainer(
        id = WorkspaceReducer.INSPECTOR_ID,
        // Volumetric, so the model has depth to turn in; sized for a ~0.30 m fitted model plus its title chip.
        form = Form.Volumetric,
        defaultSize = WindowContainerSize(width = 720.dp, height = 640.dp, depth = 560.dp),
        worldScale = WorldScale.Dynamic,
        // Explicit: the DSL merges null to true (system glass). Off, the model stands in the volume with no backdrop.
        enableMaterialBackground = false,
        // The same default placement as the companion (centred just in front of the caller, inside the emulator's
        // fixed forward view); anchored placements landed outside it (evidence/l2-workspace-001/PLACEMENT.md).
        placement = { Placement.none() },
    ) {
        MuseumRoot(controller = museum, surface = Surface.INSPECTOR, windowSurface = PolishWindowSurface.AppOpaque) {
            InspectorWindow(
                controller = museum,
                modifier = Modifier.windowConstraints(minWidth = 560.dp, minHeight = 480.dp, minDepth = 400.dp),
            )
        }
    }
    Stage(id = WorkspaceReducer.ARCHIVE_ID) {
        MuseumRoot(controller = museum, surface = Surface.ARCHIVE, windowSurface = PolishWindowSurface.AppOpaque) {
            ArchiveStage(controller = museum)
        }
    }
}
