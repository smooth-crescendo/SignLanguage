package com.android.signlanguage.ui.lesson

import android.util.Log
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.android.signlanguage.FinishedListener
import com.android.signlanguage.model.languages.EnglishLanguage
import com.android.signlanguage.model.skill.SignSkill
import com.android.signlanguage.model.skill.UserSkill
import com.android.signlanguage.ui.lesson.exercises.letter_camera.LetterCameraExerciseFragment
import com.android.signlanguage.ui.lesson.exercises.letter_sign.LetterSignExerciseFragment
import com.android.signlanguage.ui.lesson.exercises.sign_letter.SignLetterExerciseFragment
import com.android.signlanguage.ui.lesson.lesson_finished.LessonFinishedFragment
import com.android.signlanguage.ui.lesson.new_sign.NewSignFragment
import java.util.*

class LessonViewModel(
    var currentScreenChanged: ((lessonScreen: Fragment) -> Unit)? = null,
    val userSkill: UserSkill
) :
    ViewModel(), FinishedListener {

    companion object {
        private const val TAG = "LessonViewModel"

        private val EXERCISES = arrayListOf(
            LetterSignExerciseFragment::class.java,
            SignLetterExerciseFragment::class.java,
            LetterCameraExerciseFragment::class.java
        )

        private val LANGUAGE = EnglishLanguage
    }

    private var _currentScreen = MutableLiveData<Fragment>()
    val currentScreen: LiveData<Fragment> = _currentScreen

    private val exercisesCount: Int
    private val _progress = MutableLiveData(0)
    val progress: LiveData<Int> = _progress

    private val _finished = MutableLiveData(false)
    override val finished: LiveData<Boolean> = _finished

    private val _screens = LinkedList<Fragment>()

    init {
        Log.d(TAG, "init: ")

        while (userSkill.languages[0].unlockedSigns.size < 4) {
            val newSign = LANGUAGE.getLetter(userSkill.languages[0].unlockedSigns.size)
            userSkill.languages[0].unlockedSigns.add(SignSkill(newSign, 0.0))
            _screens.add(NewSignFragment.newInstance(newSign))
        }

        for (i in 0 until 8) {
            val newExercise = EXERCISES[(Random().nextInt(EXERCISES.size))]
            // FOR TEST PURPOSES
            // val newExercise = LetterSignExerciseFragment::class.java
            val f = when (newExercise) {
                LetterCameraExerciseFragment::class.java -> {
                    val sign = LANGUAGE.getLetter(Random().nextInt(LANGUAGE.maxLetters))
                    LetterCameraExerciseFragment.newInstance(sign)
                }
                SignLetterExerciseFragment::class.java -> {
                    val sign = LANGUAGE.getLetter(Random().nextInt(LANGUAGE.maxLetters))
                    SignLetterExerciseFragment.newInstance(sign)
                }
                LetterSignExerciseFragment::class.java -> {
                    val sign = LANGUAGE.getLetter(Random().nextInt(LANGUAGE.maxLetters))
                    LetterSignExerciseFragment.newInstance(sign)
                }
                else -> newExercise.newInstance() as Fragment
            }
            _screens.add(f)
        }

        _screens.add(LessonFinishedFragment())

        exercisesCount = countExercises()

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
        _progress.value = calculateProgress()
        Log.d(TAG, "startNextScreen: ${_progress.value}")
    }

    private fun calculateProgress() =
        (100.0 - countExercises().toDouble() / exercisesCount.toDouble() * 100.0).toInt()

    private fun countExercises(): Int {
        var result = _screens.count { EXERCISES.contains(it.javaClass) }
        currentScreen.value?.let {
            if (EXERCISES.contains(it.javaClass))
                result++
        }
        return result
    }
}

class LessonViewModelFactory(
    var currentScreenChanged: ((lessonScreen: Fragment) -> Unit)? = null,
    val userSkill: UserSkill
) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return LessonViewModel(currentScreenChanged, userSkill) as T
    }
}