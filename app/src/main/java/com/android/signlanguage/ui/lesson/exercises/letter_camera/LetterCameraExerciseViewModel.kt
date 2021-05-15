package com.android.signlanguage.ui.lesson.exercises.letter_camera

import android.util.Log
import android.view.View
import androidx.lifecycle.*
import com.android.signlanguage.FinishedListener
import com.android.signlanguage.model.Language
import com.android.signlanguage.ui.lesson.LessonFragment
import kotlinx.coroutines.launch
import org.tensorflow.lite.Interpreter
import kotlin.random.Random

class LetterCameraExerciseViewModel(sign: Char) : ViewModel(),
    FinishedListener {

    companion object {
        private const val TAG = "LetterCameraExerciseViewModel"
    }

    lateinit var signDetectionModel: Interpreter

    private val _finished = MutableLiveData<Boolean?>()
    override val finished: LiveData<Boolean?> = _finished

    private val _rightAnswer = MutableLiveData<Char>()
    val rightAnswer = Transformations.map(_rightAnswer) { it.toString() }

    init {
        _rightAnswer.value = sign
    }

    val isCameraAccessible = MutableLiveData(false)
    val cameraAccessErrorVisibility = Transformations.map(isCameraAccessible) {
        if (it) View.GONE else View.VISIBLE
    }

    val isLoading = MutableLiveData(false)
    val loadingVisibility = Transformations.map(isLoading) {
        if (it) View.VISIBLE else View.GONE
    }

    private val _continuousSignDetector = ContinuousSignDetector(20, 0.8, 750L)

    init {
        _continuousSignDetector.rightSignDetected = {
            if (finished.value == null) {
                viewModelScope.launch {
                    _finished.value = true
                }
            }
        }
    }

    fun handsCallback(input: Array<Array<FloatArray>>) {
        val output = Array(1) { FloatArray(Language.maxLetters) }
        signDetectionModel.run(input, output)

        var predictedSignIndex = 0
        for (j in 0 until Language.maxLetters) {
            if (output[0][j] > output[0][predictedSignIndex])
                predictedSignIndex = j
        }

        val predictedSign = 'A' + predictedSignIndex
        _continuousSignDetector.addPrediction(
            predictedSign == _rightAnswer.value,
            System.currentTimeMillis()
        )

        Log.d(TAG, "handsCallback: $predictedSign")
    }
}

class LetterCameraExerciseViewModelFactory(val sign: Char) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return LetterCameraExerciseViewModel(sign) as T
    }
}