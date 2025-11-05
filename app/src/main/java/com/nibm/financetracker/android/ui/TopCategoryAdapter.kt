package com.nibm.financetracker.android.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nibm.financetracker.android.data.network.CategoryExpenseReportDTO
import com.nibm.financetracker.android.databinding.ItemTopCategoryBinding
import java.math.BigDecimal

class TopCategoryAdapter :
    ListAdapter<CategoryExpenseReportDTO, TopCategoryAdapter.VH>(Diff()) {

    class VH(private val b: ItemTopCategoryBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(row: CategoryExpenseReportDTO, total: BigDecimal) {
            b.txtName.text = row.categoryName
            b.txtAmt.text = "$" + row.totalAmount.setScale(2).toPlainString()
            val pct = if (total > BigDecimal.ZERO)
                row.totalAmount.multiply(BigDecimal(100))
                    .divide(total, 1, java.math.RoundingMode.HALF_UP)
            else BigDecimal.ZERO
            b.txtPct.text = "${pct.toPlainString()}%"
            b.progress.progress = pct.toInt().coerceIn(0, 100)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemTopCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val total = currentList.fold(BigDecimal.ZERO) { a, b -> a + b.totalAmount }
        holder.bind(getItem(position), total)
    }

    private class Diff : DiffUtil.ItemCallback<CategoryExpenseReportDTO>() {
        override fun areItemsTheSame(o: CategoryExpenseReportDTO, n: CategoryExpenseReportDTO) =
            o.categoryName == n.categoryName
        override fun areContentsTheSame(o: CategoryExpenseReportDTO, n: CategoryExpenseReportDTO) =
            o == n
    }
}
