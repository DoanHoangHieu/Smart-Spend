package com.endterm.smart_spend.controller

import android.content.Context
import com.endterm.smart_spend.model.entity.Transaction
import com.endterm.smart_spend.model.entity.TransactionItem
import com.endterm.smart_spend.model.local.DatabaseHelper
import com.endterm.smart_spend.model.local.DatabaseHelper.Companion.T_TRANSACTIONS
import com.endterm.smart_spend.model.local.TransactionDao
import com.endterm.smart_spend.model.remote.FirestoreService
import com.endterm.smart_spend.util.TxType

class TransactionController(context: Context) : BaseController() {
    private val dao = TransactionDao(DatabaseHelper.getInstance(context))

    private fun validate(t: Transaction) {
        require(t.amount > 0) { "Số tiền phải lớn hơn 0" }
        require(t.type == TxType.INCOME || t.type == TxType.EXPENSE) { "Loại giao dịch không hợp lệ" }
        require(t.walletId.isNotBlank()) { "Chưa chọn ví" }
        require(t.categoryId.isNotBlank()) { "Chưa chọn danh mục" }
    }

    fun add(t: Transaction, onDone: (Result<Unit>) -> Unit) = async({
        validate(t)
        dao.insert(t)                                            // SQLite trước: UI đọc từ đây
        FirestoreService.upsert(T_TRANSACTIONS, t.id, t.toMap()) // rồi đẩy lên Firebase
    }, onDone)

    fun list(walletId: String? = null, onDone: (Result<List<TransactionItem>>) -> Unit) =
        async({ dao.getAllWithCategory(walletId) }, onDone)

    fun get(id: String, onDone: (Result<Transaction?>) -> Unit) =
        async({ dao.getById(id) }, onDone)

    fun update(t: Transaction, onDone: (Result<Unit>) -> Unit) = async({
        validate(t)
        check(dao.update(t) > 0) { "Không tìm thấy giao dịch" }
        FirestoreService.upsert(T_TRANSACTIONS, t.id, t.toMap())
    }, onDone)

    fun delete(id: String, onDone: (Result<Unit>) -> Unit) = async({
        dao.delete(id)
        FirestoreService.delete(T_TRANSACTIONS, id)
    }, onDone)

    /** Tổng chi / thu trong khoảng thời gian, dùng cho thẻ số dư ở Trang chủ. */
    fun total(type: String, from: String, to: String, onDone: (Result<Long>) -> Unit) =
        async({ dao.sumByType(type, from, to) }, onDone)
}
