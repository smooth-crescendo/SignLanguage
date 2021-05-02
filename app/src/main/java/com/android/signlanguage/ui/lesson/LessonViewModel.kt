package com.android.signlanguage.ui.lesson

import android.util.Log
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.android.signlanguage.FinishedListener
import com.android.signlanguage.ui.lesson.exercises.letter_camera.LetterCameraExerciseFragment
import com.android.signlanguage.ui.lesson.exercises.letter_sign.LetterSignExerciseFragment
import com.android.signlanguage.ui.lesson.exercises.sign_letter.SignLetterExerciseFragment
import java.util.*

class LessonViewModel(var currentScreenChanged: ((lessonScreen: Fragment) -> Unit)? = null) :
    ViewModel(), FinishedListener {

    companion object {
        private const val TAG = "LessonViewModel"

        private val EXERCISES = arrayListOf(
            LetterSignExerciseFragment::class.java,
            SignLetterExerciseFragment::class.java,
            LetterCameraExerciseFragment::class.java
        )
    }

    private var _currentScreen = MutableLiveData<Fragment>()
    val currentScreen: LiveData<Fragment> = _currentScreen

    private val _finished = MutableLiveData(false)
    override val finished: LiveData<Boolean> = _finished

    private val _screens = LinkedList<Fragment>()

    init {
        Log.d(TAG, "init: ")

        _screens.add(NewSignFragment.newInstance('A'))

        for (i in 0 until 4) {
            val newExercise = EXERCISES[(Random().nextInt(EXERCISES.size))]
            // FOR TEST PURPOSES
            // val newExercise = LetterSignExerciseFragment::class.java
            val f = newExercise.newInstance() as Fragment
            _screens.add(f)
        }

        _screens.add(LessonFinishedFragment())

        startNextScreen()
    }

    fun startNextScreen() {
        Log.d(TAG, "startNextScreen: ")
        val nextScreen = _screens.poll()
        if (nextScreen == null) {
            _finished.value = true
        } else {
            _currentScreen.value = nextScreen
            currentScreenChanged?.invoke(nextScreen)
        }
    }
}

class LessonViewModelFactory(var currentScreenChanged: ((lessonScreen: Fragment) -> Unit)? = null) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return LessonViewModel(currentScreenChanged) as T
    }
}