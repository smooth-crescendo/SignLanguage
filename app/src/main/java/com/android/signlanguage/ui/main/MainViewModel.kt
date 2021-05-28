package com.android.signlanguage.ui.main

import androidx.lifecycle.*
import com.android.signlanguage.model.skill.PointsMilestones
import com.android.signlanguage.model.skill.UserSkill

class MainViewModel(val userSkill: UserSkill) : ViewModel() {
    var lessonStarted: (() -> Unit)? = null
    var progressReset: (() -> Unit)? = null

    private val _points = MutableLiveData(userSkill.points)
    val points: LiveData<Int> = _points

    val pointsString: LiveData<String> = Transformations.map(points) {
        it.toString()
    }

    val pointsForNextLevel: LiveData<Int> = Transformations.map(points) {
        PointsMilestones.milestones[PointsMilestones.getClosestIndex(it)!!]
    }
    val pointsForNextLevelString: LiveData<String> = Transformations.map(pointsForNextLevel) {
        " / $it"
    }

    val level: LiveData<Int> = Transformations.map(points) {
        PointsMilestones.getClosestIndex(it)!! + 1
    }
    val levelString: LiveData<String> = Transformations.map(level) {
        it.toString()
    }

    fun startLesson() {
        lessonStarted?.invoke()
    }

    fun resetProgress() {
        userSkill.reset()
        _points.value = userSkill.points
        progressReset?.invoke()
    }

    fun update() {
        _points.value = userSkill.points
    }
}

class MainViewModelFactory(val userSkill: UserSkill) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return MainViewModel(userSkill) as T
    }
}