package com.example.gaterush.p7c91a3e2.ui.game

import androidx.lifecycle.ViewModel
import com.example.gaterush.p7c91a3e2.domain.usecase.PlayGameUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class GameViewModel(val simulation: PlayGameUseCase) : ViewModel() {
    private val mutable = MutableStateFlow(GameUiState(simulation.snapshot()))
    val state = mutable.asStateFlow()
    fun refresh() { mutable.value = GameUiState(simulation.snapshot()) }
    fun onEvent(event: GameEvent) {
        when(event) {
            GameEvent.Start, GameEvent.Restart -> simulation.start()
            GameEvent.Pause -> simulation.pause()
            GameEvent.Resume -> simulation.resume()
            GameEvent.Next -> simulation.next()
            GameEvent.Replay -> simulation.replay()
            GameEvent.Rules -> simulation.rules()
            GameEvent.Back -> simulation.back()
            GameEvent.DragStart -> simulation.beginDrag()
            GameEvent.DragEnd -> simulation.cancelDrag()
            is GameEvent.Drag -> simulation.drag(event.meters)
            GameEvent.Exit -> simulation.pause()
            GameEvent.RetryAssets -> simulation.loading()
        }
        refresh()
    }
}
