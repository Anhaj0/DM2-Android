package com.nibm.financetracker.android.data.network

import com.nibm.financetracker.android.data.local.Budget
import com.nibm.financetracker.android.data.local.Category
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiService {

    // ----- Expenses -----
    @POST("/api/expenses")
    suspend fun createExpense(@Body expense: ExpenseRequest): Response<ExpenseResponse>

    // ----- Categories -----
    @POST("/api/categories/sync")
    suspend fun syncCategory(@Body request: CategoryRequest): Response<Category>

    // ----- Budgets -----
    @POST("/api/budgets/sync")
    suspend fun syncBudget(@Body request: BudgetRequest): Response<Budget>

    // ----- Savings Goals -----
    @POST("/api/savings")
    suspend fun createSavingsGoal(@Body request: SavingsGoalRequest): Response<SavingsGoalServer>

    @POST("/api/savings/{id}/contribute")
    suspend fun addContribution(
        @Path("id") serverGoalId: Long,
        @Body request: ContributionRequest
    ): Response<SavingsGoalServer>

    @GET("/api/savings")
    suspend fun listSavingsGoals(): Response<List<SavingsGoalServer>>

    @DELETE("/api/savings/{id}")
    suspend fun deleteSavingsGoal(@Path("id") serverGoalId: Long): Response<Unit>
}

// --- DTOs ---

data class ExpenseRequest(
    val description: String,
    val amount: Double,
    val categoryId: Long,  // SERVER category id
    val userId: Long,
    val expenseDate: String,
    val localExpenseId: Long
)

data class ExpenseResponse(
    val id: Long,
    val description: String,
    val amount: Double,
    val categoryId: Long,
    val userId: Long,
    val expenseDate: String,
    val localExpenseId: Long
)

data class CategoryRequest(
    val localId: Long,
    val name: String
)

data class BudgetRequest(
    val localId: Long,
    val categoryId: Long, // SERVER category id
    val amountLimit: Double,
    val month: Int,
    val year: Int,
    val userId: Long
)

// Savings
data class SavingsGoalRequest(
    val name: String,
    val targetAmount: Double,
    val targetDate: String?,   // "yyyy-MM-dd" or null
    val userId: Long
)

data class ContributionRequest(
    val amount: String // backend expects stringifiable BigDecimal; e.g. "12.34"
)

data class SavingsGoalServer(
    val id: Long,
    val userId: Long,
    val name: String,
    val targetAmount: Double,
    val currentAmount: Double,
    val targetDate: String?    // ISO yyyy-MM-dd or null
)
