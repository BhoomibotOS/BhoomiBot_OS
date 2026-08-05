package com.bhoomibot.os.feature.autonomous.skills.library.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bhoomibot.os.feature.autonomous.skills.library.SkillLibrary
import com.bhoomibot.os.feature.autonomous.skills.models.DemonstratedSkill
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SkillLibraryUiState(
    val skills: List<DemonstratedSkill> = emptyList(),
    val isLoading: Boolean = false,
    val selectedSkill: DemonstratedSkill? = null
)

class SkillLibraryViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(SkillLibraryUiState())
    val uiState: StateFlow<SkillLibraryUiState> = _uiState.asStateFlow()

    init {
        loadSkills()
    }

    fun loadSkills() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val skills = SkillLibrary.getAllSkills(getApplication())
            _uiState.value = _uiState.value.copy(skills = skills, isLoading = false)
        }
    }

    fun selectSkill(skill: DemonstratedSkill?) {
        _uiState.value = _uiState.value.copy(selectedSkill = skill)
    }

    fun deleteSkill(skillId: String) {
        viewModelScope.launch {
            // Future: Implement delete in SkillLibrary
            loadSkills()
        }
    }
}
