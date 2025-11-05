package com.nibm.financetracker.android.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nibm.financetracker.android.R
import com.nibm.financetracker.android.data.local.Category
import com.nibm.financetracker.android.data.local.Expense
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ExpenseAdapter(
    private val onDeleteClick: (Expense) -> Unit // <-- matches your MainActivity usage
) : ListAdapter<Expense, ExpenseAdapter.VH>(DIFF) {

    // local category id -> name (from Room)
    private var categoryNameById: Map<Long, String> = emptyMap()

    fun submitCategoryList(categories: List<Category>) {
        categoryNameById = categories.associate { it.id to it.name }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_expense, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position), categoryNameById, onDeleteClick)
    }

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDescription: TextView = itemView.findViewById(R.id.tvDescription)
        private val tvCategory: TextView = itemView.findViewById(R.id.tvCategory)
        private val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        private val tvAmount: TextView = itemView.findViewById(R.id.tvAmount)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)

        private val dateFmt = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        private val moneyFmt = NumberFormat.getCurrencyInstance()

        fun bind(
            expense: Expense,
            categoryNames: Map<Long, String>,
            onDelete: (Expense) -> Unit
        ) {
            tvDescription.text = expense.description

            val catName = categoryNames[expense.categoryId] ?: "N/A"
            tvCategory.text = catName

            tvDate.text = (expense.date ?: Date()).let { dateFmt.format(it) }

            tvAmount.text = moneyFmt.format(expense.amount)
            // optional: color for negative
            // tvAmount.setTextColor(if (expense.amount >= 0) Color.GREEN else Color.RED)

            btnDelete.setOnClickListener { onDelete(expense) }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Expense>() {
            override fun areItemsTheSame(oldItem: Expense, newItem: Expense) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: Expense, newItem: Expense) = oldItem == newItem
        }
    }
}
