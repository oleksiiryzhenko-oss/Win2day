package com.taichiflow.app.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taichiflow.app.data.repository.TaiChiRepository
import com.taichiflow.app.domain.model.Difficulty
import com.taichiflow.app.domain.model.Goal
import com.taichiflow.app.domain.usecase.AppSnapshot
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** App-wide state: the derived snapshot of all stored data, plus profile/settings actions. */
class MainViewModel(private val repo: TaiChiRepository) : ViewModel() {

    val snapshot: StateFlow<AppSnapshot?> = repo.snapshot.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun updateProfile(name: String, experience: Difficulty, goal: Goal) {
        viewModelScope.launch { repo.updateProfile(name, experience, goal) }
    }

    fun setFocusModeDefault(value: Boolean) {
        viewModelScope.launch { repo.setFocusModeDefault(value) }
    }

    fun setHaptics(value: Boolean) {
        viewModelScope.launch { repo.setHaptics(value) }
    }

    fun resetAll(onDone: () -> Unit) {
        viewModelScope.launch {
            repo.resetAll()
            onDone()
        }
    }

    fun loadDemoData(onDone: () -> Unit) {
        viewModelScope.launch {
            repo.loadDemoData()
            onDone()
        }
    }

    fun deleteCustomSession(id: String, onDone: () -> Unit) {
        viewModelScope.launch {
            repo.deleteCustomSession(id)
            onDone()
        }
    }
}
