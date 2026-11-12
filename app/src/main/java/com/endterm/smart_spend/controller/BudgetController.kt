package com.endterm.smart_spend.controller

import android.content.Context
import com.endterm.smart_spend.model.entity.Budget
import com.endterm.smart_spend.model.local.BudgetDao
import com.endterm.smart_spend.model.local.DatabaseHelper
import com.endterm.smart_spend.model.local.DatabaseHelper.Companion.T_BUDGETS
import com.endterm.smart_spend.model.local.TransactionDao
import com.endterm.smart_spend.model.remote.FirestoreService
import com.endterm.smart_spend.util.TxType

/** Một dòng trên màn Ngân sách: hạn mức + đã chi + % . */
data class BudgetProgress(val budget: Budget, val spent: Long) {
    val percent: Int get() = if (budget.limitAmount == 0L) 0 else ((spent * 100) / budget.limitAmount).toInt()
    val isNearLimit: Boolean get() = percent >= budget.alertThresholdPct
}

class BudgetController(context: Context) : BaseController() {
    private val helper = DatabaseHelper.getInstance(context)
    private val dao = BudgetDao(helper)
    private val txDao = TransactionDao(helper)

    fun create(categoryId: String, limit: Long, period: String, alertPct: Int, onDone: (Result<Budget>) -> Unit) = async({
        require(limit > 0) { "Hạn mức phải lớn hơn 0" }
        require(alertPct in 1..100) { "Ngưỡng cảnh báo phải từ 1 đến 100%" }
        check(dao.getByCategory(categoryId) == null) { "Danh mục này đã có ngân sách" }
        val b = Budget(categoryId = categoryId, period = period, limitAmount = limit, alertThresholdPct = alertPct)
        dao.insert(b)
        FirestoreService.upsert(T_BUDGETS, b.id, b.toMap())
        b
    }, onDone)

    /** Đọc ngân sách kèm số đã chi trong khoảng [from, to) của kỳ hiện tại. */
    fun listWithProgress(from: String, to: String, onDone: (Result<List<BudgetProgress>>) -> Unit) = async({
        dao.getAll().map { b ->
            BudgetProgress(b, txDao.sumByType(TxType.EXPENSE, from, to, b.categoryId))
        }
    }, onDone)

    fun update(b: Budget, onDone: (Result<Unit>) -> Unit) = async({
        require(b.limitAmount > 0) { "Hạn mức phải lớn hơn 0" }
        check(dao.update(b) > 0) { "Không tìm thấy ngân sách" }
        FirestoreService.upsert(T_BUDGETS, b.id, b.toMap())
    }, onDone)

    fun delete(id: String, onDone: (Result<Unit>) -> Unit) = async({
        dao.delete(id)
        FirestoreService.delete(T_BUDGETS, id)
    }, onDone)
}
