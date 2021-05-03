package com.android.signlanguage.model

import com.android.signlanguage.R

object Language {
    private const val firstLetter = 'A'
    val maxLetters
        get() = drawables.size

    private val drawables = arrayListOf(
        R.drawable.letter_a,
        R.drawable.letter_b,
        R.drawable.letter_c,
        R.drawable.letter_d,
        R.drawable.letter_e
    )

    fun getLetter(index: Int): Char  {
        return firstLetter + index
    }

    fun getIndex(letter: Char): Int {
        return letter - firstLetter
    }

    fun getDrawable(letter: Char): Int {
        return drawables[getIndex(letter)]
    }
}