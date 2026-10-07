package com.example.moneytracker.ui.transaction

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.DiffUtil
import com.example.moneytracker.R
import com.example.moneytracker.data.model.Transaction
import com.example.moneytracker.databinding.ItemTransactionBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TransactionAdapter(
    private val onDeleteClick:
        (Transaction) -> Unit,

    private val onEditClick:
        (Transaction) -> Unit,
    private val showActions: Boolean = true
) : RecyclerView.Adapter<
        TransactionAdapter.TransactionViewHolder>() {

    private var transactions:
            List<Transaction> =
        emptyList()

    fun submitList(
        newTransactions:
        List<Transaction>
    ) {

        val oldTransactions = transactions
        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = oldTransactions.size
            override fun getNewListSize() = newTransactions.size
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
                oldTransactions[oldItemPosition].id == newTransactions[newItemPosition].id
            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) =
                oldTransactions[oldItemPosition] == newTransactions[newItemPosition]
        })
        transactions = newTransactions.toList()
        diff.dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): TransactionViewHolder {

        val binding =
            ItemTransactionBinding.inflate(
                LayoutInflater.from(
                    parent.context
                ),
                parent,
                false
            )

        return TransactionViewHolder(
            binding,
            onDeleteClick,
            onEditClick
        )
    }

    override fun onBindViewHolder(
        holder: TransactionViewHolder,
        position: Int
    ) {

        holder.bind(
            transactions[position]
        )
        (holder.itemView.findViewById<android.view.View>(R.id.editButton).parent as android.view.View)
            .visibility = if (showActions) android.view.View.VISIBLE else android.view.View.GONE
        val transaction = transactions[position]
        holder.itemView.setOnClickListener { onEditClick(transaction) }
    }

    override fun getItemCount():
            Int {

        return transactions.size
    }

    class TransactionViewHolder(
        private val binding:
        ItemTransactionBinding,

        private val onDeleteClick:
            (Transaction) -> Unit,

        private val onEditClick:
            (Transaction) -> Unit
    ) : RecyclerView.ViewHolder(
        binding.root
    ) {

        fun bind(
            transaction: Transaction
        ) {

            binding.categoryTextView.text =
                transaction.categoryId

            binding.descriptionTextView.text =
                transaction.description
                    .ifBlank {
                        "No description"
                    }

            val dateFormat =
                SimpleDateFormat(
                    "MM/dd/yyyy",
                    Locale.US
                )

            binding.dateTextView.text =
                dateFormat.format(
                    Date(
                        transaction.transactionDate
                    )
                )

            val prefix =
                if (
                    transaction.type ==
                    "income"
                ) {
                    "+"
                } else {
                    "-"
                }

            binding.amountTextView.text =
                prefix + Money.format(transaction.amountCents, transaction.currencyCode)

            val amountColor =
                if (
                    transaction.type ==
                    "income"
                ) {
                    R.color.income_green
                } else {
                    R.color.expense_red
                }

            binding.amountTextView
                .setTextColor(
                    binding.root.context
                        .getColor(
                            amountColor
                        )
                )

            binding.categoryIconTextView.text =
                when (
                    transaction.categoryId
                        .lowercase()
                ) {

                    "food" ->
                        "🍔"

                    "transport" ->
                        "🚗"

                    "shopping" ->
                        "🛍️"

                    "bills" ->
                        "🧾"

                    "entertainment" ->
                        "🎬"

                    else ->
                        "💰"
                }

            binding.deleteButton
                .setOnClickListener {
                    onDeleteClick(
                        transaction
                    )
                }

            binding.editButton
                .setOnClickListener {
                    onEditClick(
                        transaction
                    )
                }
        }
    }
}
