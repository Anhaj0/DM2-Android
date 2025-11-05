package com.nibm.financetracker.android.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budgets")
data class Budget(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    var categoryId: Long,         // LOCAL category ID
    var amountLimit: Double,
    var month: Int,
    var year: Int,
    val userId: Long = 1,

    // --- Sync fields ---
    var isSynced: Boolean = false,
    var serverId: Long? = null,
    var localId: Long = 0L        // <- make this mutable (var)
)
