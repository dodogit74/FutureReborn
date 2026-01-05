package com.example.futurereborn

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class GameViewModel(app: Application) : AndroidViewModel(app) {

    private val _state = mutableStateOf(GameState())
    val state: GameState get() = _state.value

    init {
        val ctx = getApplication<Application>().applicationContext

        // Charger sauvegarde
        viewModelScope.launch {
            val saved = Persistence.flow(ctx).first()
            if (saved != null) _state.value = saved
        }

        // Boucle du jeu
        viewModelScope.launch {
            val tickMs = 200L
            val dt = tickMs / 1000.0
            while (true) {
                delay(tickMs)
                _state.value = Engine.tick(_state.value, dt)
            }
        }

        // Autosave
        viewModelScope.launch {
            while (true) {
                delay(5_000)
                Persistence.save(ctx, _state.value)
            }
        }
    }

    fun setActivity(id: ActivityId) {
        _state.value = _state.value.copy(activeActivity = id)
    }

    fun setJob(id: JobId?) {
        _state.value = _state.value.copy(activeJob = id)
    }

    fun buyUpgrade(id: UpgradeId) {
        _state.value = Engine.buyUpgrade(_state.value, id)
    }

    fun reincarnateNow() {
        _state.value = Engine.manualReincarnate(_state.value)
    }
}
