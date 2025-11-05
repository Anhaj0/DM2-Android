package com.nibm.financetracker.android.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,              // Local-only primary key

    var description: String,
    var amount: Double,
    var categoryId: Long,
    var date: Date,

    // --- Syncing fields ---
    var isSynced: Boolean = false,
    var serverId: Long? = null,
    var localExpenseId: Long = 0L, // THIS IS THE FIX (was = id)

    // For API
    val userId: Long = 1
)