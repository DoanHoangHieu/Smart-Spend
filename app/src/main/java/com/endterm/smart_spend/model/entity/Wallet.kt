package com.endterm.smart_spend.model.entity

import com.endterm.smart_spend.util.Ids

data class Wallet(
    val id: String = Ids.new(),
    val userId: String,
    val name: String,
    val type: String = "cash",          // cash | bank | ewallet
    val initialBalance: Long = 0
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id, "user_id" to userId, "name" to name,
        "type" to type, "initial_balance" to initialBalance
    )

    companion object {
        fun fromMap(m: Map<String, Any?>) = Wallet(
            id = m["id"] as String,
            userId = m["user_id"] as String,
            name = m["name"] as String,
            type = m["type"] as? String ?: "cash",
            initialBalance = (m["initial_balance"] as? Number)?.toLong() ?: 0
        )
    }
}
