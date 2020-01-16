package com.github.quarck.minisearch

import android.app.Activity
import android.app.AlertDialog
import android.app.SearchManager
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.speech.RecognizerIntent
import android.support.v7.app.AppCompatActivity
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.TextView.OnEditorActionListener
import kotlinx.android.synthetic.main.activity_search.*


/**
 * A login screen that offers login via email/password.
 */
class SearchActivity : AppCompatActivity()  {

    private val REQUEST_SPEECH_RECOGNIZER_NOTE = 3000
    private val REQUEST_SPEECH_RECOGNIZER_WEBSEARCH = 3001

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

//        reminderText.setOnEditorActionListener(
//                OnEditorActionListener {
//                    _, actionId, _ ->
//                    if (actionId == EditorInfo.IME_ACTION_SEND) {
//                        doCreateNote()
//                        return@OnEditorActionListener true
//                    }
//                    false
//                })
//
        imageButtonVoiceTyping.setOnClickListener{
            doCreateVoiceNote()
        }

        imageButtonVoiceTypingSearch.setOnClickListener{
            doVoiceSearch()
        }

        imageViewSettings.setOnClickListener{
            onSettings()
        }
    }

    override fun onResume() {
        super.onResume()
        searchQuery.text.clear();
//        reminderText.text.clear();
    }

    fun openGoogleSearchByHack(query: String): Boolean {
        try {
            val intent = Intent(Intent.ACTION_WEB_SEARCH)
            intent.setPackage("com.google.android.googlequicksearchbox")
            intent.putExtra(SearchManager.QUERY, query)
            startActivity(intent)
            return true
        } catch (ex: Exception) {
            return false
        }
    }

    fun openSearchInAWebBrowser(query: String) {
        val url = "https://www.google.ie/search?q=" + Uri.encode(query);
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
        this.startActivity(intent)
    }

    private fun doSearch(query: String) {

//        if (!openGoogleSearchByHack(query)) {
            openSearchInAWebBrowser(query)
  //      }

        this.finish()
    }

    private fun doSearch() {
        val query = searchQuery.text.toString()
        doSearch(query)
    }

    private fun doCreateNote(text: String, isVoice: Boolean) {
        val utils = CalendarUtils(this)
        val start = System.currentTimeMillis() + 30 * 60 * 1000L
        val end = start + 15 * 60 * 1000L
        val eventId = utils.createEvent(Settings(this).calendarToUse, text, "#task", start, end, 1)
        if (eventId != -1L) {
            val intent = Intent(this, CreateEventResultActivity::class.java)
            intent.putExtra("text", text)
            intent.putExtra("isVoice", isVoice)
            intent.putExtra("eventId", eventId)
            startActivity(intent)
            finish()
        }
    }

//    private fun doCreateNote() {
//        doCreateNote(reminderText.text.toString(), false)
//    }

    private fun doCreateVoiceNote() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Note text?")
        startActivityForResult(intent, REQUEST_SPEECH_RECOGNIZER_NOTE)
    }

    private fun doVoiceSearch() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH)
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Search web for?")
        startActivityForResult(intent, REQUEST_SPEECH_RECOGNIZER_WEBSEARCH)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_SPEECH_RECOGNIZER_NOTE) {
            if (resultCode == Activity.RESULT_OK) {
                val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                if (results != null) {
                    doCreateNote(results[0], true)
                }
            }
        } else if (requestCode == REQUEST_SPEECH_RECOGNIZER_WEBSEARCH) {
            if (resultCode == Activity.RESULT_OK) {
                val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                if (results != null) {
                    doSearch(results[0])
                }
            }
        }
    }

    private fun onSettings() {
        val cals = CalendarUtils(this).getCalendars().filter { it.isVisible && it.isSynced && !it.isReadOnly}

        val names = cals.map{ "${it.displayName} (${it.accountName})" }.toTypedArray()
        val ids = cals.map{ it.calendarId }.toTypedArray()

        val builderSingle = AlertDialog.Builder(this)
        builderSingle.setIcon(R.drawable.ic_launcher_foreground)
        builderSingle.setTitle("Select calendar")

        val arrayAdapter = ArrayAdapter<String>(this, android.R.layout.select_dialog_singlechoice, names)

        builderSingle.setNegativeButton("cancel", DialogInterface.OnClickListener { dialog, which -> dialog.dismiss() })

        builderSingle.setAdapter(arrayAdapter) {
            dialog, which ->
            Settings(this).calendarToUse = ids[which]
            dialog.dismiss()
        }

        builderSingle.show()
    }

}
