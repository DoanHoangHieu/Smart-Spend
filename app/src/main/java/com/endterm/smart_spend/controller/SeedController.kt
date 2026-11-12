package com.endterm.smart_spend.controller

import android.content.Context
import com.endterm.smart_spend.model.entity.Category
import com.endterm.smart_spend.model.entity.Wallet
import com.endterm.smart_spend.model.local.CategoryDao
import com.endterm.smart_spend.model.local.DatabaseHelper
import com.endterm.smart_spend.model.local.DatabaseHelper.Companion.T_CATEGORIES
import com.endterm.smart_spend.model.local.DatabaseHelper.Companion.T_WALLETS
import com.endterm.smart_spend.model.local.WalletDao
import com.endterm.smart_spend.model.remote.FirestoreService
import com.endterm.smart_spend.util.Session
import com.endterm.smart_spend.util.TxType

/** Tạo ví + danh mục mặc định lần đầu mở app. Nếu có đăng nhập, gọi SAU SyncController.pullAll để khỏi trùng. */
class SeedController(context: Context) : BaseController() {
    private val helper = DatabaseHelper.getInstance(context)
    private val walletDao = WalletDao(helper)
    private val categoryDao = CategoryDao(helper)

    fun ensureDefaults(onDone: (Result<Unit>) -> Unit) = async({
        val uid = Session.userId()
        if (walletDao.count(uid) == 0) {
            val w = Wallet(userId = uid, name = "Tiền mặt", type = "cash")
            walletDao.insert(w)
            FirestoreService.upsert(T_WALLETS, w.id, w.toMap())
        }
        if (categoryDao.count(uid) == 0) {
            DEFAULTS.forEach { (name, type, icon) ->
                val c = Category(userId = uid, name = name, type = type, icon = icon)
                categoryDao.insert(c)
                FirestoreService.upsert(T_CATEGORIES, c.id, c.toMap())
            }
        }
    }, onDone)

    private companion object {
        val DEFAULTS = listOf(
            Triple("Ăn uống", TxType.EXPENSE, "ic_food"),
            Triple("Di chuyển", TxType.EXPENSE, "ic_car"),
            Triple("Mua sắm", TxType.EXPENSE, "ic_bag"),
            Triple("Hóa đơn", TxType.EXPENSE, "ic_bolt"),
            Triple("Giải trí", TxType.EXPENSE, "ic_heart"),
            Triple("Lương", TxType.INCOME, "ic_wage"),
            Triple("Thưởng", TxType.INCOME, "ic_coin")
        )
    }
}
