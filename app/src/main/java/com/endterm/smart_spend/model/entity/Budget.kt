package com.endterm.smart_spend.model.entity

import com.endterm.smart_spend.util.Ids

data class Budget(
    val id: String = Ids.new(),
    val categoryId: String,
    val period: String = "month",       // week | month | year
    val limitAmount: Long,
    val alertThresholdPct: Int = 80
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id, "category_id" to categoryId, "period" to period,
        "limit_amount" to limitAmount, "alert_threshold_pct" to alertThresholdPct
    )

    companion object {
        fun fromMap(m: Map<String, Any?>) = Budget(
            id = m["id"] as String,
            categoryId = m["category_id"] as String,
            period = m["period"] as? String ?: "month",
            limitAmount = (m["limit_amount"] as Number).toLong(),
            alertThresholdPct = (m["alert_threshold_pct"] as? Number)?.toInt() ?: 80
        )
    }
}
