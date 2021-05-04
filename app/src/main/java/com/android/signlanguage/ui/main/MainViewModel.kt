package com.android.signlanguage.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.android.signlanguage.model.skill.UserSkill

class MainViewModel(val userSkill: UserSkill) : ViewModel() {
    var lessonStarted: (() -> Unit)? = null
    var progressReset: (() -> Unit)? = null

    fun startLesson() {
        lessonStarted?.invoke()
    }

    fun resetProgress() {
        userSkill.reset()
        progressReset?.invoke()
    }
}

class MainViewModelFactory(val userSkill: UserSkill) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return MainViewModel(userSkill) as T
    }
}