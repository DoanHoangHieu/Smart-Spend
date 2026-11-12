package com.endterm.smart_spend.view.fragment

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.endterm.smart_spend.R
import com.endterm.smart_spend.controller.TransactionController
import com.endterm.smart_spend.model.entity.Transaction
import com.endterm.smart_spend.view.activity.AddTransactionActivity
import com.endterm.smart_spend.view.adapter.TransactionAdapter

/**
 * VIEW của màn Lịch sử: chỉ hiển thị + chuyển sự kiện cho Controller.
 * Cần thêm vào fragment_history.xml: RecyclerView id=rvTransactions, TextView id=tvEmpty.
 */
class HistoryFragment : Fragment(R.layout.fragment_history) {

    private lateinit var controller: TransactionController
    private lateinit var adapter: TransactionAdapter
    private lateinit var tvEmpty: TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        controller = TransactionController(requireContext())
        tvEmpty = view.findViewById(R.id.tvEmpty)

        adapter = TransactionAdapter(
            onClick = { openEditor(it) },
            onLongClick = { confirmDelete(it) }
        )
        view.findViewById<RecyclerView>(R.id.rvTransactions).apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@HistoryFragment.adapter
        }
    }

    override fun onResume() {
        super.onResume()
        reload()                       // quay lại từ màn Thêm/Sửa là danh sách tự cập nhật
    }

    private fun reload() {
        controller.list { result ->
            if (!isAdded) return@list  // fragment có thể đã bị gỡ khi kết quả về
            result
                .onSuccess { items ->
                    adapter.submitList(items)
                    tvEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
                }
                .onFailure { toast(it.message) }
        }
    }

    private fun openEditor(t: Transaction) {
        startActivity(Intent(requireContext(), AddTransactionActivity::class.java)
            .putExtra(AddTransactionActivity.EXTRA_TX_ID, t.id))
    }

    private fun confirmDelete(t: Transaction) {
        AlertDialog.Builder(requireContext())
            .setTitle("Xóa giao dịch?")
            .setMessage("Thao tác này không thể hoàn tác.")
            .setNegativeButton("Hủy", null)
            .setPositiveButton("Xóa") { _, _ ->
                controller.delete(t.id) { r -> r.onSuccess { reload() }.onFailure { toast(it.message) } }
            }
            .show()
    }

    private fun toast(msg: String?) {
        if (isAdded) Toast.makeText(requireContext(), msg ?: "Có lỗi xảy ra", Toast.LENGTH_SHORT).show()
    }
}
