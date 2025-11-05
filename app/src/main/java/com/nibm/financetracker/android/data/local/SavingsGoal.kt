package com.nibm.financetracker.android.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "savings_goals")
data class SavingsGoal(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,             // local PK

    var name: String,
    var targetAmount: Double,
    var currentAmount: Double = 0.0,
    var targetDate: Date? = null,
    val userId: Long = 1,

    // sync
    var isSynced: Boolean = false,
    var serverId: Long? = null,
    var localId: Long = 0L
)
