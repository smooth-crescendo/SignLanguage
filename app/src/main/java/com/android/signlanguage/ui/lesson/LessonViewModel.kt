package com.android.signlanguage.ui.lesson

import android.util.Log
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.android.signlanguage.FinishedListener
import com.android.signlanguage.model.Language
import com.android.signlanguage.model.skill.UserSkill
import com.android.signlanguage.ui.lesson.exercises.letter_camera.LetterCameraExerciseFragment
import com.android.signlanguage.ui.lesson.exercises.letter_sign.LetterSignExerciseFragment
import com.android.signlanguage.ui.lesson.exercises.sign_letter.SignLetterExerciseFragment
import com.android.signlanguage.ui.lesson.lesson_finished.LessonFinishedFragment
import com.android.signlanguage.ui.lesson.new_sign.NewSignFragment
import java.util.*

class LessonViewModel(
    var currentScreenChanged: ((lessonScreen: Fragment) -> Unit)? = null
) :
    ViewModel(), FinishedListener {

    companion object {
        private const val TAG = "LessonViewModel"

        const val MIN_SIGNS_FOR_LEARNING = 2
        const val EXERCISES_IN_LESSON = 10

        fun filterExercises(): List<Class<out Any>> {
            return ExerciseConverter.exercises.filter {
                val exercise = ExerciseConverter.extractRules(it)
                UserSkill.requireInstance().unlockedSignsCount >= exercise.unlockedSignsRequired
            }
        }
    }

    private var _currentScreen = MutableLiveData<Fragment>()
    val currentScreen: LiveData<Fragment> = _currentScreen

    private val _progress = MutableLiveData(0)
    val progress: LiveData<Int> = _progress

    private val _finished = MutableLiveData<Boolean?>()
    override val finished: LiveData<Boolean?> = _finished

    private val _delayedScreens = LinkedList<Fragment>()

    private var _doneExercises = 0
    private var _doneExercisesSuccessfully = 0

    private val _userSkill = UserSkill.requireInstance()

    init {
        Log.d(TAG, "init: ")

        startNextScreen()
    }

    fun startNextScreen(prevExerciseFailed: Boolean = false) {

        if (_currentScreen.value != null && _currentScreen.value!! is Exercise) {
            _doneExercises++
            if (!prevExerciseFailed) {
                _doneExercisesSuccessfully++
                _userSkill.upgrade((_currentScreen.value as Exercise).sign)
            }

            Log.d(TAG, "startNextScreen: Skill: $_userSkill")
        }

        if (prevExerciseFailed) {
            _delayedScreens.add(currentScreen.value!!)
        }

        if (currentScreen.value != null && currentScreen.value!!::class.java == LessonFinishedFragment::class.java) {
            _finished.value = true
            return
        }

        val nextScreen = getNextExercise()

        _currentScreen.value = nextScreen
        currentScreenChanged?.invoke(nextScreen)

        _progress.value = calculateProgress()
    }

    private fun getNextExercise(): Fragment {
        if (_doneExercisesSuccessfully >= EXERCISES_IN_LESSON) {
            return LessonFinishedFragment()
        }

        if (_userSkill.unlockedSignsCount < MIN_SIGNS_FOR_LEARNING) {
            val newSign = Language.getLetter(_userSkill.unlockedSignsCount)
            _userSkill.unlockSign(newSign)
            return NewSignFragment.newInstance(newSign)
        }

        if (_doneExercises < EXERCISES_IN_LESSON * 0.6
            && _userSkill.isNewSignReady()) {
            val newSign = Language.getLetter(_userSkill.unlockedSignsCount)
            _userSkill.unlockSign(newSign)
            return NewSignFragment.newInstance(newSign)
        }

        if (_doneExercises >= EXERCISES_IN_LESSON) {
            val nextScreen = _delayedScreens.poll()
            return nextScreen ?: LessonFinishedFragment()
        }

        val sign =
            if (_currentScreen.value != null && _currentScreen.value!! is Exercise) {
                _userSkill.getRandomUnlockedSignExcluding((_currentScreen.value as Exercise).sign)
            } else {
                _userSkill.getRandomUnlockedSign()
            }

        val filteredExercises = filterExercises()

        return when (filteredExercises.random()) {
            LetterCameraExerciseFragment::class.java -> {
                LetterCameraExerciseFragment.newInstance(sign)
            }
            SignLetterExerciseFragment::class.java -> {
                SignLetterExerciseFragment.newInstance(sign)
            }
            LetterSignExerciseFragment::class.java -> {
                LetterSignExerciseFragment.newInstance(sign)
            }
            else -> throw NotImplementedError()
        }
    }

    private fun calculateProgress() =
        (_doneExercisesSuccessfully.toDouble() / EXERCISES_IN_LESSON.toDouble() * 100.0).toInt()

//    private fun countExercises(): Int {
//        var result = _screens.count { EXERCISES.contains(it.javaClass) }
//        currentScreen.value?.let {
//            if (EXERCISES.contains(it.javaClass))
//                result++
//        }
//        return result
//    }
}

class LessonViewModelFactory(
    var currentScreenChanged: ((lessonScreen: Fragment) -> Unit)? = null
) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return LessonViewModel(currentScreenChanged) as T
    }
}