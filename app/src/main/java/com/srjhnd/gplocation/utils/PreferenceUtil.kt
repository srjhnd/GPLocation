@file:Suppress("DEPRECATION")

package com.srjhnd.gplocation.utils

import android.content.Context
import android.preference.PreferenceManager.getDefaultSharedPreferences
import android.util.Log
import com.google.gson.Gson
import com.srjhnd.gplocation.data.Location


object SharedPrefsUtils {

    private const val PREF_LOCATION_KEY = "KEY_LOCATION"

    fun putSignifiedLocation(context: Context?, value: Location): Boolean {
        val preferences = getDefaultSharedPreferences(context)
        if (preferences != null) {
            val editor = preferences.edit()
            val gson = Gson()
            val json = gson.toJson(value)
            editor.putString(PREF_LOCATION_KEY, json)
            return editor.commit()
        }
        return false
    }

    fun getSavedLocation(context: Context?): Location? {
        var value: Location? = null
        val preference = getDefaultSharedPreferences(context)
        if (preference != null) {
            val gson = Gson()
            val json = preference.getString(PREF_LOCATION_KEY, "")
            value = gson.fromJson(json, Location::class.java)
        }
        return value
    }
}