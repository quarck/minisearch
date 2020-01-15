package com.github.quarck.minisearch

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import android.util.Log
import java.util.*


data class CalendarRecord(
        val calendarId: Long,
        val owner: String,
        val displayName: String,
        val name: String,
        val accountName: String,
        val accountType: String,
        val timeZone: String,
        val color: Int,
        val isVisible: Boolean,
        val isPrimary: Boolean,
        val isReadOnly: Boolean,
        val isSynced: Boolean
)


class CalendarUtils(val ctx: Context) {

    fun getCalendars(): List<CalendarRecord> {

        val ret = mutableListOf<CalendarRecord>()

        try {

            val fields = mutableListOf(
                    CalendarContract.Calendars._ID,
                    CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
                    CalendarContract.Calendars.NAME,
                    CalendarContract.Calendars.OWNER_ACCOUNT,
                    CalendarContract.Calendars.ACCOUNT_NAME,
                    CalendarContract.Calendars.ACCOUNT_TYPE,
                    CalendarContract.Calendars.CALENDAR_COLOR,

                    CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL,
                    CalendarContract.Calendars.CALENDAR_TIME_ZONE,
                    CalendarContract.Calendars.SYNC_EVENTS,
                    CalendarContract.Calendars.VISIBLE
            )

            fields.add(CalendarContract.Calendars.IS_PRIMARY)

            val uri = CalendarContract.Calendars.CONTENT_URI

            val cursor = ctx.contentResolver.query(
                    uri, fields.toTypedArray(), null, null, null)

            while (cursor != null && cursor.moveToNext()) {

                // Get the field values
                val calID: Long? = cursor.getLong(0)
                val displayName: String? = cursor.getString(1)
                val name: String? = cursor.getString(2)
                val ownerAccount: String? = cursor.getString(3)
                val accountName: String? = cursor.getString(4)
                val accountType: String? = cursor.getString(5)
                val color: Int? = cursor.getInt(6)
                val accessLevel: Int? = cursor.getInt(7)
                val timeZone: String? = cursor.getString(8)
                val syncEvents: Int? = cursor.getInt(9)
                val visible: Int? = cursor.getInt(10)

                val isPrimary: Int? = cursor.getInt(11)

                val isEditable =
                        when(accessLevel ?: 0) {
                            CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR -> true
                            CalendarContract.Calendars.CAL_ACCESS_OWNER -> true
                            CalendarContract.Calendars.CAL_ACCESS_ROOT -> true
                            else -> false
                        }

                ret.add(CalendarRecord(
                        calendarId = calID ?: -1L,
                        owner = ownerAccount ?: "",
                        accountName = accountName ?: "",
                        accountType = accountType ?: "",
                        displayName = displayName ?: "",
                        name = name ?: "",
                        color = color ?: 0,
                        isPrimary = (isPrimary ?: 0) != 0,
                        isReadOnly = !isEditable,
                        isVisible = (visible ?: 0) != 0,
                        isSynced = (syncEvents ?: 0) != 0,
                        timeZone = timeZone ?: TimeZone.getDefault().getID()
                ))
            }

            cursor?.close()

        }
        catch (ex: Exception) {
            Log.e("", "Exception while reading list of calendars: ${ex}")
        }

        return ret
    }

    fun createEvent(title: String, text: String, startTime: Long, endTime: Long, reminderMinutesBefore: Long) : Long {

        var eventId = -1L

        var calendars = getCalendars().filter { !it.isReadOnly && it.isSynced && it.isVisible}
        if (calendars.size > 1) {
            val cal2 = calendars.filter { it.isPrimary }
            if (cal2.size > 0) {
                calendars = cal2
            }
        }

        val calendar = calendars[0]

        val values = ContentValues()

        values.put(CalendarContract.Events.TITLE, title)
        values.put(CalendarContract.Events.CALENDAR_ID, calendar.calendarId)
        values.put(CalendarContract.Events.EVENT_TIMEZONE, calendar.timeZone) // Irish summer time
        values.put(CalendarContract.Events.DESCRIPTION, text)

        values.put(CalendarContract.Events.DTSTART, startTime)
        values.put(CalendarContract.Events.DTEND, endTime)

        values.put(CalendarContract.Events.EVENT_LOCATION, "")

        values.put(CalendarContract.Events.ACCESS_LEVEL, CalendarContract.Events.ACCESS_DEFAULT)
        values.put(CalendarContract.Events.AVAILABILITY, CalendarContract.Events.AVAILABILITY_BUSY)

        values.put(CalendarContract.Events.HAS_ALARM, 1)
        values.put(CalendarContract.Events.ALL_DAY, 0)


        values.put(CalendarContract.Events.STATUS, CalendarContract.Events.STATUS_CONFIRMED)
        values.put(CalendarContract.Events.SELF_ATTENDEE_STATUS, CalendarContract.Events.STATUS_CONFIRMED)

        // https://gist.github.com/mlc/5188579
        values.put(CalendarContract.Events.ORGANIZER, calendar.owner)
        values.put(CalendarContract.Events.HAS_ATTENDEE_DATA, 1)

        try {
            val uri = ctx.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)

            // get the event ID that is the last element in the Uri
            eventId = uri?.lastPathSegment?.toLong() ?: 0L
        }
        catch (ex: Exception) {
            Log.e("", "Exception while adding new event: ${ex}")
        }

        if (eventId != -1L) {
            val reminderValues = ContentValues()
            reminderValues.put(CalendarContract.Reminders.MINUTES, reminderMinutesBefore.toInt())

            reminderValues.put(CalendarContract.Reminders.EVENT_ID, eventId)

            reminderValues.put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_DEFAULT)

            ctx.contentResolver.insert(
                    CalendarContract.Reminders.CONTENT_URI,
                    reminderValues
            )
        }

        return eventId
    }

    private fun intentForAction(action: String, eventId: Long, startTime: Long, endTime: Long): Intent {

        val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId);
        val intent = Intent(action).setData(uri)
        intent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startTime)
        intent.putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endTime)

        return intent
    }

    fun calendarViewIntent(eventId: Long, startTime: Long, endTime: Long)
            = intentForAction(Intent.ACTION_VIEW, eventId, startTime, endTime)

    fun viewCalendarEvent(eventId: Long, startTime: Long, endTime: Long)
            = ctx.startActivity(intentForAction(Intent.ACTION_VIEW, eventId, startTime, endTime))

    fun updateEventTitle(eventId: Long, title: String) : Boolean {
        var ret = false
        try {
            val values = ContentValues()

            values.put(CalendarContract.Events.TITLE, title)
            val updateUri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
            ret = ctx.contentResolver.update(updateUri, values, null, null) > 0
        }
        catch (ex: Exception) {
            Log.e("", "Exception while reading calendar event: ${ex}")
        }

        return ret
    }

    companion object {
        const val MINUTE_IN_MILLISECONDS: Long = 60L * 1000L
    }
}