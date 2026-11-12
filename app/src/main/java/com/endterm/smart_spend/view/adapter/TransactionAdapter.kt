package com.endterm.smart_spend.view.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.endterm.smart_spend.R
import com.endterm.smart_spend.model.entity.Transaction
import com.endterm.smart_spend.model.entity.TransactionItem
import com.endterm.smart_spend.util.Dates
import com.endterm.smart_spend.util.Money
import com.endterm.smart_spend.util.TxType

/** Dùng lại layout tĩnh item_transaction.xml (imgCategory, tvTitle, tvSubtitle, tvAmount). */
class TransactionAdapter(
    private val onClick: (Transaction) -> Unit,
    private val onLongClick: (Transaction) -> Unit
) : ListAdapter<TransactionItem, TransactionAdapter.VH>(DIFF) {

    class VH(view: android.view.View) : RecyclerView.ViewHolder(view) {
        val img: ImageView = view.findViewById(R.id.imgCategory)
        val title: TextView = view.findViewById(R.id.tvTitle)
        val subtitle: TextView = view.findViewById(R.id.tvSubtitle)
        val amount: TextView = view.findViewById(R.id.tvAmount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_transaction, parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        val t = item.transaction
        val ctx = holder.itemView.context
        val isIncome = t.type == TxType.INCOME

        holder.title.text = t.note.ifBlank { item.categoryName }
        holder.subtitle.text = "${item.categoryName} · ${Dates.timeOf(t.datetime)}"
        holder.amount.text = (if (isIncome) "+" else "-") + Money.format(t.amount)
        holder.amount.setTextColor(ContextCompat.getColor(ctx, if (isIncome) R.color.income else R.color.expense))

        holder.itemView.setOnClickListener { onClick(t) }
        holder.itemView.setOnLongClickListener { onLongClick(t); true }
    }

    private companion object {
        val DIFF = object : DiffUtil.ItemCallback<TransactionItem>() {
            override fun areItemsTheSame(a: TransactionItem, b: TransactionItem) = a.transaction.id == b.transaction.id
            override fun areContentsTheSame(a: TransactionItem, b: TransactionItem) = a == b
        }
    }
}
