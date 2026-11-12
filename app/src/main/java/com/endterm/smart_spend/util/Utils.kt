package com.endterm.smart_spend.util

import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/** PK trong ERD là text -> dùng UUID để SQLite và Firestore dùng chung một id. */
object Ids {
    fun new(): String = UUID.randomUUID().toString()
}

object Dates {
    private val fmt get() = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
    fun now(): String = fmt.format(Date())
    /** "2026-11-12 12:10" -> "12:10" để hiển thị. */
    fun timeOf(datetime: String): String = datetime.substringAfter(' ', datetime)
}

object Money {
    fun format(amount: Long): String = String.format(Locale("vi", "VN"), "%,d", amount) + "đ"
}

/** Chưa đăng nhập vẫn dùng được app (offline) với user "local". */
object Session {
    const val LOCAL_USER = "local"
    fun userId(): String = FirebaseAuth.getInstance().currentUser?.uid ?: LOCAL_USER
    fun isSignedIn(): Boolean = FirebaseAuth.getInstance().currentUser != null
}

object TxType {
    const val INCOME = "income"
    const val EXPENSE = "expense"
}
