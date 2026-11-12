package com.endterm.smart_spend.model.remote

import com.endterm.smart_spend.util.Session
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Lớp duy nhất nói chuyện với Firestore. Cấu trúc:
 *   users/{uid}/wallets/{id} | categories/{id} | transactions/{id} | budgets/{id}
 *
 * Firestore SDK tự bật cache offline và hàng đợi ghi: gọi set()/delete() khi mất mạng
 * thì dữ liệu được giữ lại và tự đẩy lên khi có mạng trở lại -> không cần tự viết cờ "synced".
 * Chưa đăng nhập (user "local") thì bỏ qua, app vẫn chạy 100% bằng SQLite.
 */
object FirestoreService {

    private val db: FirebaseFirestore get() = FirebaseFirestore.getInstance()

    private fun col(name: String): CollectionReference? =
        if (!Session.isSignedIn()) null
        else db.collection("users").document(Session.userId()).collection(name)

    /** Thêm mới hoặc ghi đè (dùng cho cả Create và Update). Không chặn luồng gọi. */
    fun upsert(collection: String, id: String, data: Map<String, Any?>) {
        col(collection)?.document(id)?.set(data)
    }

    fun delete(collection: String, id: String) {
        col(collection)?.document(id)?.delete()
    }

    /** Kéo toàn bộ một collection về (dùng sau khi đăng nhập / cài lại máy). */
    fun fetchAll(
        collection: String,
        onSuccess: (List<Map<String, Any?>>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val ref = col(collection) ?: return onSuccess(emptyList())
        ref.get()
            .addOnSuccessListener { snap -> onSuccess(snap.documents.mapNotNull { it.data }) }
            .addOnFailureListener(onError)
    }
}
