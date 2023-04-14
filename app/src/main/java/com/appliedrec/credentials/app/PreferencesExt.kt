package com.appliedrec.credentials.app

import android.content.SharedPreferences

object PreferenceKeys {
    const val USE_BACK_CAMERA = "useBackCamera"
    const val ENABLE_ACTIVE_LIVENESS = "enableActiveLiveness"
    const val ENABLE_DOCUMENT_VERIFICATION = "enableDocumentVerification"
}

var SharedPreferences.useBackCamera: Boolean
    get() = this.getBoolean(PreferenceKeys.USE_BACK_CAMERA, false)
    set(value) = this.edit().putBoolean(PreferenceKeys.USE_BACK_CAMERA, value).apply()

var SharedPreferences.enableActiveLiveness: Boolean
    get() = this.getBoolean(PreferenceKeys.ENABLE_ACTIVE_LIVENESS, false)
    set(value) = this.edit().putBoolean(PreferenceKeys.ENABLE_ACTIVE_LIVENESS, value).apply()

var SharedPreferences.enableDocumentVerification: Boolean
    get() = this.getBoolean(PreferenceKeys.ENABLE_DOCUMENT_VERIFICATION, false)
    set(value) = this.edit().putBoolean(PreferenceKeys.ENABLE_DOCUMENT_VERIFICATION, value).apply()