package com.nibm.financetracker.android.data.network

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Streaming

interface ReportsApi {

    // ---------- JSON reports ----------
    @GET("/api/reports/category-spending")
    suspend fun categorySpending(
        @Query("month") month: Int? = null,
        @Query("year") year: Int? = null
    ): Response<List<CategoryExpenseReportDTO>>

    @GET("/api/reports/budget-adherence")
    suspend fun budgetAdherence(): Response<List<BudgetReportDTO>>

    @GET("/api/reports/monthly-spending")
    suspend fun monthlySpending(): Response<List<MonthlyExpenseReportDTO>>

    @GET("/api/reports/savings-forecast")
    suspend fun savingsForecast(): Response<List<SavingsForecastReportDTO>>

    // ---------- PDF downloads ----------
    @Streaming
    @GET("/api/reports/download/category-spending")
    suspend fun dlCategorySpendingPdf(): Response<ResponseBody>

    @Streaming
    @GET("/api/reports/download/budget-adherence")
    suspend fun dlBudgetAdherencePdf(): Response<ResponseBody>

    @Streaming
    @GET("/api/reports/download/monthly-spending")
    suspend fun dlMonthlySpendingPdf(): Response<ResponseBody>

    @Streaming
    @GET("/api/reports/download/savings-progress")
    suspend fun dlSavingsProgressPdf(): Response<ResponseBody>

    @Streaming
    @GET("/api/reports/download/savings-forecast")
    suspend fun dlSavingsForecastPdf(): Response<ResponseBody>
}
