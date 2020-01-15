package com.github.quarck.minisearch

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.speech.RecognizerIntent
import android.support.v7.app.AppCompatActivity
import android.view.KeyEvent
import kotlinx.android.synthetic.main.activity_search.*
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import android.widget.TextView.OnEditorActionListener



/**
 * A login screen that offers login via email/password.
 */
class SearchActivity : AppCompatActivity()  {

    private val REQUEST_SPEECH_RECOGNIZER = 3000

    private val handler = Handler()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)
        // search_button.setOnClickListener { doSearch() }

        searchQuery.setOnEditorActionListener(
                OnEditorActionListener {
                    _, actionId, _ ->
                    if (actionId == EditorInfo.IME_ACTION_SEND) {
                        doSearch()
                        return@OnEditorActionListener true
                    }
                    false
                })

        reminderText.setOnEditorActionListener(
                OnEditorActionListener {
                    _, actionId, _ ->
                    if (actionId == EditorInfo.IME_ACTION_SEND) {
                        doCreateNote()
                        return@OnEditorActionListener true
                    }
                    false
                })

        imageButtonVoiceTyping.setOnClickListener{
            doCreateVoiceNote()
        }
    }

    override fun onResume() {
        super.onResume()
        searchQuery.text.clear();
        reminderText.text.clear();
    }

    private fun doSearch() {
        val query = searchQuery.text.toString()
        val url = "https://www.google.ie/search?q=" + Uri.encode(query);
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
        this.startActivity(intent)
//        this.finish()
    }

    private fun doCreateNote(text: String) {
        val utils = CalendarUtils(this)
        val start = System.currentTimeMillis() + 30 * 60 * 1000L
        val end = start + 15 * 60 * 1000L
        val eventId = utils.createEvent(text, "#task", start, end, 1)
        if (eventId != -1L) {
            val intent = Intent(this, CreateEventResultActivity::class.java)
            intent.putExtra("text", text)
            startActivity(intent)
            finish()
        }
    }

    private fun doCreateNote() {
        doCreateNote(reminderText.text.toString())
    }

    private fun doCreateVoiceNote() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
//        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, mQuestion)
        startActivityForResult(intent, REQUEST_SPEECH_RECOGNIZER)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_SPEECH_RECOGNIZER) {
            if (resultCode == Activity.RESULT_OK) {
                val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                if (results != null) {
                    doCreateNote(results[0])
                }
            }
        }
    }
}
