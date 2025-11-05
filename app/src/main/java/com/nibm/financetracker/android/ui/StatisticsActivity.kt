package com.nibm.financetracker.android.ui

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.google.android.material.snackbar.Snackbar
import com.nibm.financetracker.android.R
import com.nibm.financetracker.android.data.network.CategoryExpenseReportDTO
import com.nibm.financetracker.android.data.network.RetrofitInstance
import com.nibm.financetracker.android.databinding.ActivityStatisticsBinding
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Locale

class StatisticsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStatisticsBinding
    private val reportsApi by lazy { RetrofitInstance.reports }
    private val topAdapter = TopCategoryAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStatisticsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = getString(R.string.stats_title)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.recyclerTop.adapter = topAdapter

        binding.btnDlCategory.setOnClickListener {
            download("/api/reports/download/category-spending", "CategorySpendingReport.pdf")
        }
        binding.btnDlBudget.setOnClickListener {
            download("/api/reports/download/budget-adherence", "BudgetAdherenceReport.pdf")
        }
        binding.btnDlMonthly.setOnClickListener {
            download("/api/reports/download/monthly-spending", "MonthlySpendingReport.pdf")
        }
        binding.btnDlSavings.setOnClickListener {
            download("/api/reports/download/savings-progress", "SavingsProgressReport.pdf")
        }
        binding.btnDlForecast.setOnClickListener {
            download("/api/reports/download/savings-forecast", "SavingsForecastData.pdf")
        }

        refresh()
    }

    private fun refresh() = lifecycleScope.launch {
        try {
            val today = LocalDate.now()
            val month = today.monthValue
            val year = today.year

            // Budget adherence for progress ring
            val budgetResp = reportsApi.budgetAdherence()
            val budgets = budgetResp.body().orEmpty()
            val totalLimit = budgets.fold(BigDecimal.ZERO) { acc, b -> acc + b.amountLimit }
            val totalSpent = budgets.fold(BigDecimal.ZERO) { acc, b -> acc + b.totalSpent }
            val balance = totalLimit - totalSpent
            val pct = if (totalLimit > BigDecimal.ZERO)
                balance.multiply(BigDecimal(100)).divide(totalLimit, 2, java.math.RoundingMode.HALF_UP)
            else BigDecimal.ZERO

            binding.txtBalance.text = money(balance)
            binding.txtBudget.text = money(totalLimit)
            binding.txtSpent.text = money(totalSpent)
            drawRing(binding.ringChart, totalSpent, balance, pct.toPlainString() + "%")

            // Monthly total (left card)
            val monthly = reportsApi.monthlySpending().body().orEmpty()
            val thisMonthTotal = monthly.find { it.year == year && it.month == month }?.totalAmount ?: BigDecimal.ZERO
            binding.txtMonth.text = String.format(Locale.US, "%02d", month)
            binding.txtMonthExpense.text = money(thisMonthTotal)
            binding.txtMonthIncome.text = "—"

            // Category donut for this month
            val cats = reportsApi.categorySpending(month, year).body().orEmpty()
            drawDonut(binding.donutChart, cats)
            topAdapter.submitList(cats)
            binding.emptyTop.visibility = if (cats.isEmpty()) View.VISIBLE else View.GONE

        } catch (e: Exception) {
            Snackbar.make(binding.root, "Failed to load stats", Snackbar.LENGTH_LONG).show()
        }
    }

    private fun drawRing(chart: PieChart, spent: BigDecimal, balance: BigDecimal, centerText: String) {
        val entries = arrayListOf(
            PieEntry(spent.toFloat(), "Expenses"),
            PieEntry(balance.coerceAtLeast(BigDecimal.ZERO).toFloat(), "Balance")
        )
        val ds = PieDataSet(entries, "").apply { setDrawValues(false) }
        chart.data = PieData(ds)
        chart.description.isEnabled = false
        chart.isDrawHoleEnabled = true
        chart.holeRadius = 70f
        chart.setDrawEntryLabels(false)
        chart.centerText = centerText
        chart.legend.isEnabled = false
        chart.invalidate()
    }

    private fun drawDonut(chart: PieChart, cats: List<CategoryExpenseReportDTO>) {
        val total = cats.fold(BigDecimal.ZERO) { a, b -> a + b.totalAmount }
        val entries = cats.map { PieEntry(it.totalAmount.toFloat(), it.categoryName) }
        val ds = PieDataSet(entries, "").apply { setDrawValues(false) }
        chart.data = PieData(ds)
        chart.description.isEnabled = false
        chart.isDrawHoleEnabled = true
        chart.holeRadius = 60f
        chart.setDrawEntryLabels(false)
        chart.legend.orientation = Legend.LegendOrientation.VERTICAL
        chart.legend.isWordWrapEnabled = true
        chart.centerText = if (total > BigDecimal.ZERO) total.toPlainString() else "0"
        chart.invalidate()
    }

    private fun money(v: BigDecimal): String =
        "$" + v.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString()

    private fun download(path: String, fileName: String) {
        val url = "http://10.0.2.2:8080$path"
        val req = DownloadManager.Request(Uri.parse(url))
            .setTitle(fileName)
            .setDescription("Downloading report")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(android.os.Environment.DIRECTORY_DOWNLOADS, fileName)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val dm = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        dm.enqueue(req)
        Snackbar.make(binding.root, "Downloading $fileName…", Snackbar.LENGTH_SHORT).show()
    }
}
