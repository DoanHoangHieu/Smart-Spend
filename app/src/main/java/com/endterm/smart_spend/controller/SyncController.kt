package com.endterm.smart_spend.controller

import android.content.Context
import com.endterm.smart_spend.model.entity.Budget
import com.endterm.smart_spend.model.entity.Category
import com.endterm.smart_spend.model.entity.Transaction
import com.endterm.smart_spend.model.entity.Wallet
import com.endterm.smart_spend.model.local.BudgetDao
import com.endterm.smart_spend.model.local.CategoryDao
import com.endterm.smart_spend.model.local.DatabaseHelper
import com.endterm.smart_spend.model.local.DatabaseHelper.Companion.T_BUDGETS
import com.endterm.smart_spend.model.local.DatabaseHelper.Companion.T_CATEGORIES
import com.endterm.smart_spend.model.local.DatabaseHelper.Companion.T_TRANSACTIONS
import com.endterm.smart_spend.model.local.DatabaseHelper.Companion.T_WALLETS
import com.endterm.smart_spend.model.local.TransactionDao
import com.endterm.smart_spend.model.local.WalletDao
import com.endterm.smart_spend.model.remote.FirestoreService
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Gọi sau khi đăng nhập / cài lại máy: kéo dữ liệu Firestore về SQLite.
 * Thứ tự quan trọng vì có khóa ngoại: wallets, categories -> transactions, budgets.
 */
class SyncController(context: Context) : BaseController() {
    private val helper = DatabaseHelper.getInstance(context)
    private val walletDao = WalletDao(helper)
    private val categoryDao = CategoryDao(helper)
    private val txDao = TransactionDao(helper)
    private val budgetDao = BudgetDao(helper)

    fun pullAll(onDone: (Result<Unit>) -> Unit) = async({
        pull(T_WALLETS) { walletDao.upsert(Wallet.fromMap(it)) }
        pull(T_CATEGORIES) { categoryDao.upsert(Category.fromMap(it)) }
        pull(T_TRANSACTIONS) { txDao.upsert(Transaction.fromMap(it)) }
        pull(T_BUDGETS) { budgetDao.upsert(Budget.fromMap(it)) }
    }, onDone)

    /** Chạy ở luồng nền nên có thể chờ kết quả bất đồng bộ của Firestore bằng latch. */
    private fun pull(collection: String, save: (Map<String, Any?>) -> Unit) {
        var docs: List<Map<String, Any?>> = emptyList()
        var error: Exception? = null
        val latch = CountDownLatch(1)
        FirestoreService.fetchAll(collection,
            onSuccess = { docs = it; latch.countDown() },
            onError = { error = it; latch.countDown() })
        check(latch.await(30, TimeUnit.SECONDS)) { "Hết thời gian chờ khi tải $collection" }
        error?.let { throw it }
        docs.forEach(save)
    }
}
