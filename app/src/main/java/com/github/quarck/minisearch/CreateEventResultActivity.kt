package com.github.quarck.minisearch

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.view.View

import kotlinx.android.synthetic.main.activity_create_event_result.*

class CreateEventResultActivity : AppCompatActivity() {

    private val NEW_VOICE_REQUEST = 4000
    private var eventId: Long = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_event_result)

        val cont = intent.getStringExtra("text")
        eventId = intent.getLongExtra("eventId", -1L)
        val isVoice = intent.getBooleanExtra("isVoice", false) && (eventId != -1L)

        textViewEventContent.text = cont ?: ""

        buttonRetake.visibility = if (isVoice) View.VISIBLE else View.GONE

        buttonRetake.setOnClickListener {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Note text?")
            startActivityForResult(intent, NEW_VOICE_REQUEST)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == NEW_VOICE_REQUEST) {
            if (resultCode == Activity.RESULT_OK) {
                val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                if (results != null) {
                    updateEventTitle(results[0])
                }
            }
        }
    }

    private fun updateEventTitle(title: String) {
        if (CalendarUtils(this).updateEventTitle(eventId, title)) {
            textViewEventContent.text = title
        }
    }

}
