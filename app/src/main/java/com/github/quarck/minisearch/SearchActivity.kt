package com.github.quarck.minisearch

import android.app.Activity
import android.app.AlertDialog
import android.app.SearchManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.speech.RecognizerIntent
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.TextView.OnEditorActionListener
import kotlinx.android.synthetic.main.activity_search.*
import android.text.Editable
import android.text.TextWatcher
import android.support.v4.app.SupportActivity
import android.support.v4.app.SupportActivity.ExtraData
import android.support.v4.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import android.text.format.DateFormat
import android.text.format.DateUtils
import android.util.Log


/**
 * A login screen that offers login via email/password.
 */
class SearchActivity : AppCompatActivity(), SensorEventListener {

    private val REQUEST_SPEECH_RECOGNIZER_NOTE = 3000
    private val REQUEST_SPEECH_RECOGNIZER_WEBSEARCH = 3001

    private val handler = Handler()

    private lateinit var sensorManager: SensorManager
    private var pressureSensor: Sensor? = null
    private var accelerometer: Sensor? = null

    private var lastGFactor: Double = 0.0
    private var lastPressure: Double = 0.0

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

        searchQuery.addTextChangedListener(object : TextWatcher {

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                imageButtonVoiceTypingSearch.visibility =
                        if (searchQuery.text.isNotBlank()) View.GONE else View.VISIBLE
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            }

            override fun afterTextChanged(s: Editable) {
            }
        })


        noteText.setOnEditorActionListener(
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

        imageButtonVoiceTypingSearch.setOnClickListener{
            doVoiceSearch()
        }

        imageViewSettings.setOnClickListener{
            onSettings()
        }

        textViewDate.text =
                DateUtils.formatDateTime(
                        this, System.currentTimeMillis(),
                        DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_WEEKDAY
                                or DateUtils.FORMAT_SHOW_YEAR)


        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        pressureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        if (pressureSensor == null) {

            val list = sensorManager.getSensorList(Sensor.TYPE_PRESSURE)
            if (list.size > 0)
                pressureSensor = list[0]

            if (pressureSensor == null) {
                textViewPressure.visibility = View.GONE
                Log.i("", "No pressure sensor!!")
            }
        }
    }

    override fun onResume() {
        super.onResume()
        searchQuery.text.clear()
        noteText.text.clear()
        imageButtonVoiceTypingSearch.visibility = View.VISIBLE

        pressureSensor?.apply {
            sensorManager.registerListener(this@SearchActivity, this, SensorManager.SENSOR_DELAY_NORMAL)
        }

        accelerometer?.apply {
            sensorManager.registerListener(this@SearchActivity, this, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }


    fun openSearchInAWebBrowser(query: String) {
        val url = "https://www.google.ie/search?q=" + Uri.encode(query);
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
        this.startActivity(intent)
    }

    private fun doSearch(query: String) {
        openSearchInAWebBrowser(query)
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

    private fun doCreateNote() {
        doCreateNote(noteText.text.toString(), false)
    }

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



    // Sensor Listener
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }

    // Sensor Listener
    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null)
            return

        when (event.sensor.type) {
            Sensor.TYPE_PRESSURE ->
                onPressure(event.values[0].toDouble())
            Sensor.TYPE_ACCELEROMETER ->
                if (event.values.size == 3)
                    onAccel(event.values[0], event.values[1], event.values[2])
        }
//        val lux = event.values[0]

    }

    private fun onAccel(x: Float, y: Float, z: Float) {
        val summary = Math.sqrt((x*x + y*y + z*z).toDouble())
        val gVal = SensorManager.STANDARD_GRAVITY.toDouble()
        val gFactor = Math.round(summary / gVal * 1000.0) / 1000.0
        val gFactorWithKalman = 0.3 * gFactor + 0.7 * lastGFactor

        if (gFactorWithKalman != lastGFactor) {
            lastGFactor = gFactorWithKalman
            runOnUiThread{  
                textViewAccel.text = String.format("%1.2f g", lastGFactor)
            }
        }
    }

    private fun onPressure(x: Double) {
        val atmopshere = SensorManager.PRESSURE_STANDARD_ATMOSPHERE.toDouble()
        val pressure = Math.round(x / atmopshere * 1000.0 ) / 1000.0
        val pressureWithKalman = 0.3 * pressure + 0.7 * lastPressure

        if (pressureWithKalman != lastPressure) {
            lastPressure = pressureWithKalman
            runOnUiThread{
                textViewPressure.text = String.format("%1.2f atm", lastPressure)
            }
        }
    }


}
