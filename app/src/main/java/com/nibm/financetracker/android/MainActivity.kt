package com.nibm.financetracker.android

import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.EditText
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.nibm.financetracker.android.data.local.AppDatabase
import com.nibm.financetracker.android.data.local.Budget
import com.nibm.financetracker.android.data.local.Category
import com.nibm.financetracker.android.data.local.Contribution
import com.nibm.financetracker.android.data.local.Expense
import com.nibm.financetracker.android.data.local.SavingsGoal
import com.nibm.financetracker.android.data.network.BudgetRequest
import com.nibm.financetracker.android.data.network.CategoryRequest
import com.nibm.financetracker.android.data.network.ContributionRequest
import com.nibm.financetracker.android.data.network.ExpenseRequest
import com.nibm.financetracker.android.data.network.RetrofitInstance
import com.nibm.financetracker.android.data.network.SavingsGoalRequest
import com.nibm.financetracker.android.databinding.ActivityMainBinding
import com.nibm.financetracker.android.databinding.NavDrawerLayoutBinding
import com.nibm.financetracker.android.ui.BudgetAdapter
import com.nibm.financetracker.android.ui.CategoryAdapter
import com.nibm.financetracker.android.ui.ExpenseAdapter
import com.nibm.financetracker.android.ui.SavingsGoalAdapter
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class MainActivity : AppCompatActivity() {

    private lateinit var mainBinding: ActivityMainBinding
    private lateinit var navBinding: NavDrawerLayoutBinding

    private val appDatabase: AppDatabase by lazy {
        AppDatabase.getDatabase(this.applicationContext)
    }

    // Adapters
    private val expenseAdapter = ExpenseAdapter(onDeleteClick = { expense -> deleteExpense(expense) })
    private val categoryAdapter = CategoryAdapter()
    private val budgetAdapter = BudgetAdapter(onDeleteClick = { budget -> deleteBudget(budget) })
    private val goalAdapter = SavingsGoalAdapter(
        onContributeClick = { goal -> promptContribution(goal) },
        onDeleteClick = { goal -> deleteGoal(goal) }
    )

    // In-memory
    private var allCategories: List<Category> = emptyList()
    private var allGoalsOnce: List<SavingsGoal> = emptyList()

    // Spinners
    private lateinit var expenseCategorySpinnerAdapter: ArrayAdapter<String>
    private lateinit var budgetCategorySpinnerAdapter: ArrayAdapter<String>

    // API date formats
    private val apiDateTime = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    private val apiDate = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(mainBinding.root)

        navBinding = NavDrawerLayoutBinding.bind(mainBinding.navDrawerContent.root)

        setupDrawer()
        setupExpenseRecyclerView()
        setupBudgetRecyclerView()
        setupGoalRecyclerView()
        setupExpenseCategorySpinner()
        setupBudgetCategorySpinner()

        // Buttons
        mainBinding.buttonSave.setOnClickListener { saveExpense() }
        mainBinding.buttonSync.setOnClickListener { syncAllData() }
        navBinding.buttonSaveCategory.setOnClickListener { saveCategory() }
        navBinding.buttonSaveBudget.setOnClickListener { saveBudget() }
        navBinding.buttonSaveGoal.setOnClickListener { saveGoal() }

        // Observers
        observeExpenses()
        observeCategories()
        observeBudgetsForCurrentMonth()
        observeGoals()
    }

    // ---------- Setup ----------
    private fun setupDrawer() {
        setSupportActionBar(mainBinding.toolbar)
        val toggle = ActionBarDrawerToggle(
            this,
            mainBinding.drawerLayout,
            mainBinding.toolbar,
            R.string.nav_open_drawer,
            R.string.nav_close_drawer
        )
        mainBinding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        navBinding.recyclerViewCategories.apply {
            adapter = categoryAdapter
            layoutManager = LinearLayoutManager(this@MainActivity)
        }
    }

    private fun setupExpenseRecyclerView() {
        mainBinding.recyclerViewExpenses.apply {
            adapter = expenseAdapter
            layoutManager = LinearLayoutManager(this@MainActivity)
        }
    }

    private fun setupBudgetRecyclerView() {
        navBinding.recyclerViewBudgets.apply {
            adapter = budgetAdapter
            layoutManager = LinearLayoutManager(this@MainActivity)
        }
    }

    private fun setupGoalRecyclerView() {
        navBinding.recyclerViewGoals.apply {
            adapter = goalAdapter
            layoutManager = LinearLayoutManager(this@MainActivity)
        }
    }

    private fun setupExpenseCategorySpinner() {
        expenseCategorySpinnerAdapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_item, mutableListOf())
        expenseCategorySpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        mainBinding.spinnerCategory.setAdapter(expenseCategorySpinnerAdapter)
    }

    private fun setupBudgetCategorySpinner() {
        budgetCategorySpinnerAdapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_item, mutableListOf())
        budgetCategorySpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        navBinding.spinnerBudgetCategory.setAdapter(budgetCategorySpinnerAdapter)
    }

    // ---------- Observers ----------
    private fun observeExpenses() {
        lifecycleScope.launch {
            appDatabase.expenseDao().getAllExpenses().collectLatest { list ->
                expenseAdapter.submitList(list)
            }
        }
    }

    private fun observeCategories() {
        lifecycleScope.launch {
            appDatabase.categoryDao().getAllCategories().collectLatest { categories ->
                allCategories = categories

                categoryAdapter.submitList(categories)

                val names = categories.map { it.name }
                expenseCategorySpinnerAdapter.clear()
                expenseCategorySpinnerAdapter.addAll(names)
                expenseCategorySpinnerAdapter.notifyDataSetChanged()

                budgetCategorySpinnerAdapter.clear()
                budgetCategorySpinnerAdapter.addAll(names)
                budgetCategorySpinnerAdapter.notifyDataSetChanged()

                // For rendering category names in expense & budget lists
                expenseAdapter.submitCategoryList(categories)
                budgetAdapter.submitCategories(categories)
            }
        }
    }

    private fun observeBudgetsForCurrentMonth() {
        val cal = Calendar.getInstance()
        val month = cal.get(Calendar.MONTH) + 1
        val year = cal.get(Calendar.YEAR)
        lifecycleScope.launch {
            appDatabase.budgetDao()
                .getBudgetsForMonth(month, year)
                .collectLatest { budgets ->
                    budgetAdapter.submitList(budgets)
                }
        }
    }

    private fun observeGoals() {
        lifecycleScope.launch {
            appDatabase.savingsGoalDao().getAllGoals().collectLatest { goals ->
                goalAdapter.submitList(goals)
            }
        }
    }

    // ---------- Local saves ----------
    private fun saveCategory() {
        val name = navBinding.inputCategory.text.toString().trim()
        if (name.isBlank()) {
            showSnackbar("Category name cannot be empty.", onDrawer = true)
            return
        }

        val newCategory = Category(name = name, localId = 0L)
        lifecycleScope.launch {
            val newId = appDatabase.categoryDao().insert(newCategory)
            val final = newCategory.copy(id = newId, localId = newId)
            appDatabase.categoryDao().update(final)
            navBinding.inputCategory.text?.clear()
            showSnackbar("Category '$name' saved.", onDrawer = true)
        }
    }

    private fun saveExpense() {
        val selectedCategoryName = mainBinding.spinnerCategory.text.toString()
        val description = mainBinding.inputDescription.text.toString().trim()
        val amountString = mainBinding.inputAmount.text.toString().trim()

        val category = allCategories.find { it.name == selectedCategoryName }
        if (category == null) {
            showSnackbar("Please select a valid category.")
            return
        }
        if (description.isBlank() || amountString.isBlank()) {
            showSnackbar("Description and amount cannot be empty.")
            return
        }
        val amount = amountString.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            showSnackbar("Please enter a valid amount.")
            return
        }

        val newExpense = Expense(
            description = description,
            amount = amount,
            categoryId = category.id, // local Room id
            date = Date(),
            localExpenseId = 0L
        )

        lifecycleScope.launch {
            val newId = appDatabase.expenseDao().insert(newExpense)
            val finalExpense = newExpense.copy(id = newId, localExpenseId = newId)
            appDatabase.expenseDao().update(finalExpense)

            mainBinding.inputDescription.text?.clear()
            mainBinding.inputAmount.text?.clear()
            mainBinding.spinnerCategory.text.clear()
            mainBinding.inputDescription.requestFocus()

            showSnackbar("Expense saved locally.")
        }
    }

    private fun saveBudget() {
        val selectedName = navBinding.spinnerBudgetCategory.text.toString()
        val amountStr = navBinding.inputBudgetAmount.text.toString().trim()

        val category = allCategories.find { it.name == selectedName }
        if (category == null) {
            showSnackbar("Please choose a valid category.", onDrawer = true)
            return
        }
        val amount = amountStr.toDoubleOrNull()
        if (amount == null || amount <= 0.0) {
            showSnackbar("Enter a valid budget amount.", onDrawer = true)
            return
        }

        val cal = Calendar.getInstance()
        val month = cal.get(Calendar.MONTH) + 1
        val year = cal.get(Calendar.YEAR)

        val newBudget = Budget(
            categoryId = category.id,      // local category id
            amountLimit = amount,
            month = month,
            year = year,
            userId = 1,
            isSynced = false,
            serverId = null,
            localId = 0L
        )

        lifecycleScope.launch {
            val insertId = appDatabase.budgetDao().insert(newBudget)
            val finalized = newBudget.copy(id = insertId, localId = insertId)
            appDatabase.budgetDao().update(finalized)

            navBinding.inputBudgetAmount.text?.clear()
            navBinding.spinnerBudgetCategory.setText("", false)
            showSnackbar("Budget saved for $selectedName ($month/$year).", onDrawer = true)
        }
    }

    private fun saveGoal() {
        val name = navBinding.inputGoalName.text.toString().trim()
        val targetStr = navBinding.inputGoalTarget.text.toString().trim()
        val dateStr = navBinding.inputGoalDate.text.toString().trim()

        if (name.isBlank()) {
            showSnackbar("Goal name cannot be empty.", onDrawer = true); return
        }
        val target = targetStr.toDoubleOrNull()
        if (target == null || target <= 0.0) {
            showSnackbar("Enter a valid target amount.", onDrawer = true); return
        }

        val targetDate = parseOptionalDate(dateStr)

        val goal = SavingsGoal(
            name = name,
            targetAmount = target,
            currentAmount = 0.0,
            targetDate = targetDate,
            userId = 1,
            isSynced = false,
            serverId = null,
            localId = 0L
        )

        lifecycleScope.launch {
            val id = appDatabase.savingsGoalDao().insert(goal)
            val final = goal.copy(id = id, localId = id)
            appDatabase.savingsGoalDao().update(final)

            navBinding.inputGoalName.text?.clear()
            navBinding.inputGoalTarget.text?.clear()
            navBinding.inputGoalDate.text?.clear()

            showSnackbar("Savings goal '$name' added.", onDrawer = true)
        }
    }

    // ---------- Contribute & Deletes ----------
    private fun promptContribution(goal: SavingsGoal) {
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            hint = "Amount"
        }
        AlertDialog.Builder(this)
            .setTitle("Add Contribution")
            .setView(input)
            .setPositiveButton("Add") { d, _ ->
                val amt = input.text.toString().toDoubleOrNull()
                if (amt == null || amt <= 0) {
                    showSnackbar("Enter a valid amount.", onDrawer = true)
                } else {
                    addContributionLocal(goal, amt)
                }
                d.dismiss()
            }
            .setNegativeButton("Cancel") { d, _ -> d.dismiss() }
            .show()
    }

    private fun addContributionLocal(goal: SavingsGoal, amount: Double) {
        lifecycleScope.launch {
            val c = Contribution(goalId = goal.id, amount = amount, isSynced = false, localId = 0L)
            val id = appDatabase.contributionDao().insert(c)
            appDatabase.contributionDao().update(c.copy(id = id, localId = id))

            // Update goal progress locally
            appDatabase.savingsGoalDao().update(goal.copy(currentAmount = goal.currentAmount + amount))

            showSnackbar("Added ${"%.2f".format(amount)} to '${goal.name}'.", onDrawer = true)
        }
    }

    private fun deleteExpense(expense: Expense) {
        lifecycleScope.launch {
            appDatabase.expenseDao().delete(expense)
            showSnackbar("Expense deleted.")
        }
    }

    private fun deleteBudget(budget: Budget) {
        lifecycleScope.launch {
            appDatabase.budgetDao().delete(budget)
            showSnackbar("Budget deleted.", onDrawer = true)
        }
    }

    // NEW: delete server first (if synced), then wipe local goal + contributions
    private fun deleteGoal(goal: SavingsGoal) {
        lifecycleScope.launch {
            try {
                val sid = goal.serverId
                if (sid != null) {
                    val resp = RetrofitInstance.api.deleteSavingsGoal(sid)
                    if (resp.isSuccessful || resp.code() == 204 || resp.code() == 404) {
                        // proceed with local delete
                    } else {
                        Log.e("DeleteGoal", "Server delete failed: ${resp.code()} ${resp.message()}")
                        showSnackbar("Could not delete from server (code ${resp.code()}). Deleted locally.", onDrawer = true)
                    }
                } // if not synced yet, just delete locally

                // local cascade: contributions then goal
                appDatabase.contributionDao().deleteByGoalId(goal.id)
                appDatabase.savingsGoalDao().delete(goal)

                showSnackbar("Goal deleted.", onDrawer = true)
            } catch (e: Exception) {
                Log.e("DeleteGoal", "Error deleting goal", e)
                // Fallback: ensure local deletion still happens
                appDatabase.contributionDao().deleteByGoalId(goal.id)
                appDatabase.savingsGoalDao().delete(goal)
                showSnackbar("Goal deleted locally (server unreachable).", onDrawer = true)
            }
        }
    }

    // ---------- Sync ----------
    private fun syncAllData() {
        mainBinding.buttonSync.text = "Syncing..."
        mainBinding.buttonSync.isEnabled = false

        lifecycleScope.launch {
            var categoriesSynced = 0
            var expensesSynced = 0
            var budgetsSynced = 0
            var goalsSynced = 0
            var contribsSynced = 0

            try {
                // CATEGORIES
                val unsyncedCategories = appDatabase.categoryDao().getUnsyncedCategories()
                for (category in unsyncedCategories) {
                    val resp = RetrofitInstance.api.syncCategory(
                        CategoryRequest(localId = category.localId, name = category.name)
                    )
                    if (resp.isSuccessful && resp.body() != null) {
                        val synced = resp.body()!!
                        category.isSynced = true
                        category.serverId = synced.id
                        appDatabase.categoryDao().update(category)
                        categoriesSynced++
                    } else {
                        Log.e("SyncError", "Category ${category.id} failed: ${resp.message()}")
                    }
                }
                allCategories = appDatabase.categoryDao().getAllOnce()

                // EXPENSES
                appDatabase.expenseDao().backfillLocalIds()
                val unsyncedExpenses = appDatabase.expenseDao().getUnsyncedExpenses()
                for (expense in unsyncedExpenses) {
                    val localCat = allCategories.find { it.id == expense.categoryId }
                    val serverCategoryId = localCat?.serverId
                    if (serverCategoryId == null) {
                        Log.e("SyncError", "Expense ${expense.id} skipped: category not synced yet")
                        continue
                    }
                    val req = ExpenseRequest(
                        description = expense.description,
                        amount = expense.amount,
                        categoryId = serverCategoryId,
                        userId = expense.userId,
                        expenseDate = apiDateTime.format(expense.date),
                        localExpenseId = expense.localExpenseId
                    )
                    val resp = RetrofitInstance.api.createExpense(req)
                    if (resp.isSuccessful && resp.body() != null) {
                        expense.isSynced = true
                        expense.serverId = resp.body()!!.id
                        appDatabase.expenseDao().update(expense)
                        expensesSynced++
                    } else {
                        Log.e("SyncError", "Expense ${expense.id} failed: ${resp.message()}")
                    }
                }

                // BUDGETS
                appDatabase.budgetDao().backfillLocalIds()
                val unsyncedBudgets = appDatabase.budgetDao().getUnsyncedBudgets()
                for (budget in unsyncedBudgets) {
                    val localCat = allCategories.find { it.id == budget.categoryId }
                    val serverCategoryId = localCat?.serverId
                    if (serverCategoryId == null) {
                        Log.e("SyncError", "Budget ${budget.id} skipped: category not synced yet")
                        continue
                    }
                    val req = BudgetRequest(
                        localId = budget.localId,
                        categoryId = serverCategoryId,
                        amountLimit = budget.amountLimit,
                        month = budget.month,
                        year = budget.year,
                        userId = budget.userId
                    )
                    val resp = RetrofitInstance.api.syncBudget(req)
                    if (resp.isSuccessful && resp.body() != null) {
                        budget.isSynced = true
                        budget.serverId = resp.body()!!.id
                        appDatabase.budgetDao().update(budget)
                        budgetsSynced++
                    } else {
                        Log.e("SyncError", "Budget ${budget.id} failed: ${resp.message()}")
                    }
                }

                // SAVINGS GOALS
                appDatabase.savingsGoalDao().backfillLocalIds()
                val unsyncedGoals = appDatabase.savingsGoalDao().getUnsyncedGoals()
                for (goal in unsyncedGoals) {
                    val req = SavingsGoalRequest(
                        name = goal.name,
                        targetAmount = goal.targetAmount,
                        targetDate = goal.targetDate?.let { apiDate.format(it) },
                        userId = goal.userId
                    )
                    val resp = RetrofitInstance.api.createSavingsGoal(req)
                    if (resp.isSuccessful && resp.body() != null) {
                        val server = resp.body()!!
                        goal.isSynced = true
                        goal.serverId = server.id
                        appDatabase.savingsGoalDao().update(goal)
                        goalsSynced++
                    } else {
                        Log.e("SyncError", "Goal ${goal.id} failed: ${resp.message()}")
                    }
                }
                allGoalsOnce = appDatabase.savingsGoalDao().getAllOnce()

                // CONTRIBUTIONS
                appDatabase.contributionDao().backfillLocalIds()
                val unsyncedContribs = appDatabase.contributionDao().getUnsyncedContributions()
                for (c in unsyncedContribs) {
                    val localGoal = allGoalsOnce.find { it.id == c.goalId }
                    val serverGoalId = localGoal?.serverId
                    if (serverGoalId == null) {
                        Log.e("SyncError", "Contribution ${c.id} skipped: goal not synced yet")
                        continue
                    }
                    val req = ContributionRequest(amount = formatBigDecimalString(c.amount))
                    val resp = RetrofitInstance.api.addContribution(serverGoalId, req)
                    if (resp.isSuccessful && resp.body() != null) {
                        c.isSynced = true
                        appDatabase.contributionDao().update(c)
                        val serverGoal = resp.body()!!
                        appDatabase.savingsGoalDao()
                            .update(localGoal.copy(currentAmount = serverGoal.currentAmount))
                        contribsSynced++
                    } else {
                        Log.e("SyncError", "Contribution ${c.id} failed: ${resp.message()}")
                    }
                }

                val nothing =
                    categoriesSynced == 0 && expensesSynced == 0 && budgetsSynced == 0 &&
                            goalsSynced == 0 && contribsSynced == 0 &&
                            appDatabase.categoryDao().getUnsyncedCategories().isEmpty() &&
                            appDatabase.expenseDao().getUnsyncedExpenses().isEmpty() &&
                            appDatabase.budgetDao().getUnsyncedBudgets().isEmpty() &&
                            appDatabase.savingsGoalDao().getUnsyncedGoals().isEmpty() &&
                            appDatabase.contributionDao().getUnsyncedContributions().isEmpty()

                if (nothing) {
                    showSnackbar("Everything is up to date.")
                } else {
                    showSnackbar(
                        "Sync complete! $categoriesSynced categories, " +
                                "$expensesSynced expenses, $budgetsSynced budgets, " +
                                "$goalsSynced goals, $contribsSynced contributions."
                    )
                }

            } catch (e: Exception) {
                Log.e("SyncError", "Network error", e)
                showSnackbar("Sync failed: Check network connection.")
            } finally {
                mainBinding.buttonSync.text = "Sync"
                mainBinding.buttonSync.isEnabled = true
            }
        }
    }

    // ---------- Helpers ----------
    private fun showSnackbar(message: String, onDrawer: Boolean = false) {
        val view = if (onDrawer || mainBinding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            navBinding.root
        } else {
            mainBinding.root
        }
        Snackbar.make(view, message, Snackbar.LENGTH_LONG).show()
    }

    private fun parseOptionalDate(s: String?): Date? {
        if (s.isNullOrBlank()) return null
        return try {
            apiDate.parse(s)
        } catch (_: ParseException) {
            null
        }
    }

    private fun formatBigDecimalString(amount: Double): String {
        return String.format(Locale.US, "%.2f", amount)
    }
}
