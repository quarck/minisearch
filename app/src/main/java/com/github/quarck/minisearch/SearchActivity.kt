package com.github.quarck.minisearch

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_search.*

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
        search_button.setOnClickListener { doSearch() }
    }

    override fun onResume() {
        super.onResume()
        searchQuery.text.clear();
    }

    private fun doSearch() {
        val query = searchQuery.text.toString()
        val url = "https://www.google.ie/search?q=" + Uri.encode(query);
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        this.startActivity(intent)
        this.finish()
    }
}
