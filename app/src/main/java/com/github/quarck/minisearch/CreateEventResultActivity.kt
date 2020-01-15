package com.github.quarck.minisearch

import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity

import kotlinx.android.synthetic.main.activity_create_event_result.*

class CreateEventResultActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_event_result)

        val cont = intent.getStringExtra("text")

        textViewEventContent.text = cont ?: ""
    }

}
