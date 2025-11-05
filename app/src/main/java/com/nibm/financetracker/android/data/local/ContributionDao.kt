package com.nibm.financetracker.android.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ContributionDao {

    @Insert
    suspend fun insert(contribution: Contribution): Long

    @Update
    suspend fun update(contribution: Contribution)

    @Delete
    suspend fun delete(contribution: Contribution)

    @Query("SELECT * FROM contributions WHERE isSynced = 0")
    suspend fun getUnsyncedContributions(): List<Contribution>

    @Query("SELECT * FROM contributions WHERE goalId = :goalId ORDER BY date DESC, id DESC")
    fun getByGoal(goalId: Long): Flow<List<Contribution>>

    @Query("DELETE FROM contributions WHERE goalId = :goalId")
    suspend fun deleteByGoalId(goalId: Long)

    @Query("UPDATE contributions SET localId = id WHERE localId = 0")
    suspend fun backfillLocalIds()
}
