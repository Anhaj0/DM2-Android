package com.nibm.financetracker.android.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {

    @Insert
    suspend fun insert(budget: Budget): Long

    @Update
    suspend fun update(budget: Budget)

    @Delete
    suspend fun delete(budget: Budget)

    @Query("SELECT * FROM budgets ORDER BY year DESC, month DESC")
    fun getAllBudgets(): Flow<List<Budget>>

    @Query("""
        SELECT * FROM budgets 
        WHERE month = :month AND year = :year
        ORDER BY id DESC
    """)
    fun getBudgetsForMonth(month: Int, year: Int): Flow<List<Budget>>

    @Query("""
        SELECT * FROM budgets 
        WHERE categoryId = :categoryId AND month = :month AND year = :year
        LIMIT 1
    """)
    suspend fun findExisting(categoryId: Long, month: Int, year: Int): Budget?

    @Query("SELECT * FROM budgets WHERE isSynced = 0")
    suspend fun getUnsyncedBudgets(): List<Budget>

    // NEW: ensure localId is populated before sync
    @Query("UPDATE budgets SET localId = id WHERE localId = 0")
    suspend fun backfillLocalIds()
}
