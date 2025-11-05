package com.nibm.financetracker.android.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SavingsGoalDao {

    @Insert
    suspend fun insert(goal: SavingsGoal): Long

    @Update
    suspend fun update(goal: SavingsGoal)

    @Delete
    suspend fun delete(goal: SavingsGoal)

    @Query("SELECT * FROM savings_goals ORDER BY id DESC")
    fun getAllGoals(): Flow<List<SavingsGoal>>

    @Query("SELECT * FROM savings_goals WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): SavingsGoal?

    @Query("SELECT * FROM savings_goals")
    suspend fun getAllOnce(): List<SavingsGoal>

    @Query("SELECT * FROM savings_goals WHERE isSynced = 0")
    suspend fun getUnsyncedGoals(): List<SavingsGoal>

    @Query("UPDATE savings_goals SET localId = id WHERE localId = 0")
    suspend fun backfillLocalIds()

    @Query("DELETE FROM savings_goals WHERE id = :id")
    suspend fun deleteById(id: Long)
}
