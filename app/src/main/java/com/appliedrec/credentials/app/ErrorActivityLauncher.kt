package com.appliedrec.credentials.app

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity

fun AppCompatActivity.showError(heading: String, message: String) {
    val intent = Intent(this, ErrorActivity::class.java)
    intent.putExtra("heading", heading)
    intent.putExtra("text", message)
    startActivity(intent)
}