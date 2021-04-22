package com.android.signlanguage.ui.lesson.exercises.letter_camera

import java.util.*

class ContinuousSignDetector(
    private val _maxPredictions: Int,
    private val _correctSignPercentageFilter: Double,
    private val _resetTriggerDuration: Long
) {
    private var _predictions = LinkedList<Boolean>()
    private var _rightPredictions = 0
    private var _lastPredictionTime: Long = 0 // ms

    var rightSignDetected: (() -> Unit)? = null

    fun addPrediction(isRight: Boolean, time: Long) {
        if (_lastPredictionTime != 0L && time - _lastPredictionTime > _resetTriggerDuration) {
            reset()
        }
        _predictions.add(isRight)
        if (isRight)
            _rightPredictions++
        _lastPredictionTime = time

        if (_predictions.size > _maxPredictions) {
            if (_predictions.poll() == true) {
                _rightPredictions--
            }
        }

        val correctSignPercentage = _rightPredictions / _predictions.size
        if (_predictions.size == _maxPredictions && correctSignPercentage >= _correctSignPercentageFilter) {
            rightSignDetected?.invoke()
        }
    }

    private fun reset() {
        _predictions.clear()
        _rightPredictions = 0
        _lastPredictionTime = 0
    }
}