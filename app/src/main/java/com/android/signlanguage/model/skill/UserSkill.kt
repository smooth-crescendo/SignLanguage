package com.android.signlanguage.model.skill

import android.content.Context
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import java.io.File

class UserSkill {
    private val _unlockedSigns: MutableList<SignSkill> = mutableListOf()
    val unlockedSignsCount
        get() = _unlockedSigns.size

    fun unlockSign(sign: Char, skill: Double = 0.0) {
        if (!_unlockedSigns.contains(sign))
            _unlockedSigns += SignSkill(sign, skill)
    }

    fun reset() {
        _unlockedSigns.clear()
    }

    companion object {
        private const val FILENAME = "user_skill"

        private var instance: UserSkill? = null

        fun getInstance(context: Context): UserSkill {
            if (instance == null) {
                instance = read(context)
            }
            return instance!!
        }

        private fun read(context: Context): UserSkill {
            val file = File(context.filesDir, FILENAME)
            return if (file.exists()) {
                val content = file.readText()

                val moshi = Moshi.Builder().build()
                val jsonAdapter: JsonAdapter<UserSkill> =
                    moshi.adapter(UserSkill::class.java)

                jsonAdapter.fromJson(content)!!
            } else {
                val userSkill = UserSkill()
                userSkill
            }
        }

        fun save(context: Context) {
            val file = File(context.filesDir, FILENAME)
            if (!file.exists()) {
                file.createNewFile()
            }

            val moshi = Moshi.Builder().build()
            val jsonAdapter: JsonAdapter<UserSkill> =
                moshi.adapter(UserSkill::class.java)

            val content = jsonAdapter.toJson(getInstance(context))
            file.writeText(content)
        }
    }
}