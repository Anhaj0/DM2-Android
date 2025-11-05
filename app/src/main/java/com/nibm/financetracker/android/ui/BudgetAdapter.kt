package com.nibm.financetracker.android.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nibm.financetracker.android.data.local.Budget
import com.nibm.financetracker.android.data.local.Category
import com.nibm.financetracker.android.databinding.ItemBudgetBinding

class BudgetAdapter(
    private val onDeleteClick: (Budget) -> Unit
) : ListAdapter<Budget, BudgetAdapter.BudgetVH>(Diff()) {

    // categories to resolve names
    private var categories: List<Category> = emptyList()
    fun submitCategories(list: List<Category>) {
        categories = list
        notifyItemRangeChanged(0, itemCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BudgetVH {
        val binding = ItemBudgetBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BudgetVH(binding)
    }

    override fun onBindViewHolder(holder: BudgetVH, position: Int) {
        val b = getItem(position)
        val catName = categories.find { it.id == b.categoryId }?.name ?: "—"
        holder.bind(b, catName, onDeleteClick)
    }

    class BudgetVH(private val binding: ItemBudgetBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(budget: Budget, categoryName: String, onDeleteClick: (Budget) -> Unit) {
            binding.textBudgetTitle.text = "$categoryName  (${budget.month}/${budget.year})"
            binding.textBudgetAmount.text = "$${"%.2f".format(budget.amountLimit)}"
            binding.iconSyncStatus.visibility = if (budget.isSynced) View.VISIBLE else View.GONE
            binding.buttonDelete.setOnClickListener { onDeleteClick(budget) }
        }
    }

    class Diff : DiffUtil.ItemCallback<Budget>() {
        override fun areItemsTheSame(old: Budget, new: Budget) = old.id == new.id
        override fun areContentsTheSame(old: Budget, new: Budget) = old == new
    }
}
