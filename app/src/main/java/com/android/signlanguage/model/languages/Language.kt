package com.android.signlanguage.model.languages

abstract class Language {
    abstract fun getLetter(index: Int): Char
    abstract fun getIndex(letter: Char): Int
    abstract fun getDrawable(letter: Char): Int
}