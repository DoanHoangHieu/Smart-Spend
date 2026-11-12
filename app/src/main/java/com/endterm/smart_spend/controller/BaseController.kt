package com.endterm.smart_spend.controller

import android.os.Handler
import android.os.Looper
import java.util.concurrent.Executors

/**
 * Controller chạy việc DB ở luồng nền, trả kết quả về luồng chính cho View.
 * View chỉ nhận Result<T> -> không biết gì về SQLite/Firestore.
 */
abstract class BaseController {

    protected fun <T> async(work: () -> T, onDone: (Result<T>) -> Unit) {
        IO.execute {
            val result = runCatching(work)
            MAIN.post { onDone(result) }
        }
    }

    private companion object {
        val IO = Executors.newSingleThreadExecutor()   // 1 luồng => ghi SQLite tuần tự, không tranh chấp
        val MAIN = Handler(Looper.getMainLooper())
    }
}
