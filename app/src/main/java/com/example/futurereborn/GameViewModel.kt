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
        val s = _state.value
        // Quand on change de job, il y a un temps d'attente (réduit par Charisme)
        val delay = if (id == null || id == s.activeJob) {
            0.0
        } else {
            Engine.jobStartDelaySeconds(s)
        }
        _state.value = s.copy(activeJob = id, jobWaitSecondsRemaining = delay)
    }

    fun selectHousing(id: HousingId) {
        _state.value = _state.value.copy(selectedHousing = id)
    }

    fun selectFood(id: FoodId?) {
        _state.value = _state.value.copy(selectedFood = id)
    }

    fun buyOther(id: OtherId) {
        val s = _state.value
        _state.value = s.copy(
            ownedOther = s.ownedOther + id,
            activeOther = s.activeOther + id
        )
    }

    fun toggleOtherActive(id: OtherId) {
        val s = _state.value
        val next = if (s.activeOther.contains(id)) s.activeOther - id else s.activeOther + id
        _state.value = s.copy(activeOther = next)
    }

    fun buyUpgrade(id: UpgradeId) {
        _state.value = Engine.buyUpgrade(_state.value, id)
    }

    fun reincarnateNow() {
        _state.value = Engine.manualReincarnate(_state.value)
    }
}
