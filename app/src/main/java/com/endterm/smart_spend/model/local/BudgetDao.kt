package com.endterm.smart_spend.model.local

import android.content.ContentValues
import android.database.Cursor
import com.endterm.smart_spend.model.entity.Budget
import com.endterm.smart_spend.model.local.DatabaseHelper.Companion.T_BUDGETS

class BudgetDao(private val helper: DatabaseHelper) {

    private fun Budget.toValues() = ContentValues().apply {
        put("id", id); put("category_id", categoryId); put("period", period)
        put("limit_amount", limitAmount); put("alert_threshold_pct", alertThresholdPct)
    }

    private fun Cursor.toBudget() = Budget(
        id = getString(getColumnIndexOrThrow("id")),
        categoryId = getString(getColumnIndexOrThrow("category_id")),
        period = getString(getColumnIndexOrThrow("period")),
        limitAmount = getLong(getColumnIndexOrThrow("limit_amount")),
        alertThresholdPct = getInt(getColumnIndexOrThrow("alert_threshold_pct"))
    )

    fun insert(b: Budget) { helper.writableDatabase.insertOrThrow(T_BUDGETS, null, b.toValues()) }

    fun getAll(): List<Budget> =
        helper.readableDatabase.query(T_BUDGETS, null, null, null, null, null, null).use { c ->
            buildList { while (c.moveToNext()) add(c.toBudget()) }
        }

    fun getById(id: String): Budget? =
        helper.readableDatabase.query(T_BUDGETS, null, "id = ?", arrayOf(id), null, null, null).use { c ->
            if (c.moveToFirst()) c.toBudget() else null
        }

    fun getByCategory(categoryId: String): Budget? =
        helper.readableDatabase.query(T_BUDGETS, null, "category_id = ?", arrayOf(categoryId), null, null, null).use { c ->
            if (c.moveToFirst()) c.toBudget() else null
        }

    fun update(b: Budget): Int =
        helper.writableDatabase.update(T_BUDGETS, b.toValues(), "id = ?", arrayOf(b.id))

    fun delete(id: String): Int =
        helper.writableDatabase.delete(T_BUDGETS, "id = ?", arrayOf(id))

    fun upsert(b: Budget) = helper.writableDatabase.upsert(T_BUDGETS, b.id, b.toValues())
}
