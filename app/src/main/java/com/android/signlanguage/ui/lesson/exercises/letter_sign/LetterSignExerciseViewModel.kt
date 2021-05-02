package com.android.signlanguage.ui.lesson.exercises.letter_sign

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.android.signlanguage.FinishedListener
import com.android.signlanguage.ui.lesson.LessonFragment
import kotlin.random.Random

class LetterSignExerciseViewModel : ViewModel(), FinishedListener {
    companion object {
        private const val TAG = "LetterSignExerciseViewModel"
        private const val POSSIBLE_ANSWERS = 2
    }

    private val _finished = MutableLiveData(false)
    override val finished: LiveData<Boolean> = _finished

    private val _rightAnswerIndex: Int

    private var _possibleAnswers: List<MutableLiveData<Char>> = List(POSSIBLE_ANSWERS) { MutableLiveData() }

    init {
        Log.d(TAG, "init: ")
        val rightSign = 'A' + Random.nextInt(LessonFragment.signsDictionary.size)
        _possibleAnswers[0].value = rightSign
        for (i in 1 until POSSIBLE_ANSWERS) {
            var nextPossibleSign: Char
            do {
                nextPossibleSign = 'A' + Random.nextInt(LessonFragment.signsDictionary.size)
            } while (_possibleAnswers.indexOfFirst { it.value == nextPossibleSign } != -1)
            _possibleAnswers[i].value = nextPossibleSign
        }
        _possibleAnswers = _possibleAnswers.shuffled()
        _rightAnswerIndex = _possibleAnswers.indexOfFirst { it.value == rightSign}
    }

    val possibleAnswer1: LiveData<Char> = _possibleAnswers[0]
    val possibleAnswer2: LiveData<Char> = _possibleAnswers[1]

    val rightAnswer = Transformations.map(_possibleAnswers[_rightAnswerIndex]) {
        it.toString()
    }

    fun answer(signIndex: Int) {
        Log.d(TAG, "answer: ")
        if (signIndex == _rightAnswerIndex)
            _finished.value = true
    }
}