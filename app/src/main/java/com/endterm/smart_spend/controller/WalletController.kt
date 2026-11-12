package com.endterm.smart_spend.controller

import android.content.Context
import com.endterm.smart_spend.model.entity.Wallet
import com.endterm.smart_spend.model.local.DatabaseHelper
import com.endterm.smart_spend.model.local.DatabaseHelper.Companion.T_TRANSACTIONS
import com.endterm.smart_spend.model.local.DatabaseHelper.Companion.T_WALLETS
import com.endterm.smart_spend.model.local.TransactionDao
import com.endterm.smart_spend.model.local.WalletDao
import com.endterm.smart_spend.model.remote.FirestoreService
import com.endterm.smart_spend.util.Session

class WalletController(context: Context) : BaseController() {
    private val helper = DatabaseHelper.getInstance(context)
    private val dao = WalletDao(helper)
    private val txDao = TransactionDao(helper)

    fun create(name: String, type: String, initialBalance: Long, onDone: (Result<Wallet>) -> Unit) = async({
        require(name.isNotBlank()) { "Tên ví không được để trống" }
        val w = Wallet(userId = Session.userId(), name = name.trim(), type = type, initialBalance = initialBalance)
        dao.insert(w)                                           // 1) SQLite
        FirestoreService.upsert(T_WALLETS, w.id, w.toMap())     // 2) Firebase
        w
    }, onDone)

    fun list(onDone: (Result<List<Wallet>>) -> Unit) =
        async({ dao.getAll(Session.userId()) }, onDone)

    fun balance(walletId: String, onDone: (Result<Long>) -> Unit) =
        async({ dao.getBalance(walletId) }, onDone)

    fun update(w: Wallet, onDone: (Result<Unit>) -> Unit) = async({
        require(w.name.isNotBlank()) { "Tên ví không được để trống" }
        check(dao.update(w) > 0) { "Không tìm thấy ví" }
        FirestoreService.upsert(T_WALLETS, w.id, w.toMap())
    }, onDone)

    fun delete(walletId: String, onDone: (Result<Unit>) -> Unit) = async({
        val txIds = txDao.idsByWallet(walletId)                 // lấy trước: SQLite sẽ CASCADE xóa chúng
        dao.delete(walletId)
        txIds.forEach { FirestoreService.delete(T_TRANSACTIONS, it) }   // Firestore không tự cascade
        FirestoreService.delete(T_WALLETS, walletId)
    }, onDone)
}
