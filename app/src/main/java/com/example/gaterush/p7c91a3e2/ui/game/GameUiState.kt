package com.example.gaterush.p7c91a3e2.ui.game

import com.example.gaterush.p7c91a3e2.domain.model.GameSnapshot

data class GameUiState(val game: GameSnapshot = GameSnapshot())
sealed interface GameEvent {
    data object Start : GameEvent
    data object Pause : GameEvent
    data object Resume : GameEvent
    data object Restart : GameEvent
    data object Next : GameEvent
    data object Replay : GameEvent
    data object Rules : GameEvent
    data object Back : GameEvent
    data object Exit : GameEvent
    data object RetryAssets : GameEvent
    data object DragStart : GameEvent
    data object DragEnd : GameEvent
    data class Drag(val meters: Float) : GameEvent
}
