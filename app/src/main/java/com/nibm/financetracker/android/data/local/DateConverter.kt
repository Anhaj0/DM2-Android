package com.nibm.financetracker.android.data.local

import androidx.room.TypeConverter
import java.util.Date

/**
 * Tells Room how to convert complex types (like Date) into simple types
 * (like Long) that it can store in the SQLite database.
 */
class DateConverter {
    @TypeConverter
    fun toDate(timestamp: Long?): Date? {
        return timestamp?.let { Date(it) }
    }

    @TypeConverter
    fun fromDate(date: Date?): Long? {
        return date?.time
    }
}