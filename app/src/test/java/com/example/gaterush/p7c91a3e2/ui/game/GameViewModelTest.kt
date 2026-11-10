package com.example.gaterush.p7c91a3e2.ui.game

import com.example.gaterush.p7c91a3e2.data.repository.LocalLevelRepository
import com.example.gaterush.p7c91a3e2.domain.model.Phase
import com.example.gaterush.p7c91a3e2.domain.usecase.PlayGameUseCase
import org.junit.Assert.*
import org.junit.Test

class GameViewModelTest {
    private fun model()=GameViewModel(PlayGameUseCase(LocalLevelRepository()))
    @Test fun initialLoadBlocksStart() {
        val vm=model(); vm.onEvent(GameEvent.Start)
        assertEquals(Phase.LOADING,vm.state.value.game.phase)
    }
    @Test fun startPauseResumePublishesState() {
        val vm=model(); vm.simulation.assetsReady()
        vm.onEvent(GameEvent.Start); assertEquals(Phase.RUNNING,vm.state.value.game.phase)
        vm.onEvent(GameEvent.Pause); assertEquals(Phase.PAUSED,vm.state.value.game.phase)
        vm.onEvent(GameEvent.Resume); assertEquals(Phase.RUNNING,vm.state.value.game.phase)
    }
    @Test fun dragBoundaryAndEndAreReducedThroughEvents() {
        val vm=model(); vm.simulation.assetsReady(); vm.onEvent(GameEvent.Start)
        vm.onEvent(GameEvent.DragStart); vm.onEvent(GameEvent.Drag(-5f))
        assertEquals(-.32f,vm.simulation.targetX,0f)
        vm.onEvent(GameEvent.DragEnd); assertFalse(vm.state.value.game.dragging)
    }
    @Test fun rulesReturnPreservesPausedGame() {
        val vm=model(); vm.simulation.assetsReady(); vm.onEvent(GameEvent.Start)
        vm.onEvent(GameEvent.Pause); vm.onEvent(GameEvent.Rules)
        assertEquals(Phase.RULES,vm.state.value.game.phase)
        vm.onEvent(GameEvent.Back); assertEquals(Phase.PAUSED,vm.state.value.game.phase)
    }
    @Test fun failedRequiredAssetOffersLoadingRetry() {
        val vm=model(); vm.simulation.assetsFailed("missing"); vm.refresh()
        assertEquals(Phase.LOAD_ERROR,vm.state.value.game.phase)
        vm.onEvent(GameEvent.Start); assertEquals(Phase.LOAD_ERROR,vm.state.value.game.phase)
        vm.onEvent(GameEvent.RetryAssets); assertEquals(Phase.LOADING,vm.state.value.game.phase)
    }
}
