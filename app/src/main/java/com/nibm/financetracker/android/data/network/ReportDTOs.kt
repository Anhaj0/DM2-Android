package com.nibm.financetracker.android.data.network

import java.math.BigDecimal

data class CategoryExpenseReportDTO(
    val categoryName: String,
    val totalAmount: BigDecimal
)

data class BudgetReportDTO(
    val categoryName: String,
    val amountLimit: BigDecimal,
    val totalSpent: BigDecimal,
    val remainingAmount: BigDecimal
)

data class MonthlyExpenseReportDTO(
    val year: Int,
    val month: Int,
    val totalAmount: BigDecimal
)

data class SavingsForecastReportDTO(
    val contributionDate: String,   // ISO yyyy-MM-dd
    val cumulativeAmount: BigDecimal
)
