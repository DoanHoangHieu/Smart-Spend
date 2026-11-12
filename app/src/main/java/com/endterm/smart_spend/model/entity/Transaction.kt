package com.endterm.smart_spend.model.entity

import com.endterm.smart_spend.util.Dates
import com.endterm.smart_spend.util.Ids

data class Transaction(
    val id: String = Ids.new(),
    val walletId: String,
    val categoryId: String,
    val amount: Long,                   // VND, luôn > 0; chiều thu/chi nằm ở `type`
    val type: String,                   // income | expense
    val note: String = "",
    val datetime: String = Dates.now(), // "yyyy-MM-dd HH:mm"
    val recurringRuleId: String? = null,
    val imageUrl: String? = null
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id, "wallet_id" to walletId, "category_id" to categoryId,
        "recurring_rule_id" to recurringRuleId, "amount" to amount, "note" to note,
        "datetime" to datetime, "type" to type, "image_url" to imageUrl
    )

    companion object {
        fun fromMap(m: Map<String, Any?>) = Transaction(
            id = m["id"] as String,
            walletId = m["wallet_id"] as String,
            categoryId = m["category_id"] as String,
            amount = (m["amount"] as Number).toLong(),
            type = m["type"] as String,
            note = m["note"] as? String ?: "",
            datetime = m["datetime"] as String,
            recurringRuleId = m["recurring_rule_id"] as? String,
            imageUrl = m["image_url"] as? String
        )
    }
}

/** Kết quả JOIN để hiển thị danh sách (item_transaction.xml). */
data class TransactionItem(val transaction: Transaction, val categoryName: String)
