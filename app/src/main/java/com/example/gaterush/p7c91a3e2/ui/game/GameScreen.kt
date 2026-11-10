package com.example.gaterush.p7c91a3e2.ui.game

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gaterush.p7c91a3e2.scene.GateRushScene
import com.example.gaterush.p7c91a3e2.platform.LaunchActivity
import com.example.gaterush.p7c91a3e2.domain.model.Phase
import com.example.gaterush.p7c91a3e2.ui.game.components.GameHud
import com.pico.spatial.ui.design.PicoTheme
import com.pico.spatial.ui.foundation.content.SpatialView
import com.pico.spatial.ui.foundation.gesture.TargetEntity
import com.pico.spatial.ui.foundation.gesture.detectSpatialDragGesture
import com.pico.spatial.ui.platform.LengthUnit
import com.pico.spatial.ui.platform.LocalPhysicalLengthConverter
import com.pico.spatial.ui.platform.LocalSpatialContainerStateOwner
import com.pico.spatial.ui.platform.containers.LocalSpatialNavigator
import com.pico.spatial.core.container.SpatialContainerStateOwner
import com.pico.spatial.core.container.SpatialContainerStateEvent
import com.pico.spatial.core.container.SpatialContainerStateObserver
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun GameScreen() {
    val vm:GameViewModel=viewModel(factory=GameViewModelFactory())
    val state by vm.state.collectAsStateWithLifecycle()
    var loadAttempt by remember { mutableIntStateOf(0) }
    val context=LocalContext.current
    val scene=remember(loadAttempt) {
        GateRushScene(context,vm.simulation) {
            if(LaunchActivity.diagnosticsStart && vm.simulation.phase==Phase.READY) {
                LaunchActivity.diagnosticsStart=false
                vm.onEvent(GameEvent.Start)
            } else vm.refresh()
        }
    }
    val lifecycle=LocalLifecycleOwner.current.lifecycle
    val containerOwner=LocalSpatialContainerStateOwner.current
    val navigator=LocalSpatialNavigator.current
    val scope=rememberCoroutineScope()
    DisposableEffect(scene,lifecycle) {
        val observer=LifecycleEventObserver { _,event ->
            if(event==Lifecycle.Event.ON_PAUSE || event==Lifecycle.Event.ON_STOP) {
                scene.setForeground(false)
                vm.onEvent(GameEvent.Pause)
            } else if(event==Lifecycle.Event.ON_RESUME) scene.setForeground(true)
        }
        scene.setForeground(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer); scene.close() }
    }
    DisposableEffect(scene,containerOwner) {
        val observable=containerOwner.stateObservable
        fun applyFocus() {
            val focused=observable.currentState.isFocused
            scene.setContainerFocused(focused)
            if(!focused) vm.onEvent(GameEvent.Pause)
        }
        val observer=object:SpatialContainerStateObserver {
            override fun onStateChanged(owner:SpatialContainerStateOwner,event:SpatialContainerStateEvent) {
                applyFocus()
            }
        }
        observable.addObserver(observer); applyFocus()
        onDispose { observable.removeObserver(observer) }
    }
    val onEvent:(GameEvent)->Unit={ event ->
        vm.onEvent(event)
        scene.synchronizeAudioState()
        when(event) {
            GameEvent.RetryAssets -> loadAttempt++
            GameEvent.Exit -> scope.launch {
                try { navigator.closeStage(); context.activity()?.finish() }
                catch(cancelled:CancellationException) { throw cancelled }
                catch(error:Exception) {
                    Log.e("GateRush","EXIT_FAILED",error)
                    vm.simulation.assetsFailed("退出未完成，请再次点击退出"); vm.refresh()
                }
            }
            else -> Unit
        }
    }
    key(loadAttempt) { GameContent(state,onEvent,scene) }
}
@Composable
internal fun GameContent(state:GameUiState,onEvent:(GameEvent)->Unit,scene:GateRushScene) {
    val context=LocalContext.current
    val density=LocalDensity.current
    val converter=LocalPhysicalLengthConverter.current
    SpatialView(
        modifier=Modifier.fillMaxSize().pointerInput(scene,density,converter) {
            detectSpatialDragGesture(context,TargetEntity.hit(scene.avatarHit),
                { _,_ -> onEvent(GameEvent.DragStart) },
                { _ -> onEvent(GameEvent.DragEnd) },
                { _ -> onEvent(GameEvent.DragEnd) }) { value ->
                val dp=with(density) { value.dragAmount.x.toDp() }
                onEvent(GameEvent.Drag(converter.dpToLength(dp,LengthUnit.Meters)))
            }
        },
        attachments={ AttachmentPanel("game_hud") { PicoTheme { GameHud(state,onEvent) } } }
    ) { content,attachments ->
        content.addEntity(scene.root)
        try { scene.initialize(attachments.entity("game_hud")) }
        catch(cancelled:CancellationException) { throw cancelled }
        catch(error:Exception) {
            Log.e("GateRush","SCENE_LOAD_FAILED",error)
            scene.game.assetsFailed(error.message ?: error.javaClass.simpleName)
            onEvent(GameEvent.DragEnd)
        }
    }
}
private fun Context.activity():Activity? = when(this) {
    is Activity -> this
    is ContextWrapper -> if(baseContext===this) null else baseContext.activity()
    else -> null
}
