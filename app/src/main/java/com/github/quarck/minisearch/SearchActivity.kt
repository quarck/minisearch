package com.github.quarck.minisearch

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
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
    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
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

    private fun doCreateNote() {
        val text = reminderText.text.toString()
        val intent = Intent(Intent.ACTION_MAIN)
        intent.setComponent(ComponentName("com.github.quarck.cnlight", "com.github.quarck.calnotify.ui.CreateNoteActivity"))
        intent.putExtra("voice", false)
        intent.putExtra("text", text)
        intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
        this.startActivity(intent)
    }

    private fun doCreateVoiceNote() {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.setComponent(ComponentName("com.github.quarck.cnlight", "com.github.quarck.calnotify.ui.CreateNoteActivity"))
        intent.putExtra("voice", true)
        intent.putExtra("text", "")
        intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
        this.startActivity(intent)
    }
}
