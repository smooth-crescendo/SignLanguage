package com.android.signlanguage.model.languages

import com.android.signlanguage.R
import com.android.signlanguage.model.Language

object EnglishLanguage : Language() {
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

    override fun getLetter(index: Int): Char  {
        return firstLetter + index
    }

    override fun getIndex(letter: Char): Int {
        return letter - firstLetter
    }

    override fun getDrawable(letter: Char): Int {
        return drawables[getIndex(letter)]
    }
}