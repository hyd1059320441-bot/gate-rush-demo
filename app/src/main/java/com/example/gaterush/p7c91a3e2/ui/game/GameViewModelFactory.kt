package com.example.gaterush.p7c91a3e2.ui.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.gaterush.p7c91a3e2.data.repository.LocalLevelRepository
import com.example.gaterush.p7c91a3e2.domain.usecase.PlayGameUseCase

class GameViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(GameViewModel::class.java))
        @Suppress("UNCHECKED_CAST")
        return GameViewModel(PlayGameUseCase(LocalLevelRepository())) as T
    }
}
