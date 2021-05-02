package com.android.signlanguage.ui.main

import androidx.lifecycle.ViewModel

class MainViewModel : ViewModel() {
    var lessonStarted: (() -> Unit)? = null

    fun startLesson() {
        lessonStarted?.invoke()
    }
}