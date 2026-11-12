package com.endterm.smart_spend.controller

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import com.endterm.smart_spend.model.entity.Category
import com.endterm.smart_spend.model.local.CategoryDao
import com.endterm.smart_spend.model.local.DatabaseHelper
import com.endterm.smart_spend.model.local.DatabaseHelper.Companion.T_CATEGORIES
import com.endterm.smart_spend.model.remote.FirestoreService
import com.endterm.smart_spend.util.Session

class CategoryController(context: Context) : BaseController() {
    private val dao = CategoryDao(DatabaseHelper.getInstance(context))

    fun create(name: String, type: String, icon: String, onDone: (Result<Category>) -> Unit) = async({
        require(name.isNotBlank()) { "Tên danh mục không được để trống" }
        val c = Category(userId = Session.userId(), name = name.trim(), type = type, icon = icon)
        dao.insert(c)
        FirestoreService.upsert(T_CATEGORIES, c.id, c.toMap())
        c
    }, onDone)

    fun list(type: String? = null, onDone: (Result<List<Category>>) -> Unit) =
        async({ dao.getAll(Session.userId(), type) }, onDone)

    fun update(c: Category, onDone: (Result<Unit>) -> Unit) = async({
        require(c.name.isNotBlank()) { "Tên danh mục không được để trống" }
        check(dao.update(c) > 0) { "Không tìm thấy danh mục" }
        FirestoreService.upsert(T_CATEGORIES, c.id, c.toMap())
    }, onDone)

    fun delete(id: String, onDone: (Result<Unit>) -> Unit) = async({
        try {
            dao.delete(id)
        } catch (e: SQLiteConstraintException) {
            throw IllegalStateException("Danh mục đang có giao dịch, hãy xóa/chuyển giao dịch trước", e)
        }
        FirestoreService.delete(T_CATEGORIES, id)
    }, onDone)
}
