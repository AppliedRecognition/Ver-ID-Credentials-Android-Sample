package com.appliedrec.credentials.app

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.appliedrec.credentials.app.databinding.ActivityErrorBinding

class ErrorActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val viewBinding = ActivityErrorBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)
        val heading = intent.getStringExtra("heading") ?: "Error"
        val text = intent.getStringExtra("text") ?: "Something went wrong"
        viewBinding.heading.text = heading
        viewBinding.message.text = text
    }
}