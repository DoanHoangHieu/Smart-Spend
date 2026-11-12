package com.endterm.smart_spend.model.entity

import com.endterm.smart_spend.util.Ids

data class Category(
    val id: String = Ids.new(),
    val userId: String,
    val parentId: String? = null,
    val name: String,
    val type: String,                   // income | expense
    val icon: String = "ic_tag"
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id, "user_id" to userId, "parent_id" to parentId,
        "name" to name, "type" to type, "icon" to icon
    )

    companion object {
        fun fromMap(m: Map<String, Any?>) = Category(
            id = m["id"] as String,
            userId = m["user_id"] as String,
            parentId = m["parent_id"] as? String,
            name = m["name"] as String,
            type = m["type"] as String,
            icon = m["icon"] as? String ?: "ic_tag"
        )
    }
}
