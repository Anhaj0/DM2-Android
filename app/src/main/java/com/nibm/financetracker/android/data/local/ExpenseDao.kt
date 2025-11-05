package com.nibm.financetracker.android.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {

    @Insert
    suspend fun insert(expense: Expense): Long

    @Update
    suspend fun update(expense: Expense)

    @Delete
    suspend fun delete(expense: Expense)

    @Query("SELECT * FROM expenses ORDER BY date DESC")
    fun getAllExpenses(): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE isSynced = 0")
    suspend fun getUnsyncedExpenses(): List<Expense>

    // NEW: ensure localExpenseId is populated even if Sync is pressed instantly
    @Query("UPDATE expenses SET localExpenseId = id WHERE localExpenseId = 0")
    suspend fun backfillLocalIds()
}
