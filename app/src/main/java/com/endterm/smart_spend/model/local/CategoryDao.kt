package com.endterm.smart_spend.model.local

import android.content.ContentValues
import android.database.Cursor
import com.endterm.smart_spend.model.entity.Category
import com.endterm.smart_spend.model.local.DatabaseHelper.Companion.T_CATEGORIES

class CategoryDao(private val helper: DatabaseHelper) {

    private fun Category.toValues() = ContentValues().apply {
        put("id", id); put("user_id", userId); put("parent_id", parentId)
        put("name", name); put("type", type); put("icon", icon)
    }

    private fun Cursor.toCategory() = Category(
        id = getString(getColumnIndexOrThrow("id")),
        userId = getString(getColumnIndexOrThrow("user_id")),
        parentId = getString(getColumnIndexOrThrow("parent_id")),   // null nếu NULL
        name = getString(getColumnIndexOrThrow("name")),
        type = getString(getColumnIndexOrThrow("type")),
        icon = getString(getColumnIndexOrThrow("icon")) ?: "ic_tag"
    )

    fun insert(c: Category) { helper.writableDatabase.insertOrThrow(T_CATEGORIES, null, c.toValues()) }

    /** type = null -> lấy tất cả. */
    fun getAll(userId: String, type: String? = null): List<Category> {
        val where = if (type == null) "user_id = ?" else "user_id = ? AND type = ?"
        val args = if (type == null) arrayOf(userId) else arrayOf(userId, type)
        return helper.readableDatabase.query(T_CATEGORIES, null, where, args, null, null, "name").use { c ->
            buildList { while (c.moveToNext()) add(c.toCategory()) }
        }
    }

    fun getById(id: String): Category? =
        helper.readableDatabase.query(T_CATEGORIES, null, "id = ?", arrayOf(id), null, null, null).use { c ->
            if (c.moveToFirst()) c.toCategory() else null
        }

    fun count(userId: String): Int =
        helper.readableDatabase.rawQuery("SELECT COUNT(*) FROM categories WHERE user_id = ?", arrayOf(userId))
            .use { it.moveToFirst(); it.getInt(0) }

    fun update(c: Category): Int =
        helper.writableDatabase.update(T_CATEGORIES, c.toValues(), "id = ?", arrayOf(c.id))

    /** Ném SQLiteConstraintException nếu danh mục còn giao dịch (FK không CASCADE) - Controller bắt lỗi này. */
    fun delete(id: String): Int =
        helper.writableDatabase.delete(T_CATEGORIES, "id = ?", arrayOf(id))

    fun upsert(c: Category) = helper.writableDatabase.upsert(T_CATEGORIES, c.id, c.toValues())
}
