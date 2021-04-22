package com.android.signlanguage.ui.lesson.exercises.letter_camera

import androidx.lifecycle.*
import com.android.signlanguage.FinishedListener
import com.android.signlanguage.ui.lesson.LessonFragment
import kotlinx.coroutines.launch
import org.tensorflow.lite.Interpreter
import kotlin.random.Random

class LetterCameraExerciseViewModel() : ViewModel(),
    FinishedListener {

    companion object {
        private const val TAG = "LetterCameraExerciseViewModel"
    }

    lateinit var signDetectionModel: Interpreter

    private val _finished = MutableLiveData(false)
    override val finished: LiveData<Boolean> = _finished

    private val _rightAnswer = MutableLiveData('A' + Random.nextInt(LessonFragment.maxSigns))
    val rightAnswer = Transformations.map(_rightAnswer) { it.toString() }

    private val _continuousSignDetector = ContinuousSignDetector(20, 0.8, 750L)

    init {
        _continuousSignDetector.rightSignDetected = {
            if (finished.value == false) {
                viewModelScope.launch {
                    _finished.value = true
                }
            }
        }
    }

    fun handsCallback(input: Array<Array<FloatArray>>) {
        val output = Array(1) { FloatArray(5) }
        signDetectionModel.run(input, output)

        var predictedSignIndex = 0
        for (j in 0..4) {
            if (output[0][j] > output[0][predictedSignIndex])
                predictedSignIndex = j
        }

        val predictedSign = 'A' + predictedSignIndex
        _continuousSignDetector.addPrediction(
            predictedSign == _rightAnswer.value,
            System.currentTimeMillis()
        )
    }
}