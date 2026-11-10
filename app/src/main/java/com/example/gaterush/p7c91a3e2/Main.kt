package com.example.gaterush.p7c91a3e2

import com.example.gaterush.p7c91a3e2.content.HomeStage
import com.pico.spatial.ui.design.PicoTheme
import com.pico.spatial.ui.foundation.dsl.DefaultStage
import com.pico.spatial.ui.foundation.dsl.SpatialAppScope

fun mainApp(scope: SpatialAppScope) =
    with(scope) {
        DefaultStage {
            PicoTheme {
                HomeStage()
            }
        }
    }

