package com.endterm.smart_spend.view.activity

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.endterm.smart_spend.R
import com.endterm.smart_spend.controller.CategoryController
import com.endterm.smart_spend.controller.TransactionController
import com.endterm.smart_spend.controller.WalletController
import com.endterm.smart_spend.model.entity.Category
import com.endterm.smart_spend.model.entity.Transaction
import com.endterm.smart_spend.model.entity.Wallet
import com.endterm.smart_spend.util.TxType

/**
 * VIEW dùng chung cho Thêm (không có EXTRA_TX_ID) và Sửa (có EXTRA_TX_ID).
 * Cần thêm id vào activity_add_transaction.xml:
 *   tabExpense, tabIncome (TextView) | edtAmount (đổi TextView số tiền -> EditText, inputType=number)
 *   edtNote (EditText) | spnWallet, spnCategory (Spinner) | btnSave (Button/TextView) | btnClose (ImageView)
 */
class AddTransactionActivity : AppCompatActivity() {

    private lateinit var txCtl: TransactionController
    private lateinit var walletCtl: WalletController
    private lateinit var categoryCtl: CategoryController

    private lateinit var edtAmount: EditText
    private lateinit var edtNote: EditText
    private lateinit var spnWallet: Spinner
    private lateinit var spnCategory: Spinner
    private lateinit var tabExpense: TextView
    private lateinit var tabIncome: TextView

    private var type = TxType.EXPENSE
    private var wallets: List<Wallet> = emptyList()
    private var categories: List<Category> = emptyList()
    private var editing: Transaction? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_transaction)

        txCtl = TransactionController(this)
        walletCtl = WalletController(this)
        categoryCtl = CategoryController(this)

        edtAmount = findViewById(R.id.edtAmount)
        edtNote = findViewById(R.id.edtNote)
        spnWallet = findViewById(R.id.spnWallet)
        spnCategory = findViewById(R.id.spnCategory)
        tabExpense = findViewById(R.id.tabExpense)
        tabIncome = findViewById(R.id.tabIncome)

        tabExpense.setOnClickListener { switchType(TxType.EXPENSE) }
        tabIncome.setOnClickListener { switchType(TxType.INCOME) }
        findViewById<View>(R.id.btnClose).setOnClickListener { finish() }
        findViewById<View>(R.id.btnSave).setOnClickListener { save() }

        val editId = intent.getStringExtra(EXTRA_TX_ID)
        if (editId == null) loadWallets() else loadForEdit(editId)
    }

    // ---------- đọc dữ liệu cho form ----------
    private fun loadForEdit(id: String) {
        txCtl.get(id) { r ->
            r.onSuccess { t ->
                if (t == null) { toast("Không tìm thấy giao dịch"); finish(); return@onSuccess }
                editing = t
                type = t.type
                edtAmount.setText(t.amount.toString())
                edtNote.setText(t.note)
                loadWallets()
            }.onFailure { toast(it.message) }
        }
    }

    private fun loadWallets() {
        walletCtl.list { r ->
            r.onSuccess { list ->
                wallets = list
                bindSpinner(spnWallet, list.map { it.name })
                editing?.let { e -> spnWallet.setSelection(list.indexOfFirst { it.id == e.walletId }.coerceAtLeast(0)) }
                loadCategories()
            }.onFailure { toast(it.message) }
        }
    }

    private fun loadCategories() {
        updateTabs()
        categoryCtl.list(type) { r ->
            r.onSuccess { list ->
                categories = list
                bindSpinner(spnCategory, list.map { it.name })
                // chỉ chọn lại danh mục cũ khi loại giao dịch chưa bị đổi (danh mục thu/chi là hai tập khác nhau)
                editing?.takeIf { it.type == type }?.let { e ->
                    spnCategory.setSelection(list.indexOfFirst { it.id == e.categoryId }.coerceAtLeast(0))
                }
            }.onFailure { toast(it.message) }
        }
    }

    private fun bindSpinner(spinner: Spinner, names: List<String>) {
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, names)
    }

    private fun switchType(newType: String) {
        if (newType == type) return
        type = newType
        loadCategories()
    }

    private fun updateTabs() {
        tabExpense.isSelected = type == TxType.EXPENSE
        tabIncome.isSelected = type == TxType.INCOME
    }

    // ---------- Create / Update ----------
    private fun save() {
        val amount = edtAmount.text.toString().filter { it.isDigit() }.toLongOrNull() ?: 0L
        val wallet = wallets.getOrNull(spnWallet.selectedItemPosition)
        val category = categories.getOrNull(spnCategory.selectedItemPosition)
        if (wallet == null || category == null) { toast("Chưa có ví hoặc danh mục"); return }

        val old = editing
        val tx = old?.copy(
            walletId = wallet.id, categoryId = category.id,
            amount = amount, type = type, note = edtNote.text.toString().trim()
        ) ?: Transaction(
            walletId = wallet.id, categoryId = category.id,
            amount = amount, type = type, note = edtNote.text.toString().trim()
        )

        val done: (Result<Unit>) -> Unit = { r ->
            r.onSuccess { finish() }.onFailure { toast(it.message) }
        }
        if (old == null) txCtl.add(tx, done) else txCtl.update(tx, done)
    }

    private fun toast(msg: String?) =
        Toast.makeText(this, msg ?: "Có lỗi xảy ra", Toast.LENGTH_SHORT).show()

    companion object {
        const val EXTRA_TX_ID = "tx_id"
    }
}
