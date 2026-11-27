package com.example.myapplication

import android.content.Context

object AppKeyStore {
    private const val PREFS = "multiai_provider_keys"
    private lateinit var appContext: Context
    fun initialize(context: Context) { appContext = context.applicationContext }
    fun get(key: String): String = if (::appContext.isInitialized) appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(key, "").orEmpty() else ""
    fun set(key: String, value: String) { if (::appContext.isInitialized) appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(key, value.trim()).apply() }
}
