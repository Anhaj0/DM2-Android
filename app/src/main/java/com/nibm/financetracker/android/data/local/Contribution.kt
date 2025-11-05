package com.nibm.financetracker.android.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "contributions")
data class Contribution(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,            // local PK

    var goalId: Long,             // local SavingsGoal.id
    var amount: Double,
    var date: Date = Date(),

    // sync
    var isSynced: Boolean = false,
    var serverId: Long? = null,   // not used (API doesn't return), kept for symmetry
    var localId: Long = 0L
)
