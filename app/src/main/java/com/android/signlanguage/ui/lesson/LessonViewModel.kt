package com.android.signlanguage.ui.lesson

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.android.signlanguage.FinishedListener
import com.android.signlanguage.ui.lesson.exercises.Exercise
import java.util.*

class LessonViewModel : ViewModel(), FinishedListener {
    private var _currentExercise = MutableLiveData<Exercise>()
    val currentExercise: LiveData<Exercise> = _currentExercise

    private val _finished = MutableLiveData(false)
    override val finished: LiveData<Boolean> = _finished

    private val _exercises = LinkedList<Exercise>()

    init {
        for (i in 0 until 10) {
            val newExercise = Exercise.values()[(Random().nextInt(Exercise.values().size))]
            _exercises.add(newExercise)
        }

        startNextExercise()
    }

    fun startNextExercise() {
        val nextExercise = _exercises.poll()
        if (nextExercise == null) {
            _finished.value = true
        } else {
            _currentExercise.value = nextExercise
        }
    }
}