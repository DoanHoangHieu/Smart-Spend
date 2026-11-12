package com.endterm.smart_spend.model.local

import android.content.ContentValues
import android.database.Cursor
import com.endterm.smart_spend.model.entity.Wallet
import com.endterm.smart_spend.model.local.DatabaseHelper.Companion.T_WALLETS

class WalletDao(private val helper: DatabaseHelper) {

    private fun Wallet.toValues() = ContentValues().apply {
        put("id", id); put("user_id", userId); put("name", name)
        put("type", type); put("initial_balance", initialBalance)
    }

    private fun Cursor.toWallet() = Wallet(
        id = getString(getColumnIndexOrThrow("id")),
        userId = getString(getColumnIndexOrThrow("user_id")),
        name = getString(getColumnIndexOrThrow("name")),
        type = getString(getColumnIndexOrThrow("type")),
        initialBalance = getLong(getColumnIndexOrThrow("initial_balance"))
    )

    // ---- CREATE ----
    fun insert(w: Wallet) { helper.writableDatabase.insertOrThrow(T_WALLETS, null, w.toValues()) }

    // ---- READ ----
    fun getAll(userId: String): List<Wallet> =
        helper.readableDatabase.query(T_WALLETS, null, "user_id = ?", arrayOf(userId), null, null, "name").use { c ->
            buildList { while (c.moveToNext()) add(c.toWallet()) }
        }

    fun getById(id: String): Wallet? =
        helper.readableDatabase.query(T_WALLETS, null, "id = ?", arrayOf(id), null, null, null).use { c ->
            if (c.moveToFirst()) c.toWallet() else null
        }

    /** Số dư = số dư ban đầu + tổng thu - tổng chi (tính bằng SQL, không lưu cột riêng để khỏi lệch dữ liệu). */
    fun getBalance(walletId: String): Long =
        helper.readableDatabase.rawQuery(
            """SELECT w.initial_balance +
                      COALESCE(SUM(CASE t.type WHEN 'income' THEN t.amount ELSE -t.amount END), 0)
               FROM wallets w LEFT JOIN transactions t ON t.wallet_id = w.id
               WHERE w.id = ? GROUP BY w.id""", arrayOf(walletId)
        ).use { if (it.moveToFirst()) it.getLong(0) else 0L }

    fun count(userId: String): Int =
        helper.readableDatabase.rawQuery("SELECT COUNT(*) FROM wallets WHERE user_id = ?", arrayOf(userId))
            .use { it.moveToFirst(); it.getInt(0) }

    // ---- UPDATE ----
    fun update(w: Wallet): Int =
        helper.writableDatabase.update(T_WALLETS, w.toValues(), "id = ?", arrayOf(w.id))

    // ---- DELETE ---- (giao dịch của ví bị xóa theo nhờ ON DELETE CASCADE)
    fun delete(id: String): Int =
        helper.writableDatabase.delete(T_WALLETS, "id = ?", arrayOf(id))

    /** Dùng khi kéo dữ liệu từ Firestore về. */
    fun upsert(w: Wallet) = helper.writableDatabase.upsert(T_WALLETS, w.id, w.toValues())
}
