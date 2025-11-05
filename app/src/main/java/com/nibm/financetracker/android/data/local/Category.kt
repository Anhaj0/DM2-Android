package com.nibm.financetracker.android.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    var name: String,

    // --- Syncing fields ---
    var isSynced: Boolean = false,
    var serverId: Long? = null,
    var localId: Long = 0L // THIS IS THE FIX (was = id)
)