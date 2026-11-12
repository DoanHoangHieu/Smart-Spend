package com.endterm.smart_spend.model.local

import android.content.ContentValues
import android.database.Cursor
import com.endterm.smart_spend.model.entity.Transaction
import com.endterm.smart_spend.model.entity.TransactionItem
import com.endterm.smart_spend.model.local.DatabaseHelper.Companion.T_TRANSACTIONS

class TransactionDao(private val helper: DatabaseHelper) {

    private fun Transaction.toValues() = ContentValues().apply {
        put("id", id); put("wallet_id", walletId); put("category_id", categoryId)
        put("recurring_rule_id", recurringRuleId); put("amount", amount); put("note", note)
        put("datetime", datetime); put("type", type); put("image_url", imageUrl)
    }

    private fun Cursor.str(col: String): String = getString(getColumnIndexOrThrow(col))
    private fun Cursor.strOrNull(col: String): String? = getString(getColumnIndexOrThrow(col))

    private fun Cursor.toTransaction() = Transaction(
        id = str("id"), walletId = str("wallet_id"), categoryId = str("category_id"),
        amount = getLong(getColumnIndexOrThrow("amount")), type = str("type"),
        note = strOrNull("note") ?: "", datetime = str("datetime"),
        recurringRuleId = strOrNull("recurring_rule_id"), imageUrl = strOrNull("image_url")
    )

    // ---- CREATE ----
    fun insert(t: Transaction) { helper.writableDatabase.insertOrThrow(T_TRANSACTIONS, null, t.toValues()) }

    // ---- READ ----
    fun getById(id: String): Transaction? =
        helper.readableDatabase.query(T_TRANSACTIONS, null, "id = ?", arrayOf(id), null, null, null).use { c ->
            if (c.moveToFirst()) c.toTransaction() else null
        }

    /** Danh sách mới nhất trước, kèm tên danh mục (JOIN) để đổ thẳng vào RecyclerView. */
    fun getAllWithCategory(walletId: String? = null, limit: Int = 200): List<TransactionItem> {
        val where = if (walletId == null) "" else "WHERE t.wallet_id = ?"
        val args = if (walletId == null) emptyArray<String>() else arrayOf(walletId)
        val sql = """SELECT t.*, c.name AS category_name
                     FROM transactions t LEFT JOIN categories c ON c.id = t.category_id
                     $where ORDER BY t.datetime DESC LIMIT $limit"""
        return helper.readableDatabase.rawQuery(sql, args).use { c ->
            buildList {
                while (c.moveToNext()) add(TransactionItem(c.toTransaction(), c.strOrNull("category_name") ?: "Khác"))
            }
        }
    }

    fun idsByWallet(walletId: String): List<String> =
        helper.readableDatabase.query(T_TRANSACTIONS, arrayOf("id"), "wallet_id = ?", arrayOf(walletId), null, null, null)
            .use { c -> buildList { while (c.moveToNext()) add(c.getString(0)) } }

    /** Tổng thu hoặc chi trong khoảng [from, to) - dùng cho Trang chủ / Báo cáo / Ngân sách. */
    fun sumByType(type: String, from: String, to: String, categoryId: String? = null): Long {
        val extra = if (categoryId == null) "" else " AND category_id = ?"
        val args = listOfNotNull(type, from, to, categoryId).toTypedArray()
        return helper.readableDatabase.rawQuery(
            "SELECT COALESCE(SUM(amount),0) FROM transactions WHERE type = ? AND datetime >= ? AND datetime < ?$extra", args
        ).use { it.moveToFirst(); it.getLong(0) }
    }

    // ---- UPDATE ----
    fun update(t: Transaction): Int =
        helper.writableDatabase.update(T_TRANSACTIONS, t.toValues(), "id = ?", arrayOf(t.id))

    // ---- DELETE ----
    fun delete(id: String): Int =
        helper.writableDatabase.delete(T_TRANSACTIONS, "id = ?", arrayOf(id))

    fun upsert(t: Transaction) = helper.writableDatabase.upsert(T_TRANSACTIONS, t.id, t.toValues())
}
