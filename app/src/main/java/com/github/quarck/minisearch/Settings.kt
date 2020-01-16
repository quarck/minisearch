package com.github.quarck.minisearch

import android.content.Context

class Settings(context: Context) : PersistentStorageBase(context) {
    var calendarToUse: Long
        get() = getLong("cal", -1L)
        set(value) = setLong("cal", value)
}