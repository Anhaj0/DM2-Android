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
import com.nibm.financetracker.android.data.local.SavingsGoal
import java.text.NumberFormat

class SavingsGoalAdapter(
    private val onContributeClick: (SavingsGoal) -> Unit,
    private val onDeleteClick: (SavingsGoal) -> Unit
) : ListAdapter<SavingsGoal, SavingsGoalAdapter.VH>(Diff()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_savings_goal, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position), onContributeClick, onDeleteClick)
    }

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val name: TextView = itemView.findViewById(R.id.textGoalName)
        private val progress: TextView = itemView.findViewById(R.id.textGoalProgress)
        private val btnContribute: ImageButton = itemView.findViewById(R.id.buttonContribute)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.buttonDeleteGoal)

        private val moneyFmt = NumberFormat.getCurrencyInstance()

        fun bind(
            goal: SavingsGoal,
            onContribute: (SavingsGoal) -> Unit,
            onDelete: (SavingsGoal) -> Unit
        ) {
            name.text = goal.name
            progress.text = "${moneyFmt.format(goal.currentAmount)} / ${moneyFmt.format(goal.targetAmount)}"
            btnContribute.setOnClickListener { onContribute(goal) }
            btnDelete.setOnClickListener { onDelete(goal) }
        }
    }

    class Diff : DiffUtil.ItemCallback<SavingsGoal>() {
        override fun areItemsTheSame(old: SavingsGoal, new: SavingsGoal) = old.id == new.id
        override fun areContentsTheSame(old: SavingsGoal, new: SavingsGoal) = old == new
    }
}
