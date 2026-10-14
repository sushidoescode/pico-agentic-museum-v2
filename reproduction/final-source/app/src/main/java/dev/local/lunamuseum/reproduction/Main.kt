package dev.local.lunamuseum.reproduction

import androidx.compose.ui.unit.dp
import com.pico.spatial.ui.design.PicoTheme
import com.pico.spatial.ui.foundation.dsl.DefaultWindowContainer
import com.pico.spatial.ui.foundation.dsl.Form
import com.pico.spatial.ui.foundation.dsl.SpatialAppScope
import com.pico.spatial.ui.foundation.dsl.Stage
import com.pico.spatial.ui.foundation.dsl.WindowContainer
import com.pico.spatial.ui.foundation.dsl.WindowContainerSize
import com.pico.spatial.ui.platform.containers.LocalSpatialNavigator
import dev.local.lunamuseum.reproduction.content.CompanionWindow
import dev.local.lunamuseum.reproduction.content.HomeVolume
import dev.local.lunamuseum.reproduction.content.MuseumRoomStage

fun mainApp(scope: SpatialAppScope) =
    with(scope) {
        DefaultWindowContainer {
            PicoTheme {
                val navigator = LocalSpatialNavigator.current
                HomeVolume(
                    onOpenCompanion = {
                        CompanionWindow.open(navigator)
                    },
                )
            }
        }

        WindowContainer(
            id = CompanionWindow.ID,
            form = Form.Planar,
            defaultSize = WindowContainerSize(width = 480.dp, height = 620.dp),
        ) {
            PicoTheme {
                CompanionWindow.Content()
            }
        }

        Stage(id = MuseumRoomStage.ID) {
            PicoTheme {
                MuseumRoomStage.Content()
            }
        }
    }
