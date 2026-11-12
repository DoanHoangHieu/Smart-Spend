package com.endterm.smart_spend.model.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * SQLite cục bộ - tạo đủ 18 bảng theo ERD.
 * Khóa chính là TEXT (UUID) để đồng bộ 1-1 với document id của Firestore.
 */
class DatabaseHelper private constructor(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DB_NAME, null, DB_VERSION) {

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)   // bắt buộc bật thì FK/CASCADE mới chạy
    }

    override fun onCreate(db: SQLiteDatabase) {
        SCHEMA.forEach(db::execSQL)
        INDEXES.forEach(db::execSQL)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Đồ án: tăng DB_VERSION rồi xóa-tạo lại. Bản thật cần ALTER TABLE theo từng version.
        db.setForeignKeyConstraintsEnabled(false)
        TABLES.forEach { db.execSQL("DROP TABLE IF EXISTS $it") }
        onCreate(db)
    }

    companion object {
        const val DB_NAME = "smartspend.db"
        const val DB_VERSION = 1

        // tên bảng = tên collection trên Firestore
        const val T_WALLETS = "wallets"
        const val T_CATEGORIES = "categories"
        const val T_TRANSACTIONS = "transactions"
        const val T_BUDGETS = "budgets"

        @Volatile private var instance: DatabaseHelper? = null
        fun getInstance(context: Context): DatabaseHelper =
            instance ?: synchronized(this) {
                instance ?: DatabaseHelper(context).also { instance = it }
            }

        private val TABLES = listOf(
            "users", "notifications", "wallets", "categories", "transactions", "budgets",
            "savings_goals", "goal_contributions", "debts", "debt_payments",
            "recurring_rules", "bill_reminders", "attachments",
            "expense_groups", "group_members", "group_expenses",
            "group_expense_shares", "group_settlements"
        )

        private val SCHEMA = listOf(
            // --- Người dùng ---
            """CREATE TABLE users(
                id TEXT PRIMARY KEY, email TEXT NOT NULL, display_name TEXT NOT NULL,
                password TEXT)""",   // mật khẩu do Firebase Auth quản lý -> để NULL, không lưu plaintext
            """CREATE TABLE notifications(
                id TEXT PRIMARY KEY, user_id TEXT NOT NULL, type TEXT NOT NULL,
                is_read INTEGER NOT NULL DEFAULT 0)""",

            // --- Ví / Danh mục / Giao dịch ---
            """CREATE TABLE wallets(
                id TEXT PRIMARY KEY, user_id TEXT NOT NULL, name TEXT NOT NULL,
                type TEXT NOT NULL DEFAULT 'cash', initial_balance INTEGER NOT NULL DEFAULT 0)""",
            """CREATE TABLE categories(
                id TEXT PRIMARY KEY, user_id TEXT NOT NULL, parent_id TEXT,
                name TEXT NOT NULL, type TEXT NOT NULL CHECK(type IN ('income','expense')),
                icon TEXT,
                FOREIGN KEY(parent_id) REFERENCES categories(id) ON DELETE SET NULL)""",
            """CREATE TABLE recurring_rules(
                id TEXT PRIMARY KEY, wallet_id TEXT NOT NULL, category_id TEXT NOT NULL,
                amount INTEGER NOT NULL, frequency TEXT NOT NULL,
                FOREIGN KEY(wallet_id) REFERENCES wallets(id) ON DELETE CASCADE,
                FOREIGN KEY(category_id) REFERENCES categories(id))""",
            """CREATE TABLE transactions(
                id TEXT PRIMARY KEY, wallet_id TEXT NOT NULL, category_id TEXT NOT NULL,
                recurring_rule_id TEXT, amount INTEGER NOT NULL CHECK(amount > 0), note TEXT,
                datetime TEXT NOT NULL, type TEXT NOT NULL CHECK(type IN ('income','expense')),
                image_url TEXT,
                FOREIGN KEY(wallet_id) REFERENCES wallets(id) ON DELETE CASCADE,
                FOREIGN KEY(category_id) REFERENCES categories(id),
                FOREIGN KEY(recurring_rule_id) REFERENCES recurring_rules(id) ON DELETE SET NULL)""",
            """CREATE TABLE attachments(
                id TEXT PRIMARY KEY, transaction_id TEXT NOT NULL, local_path TEXT NOT NULL,
                FOREIGN KEY(transaction_id) REFERENCES transactions(id) ON DELETE CASCADE)""",

            // --- Ngân sách ---
            """CREATE TABLE budgets(
                id TEXT PRIMARY KEY, category_id TEXT NOT NULL, period TEXT NOT NULL,
                limit_amount INTEGER NOT NULL, alert_threshold_pct INTEGER NOT NULL DEFAULT 80,
                FOREIGN KEY(category_id) REFERENCES categories(id) ON DELETE CASCADE)""",
            """CREATE TABLE bill_reminders(
                id TEXT PRIMARY KEY, recurring_rule_id TEXT NOT NULL, due_at TEXT NOT NULL,
                FOREIGN KEY(recurring_rule_id) REFERENCES recurring_rules(id) ON DELETE CASCADE)""",

            // --- Mục tiêu & Nợ ---
            """CREATE TABLE savings_goals(
                id TEXT PRIMARY KEY, name TEXT NOT NULL, target_amount INTEGER NOT NULL, deadline TEXT)""",
            """CREATE TABLE goal_contributions(
                id TEXT PRIMARY KEY, goal_id TEXT NOT NULL, wallet_id TEXT NOT NULL,
                amount INTEGER NOT NULL,
                FOREIGN KEY(goal_id) REFERENCES savings_goals(id) ON DELETE CASCADE,
                FOREIGN KEY(wallet_id) REFERENCES wallets(id))""",
            """CREATE TABLE debts(
                id TEXT PRIMARY KEY, direction TEXT NOT NULL CHECK(direction IN ('lend','borrow')),
                principal INTEGER NOT NULL, status TEXT NOT NULL DEFAULT 'open')""",
            """CREATE TABLE debt_payments(
                id TEXT PRIMARY KEY, debt_id TEXT NOT NULL, wallet_id TEXT NOT NULL,
                amount INTEGER NOT NULL,
                FOREIGN KEY(debt_id) REFERENCES debts(id) ON DELETE CASCADE,
                FOREIGN KEY(wallet_id) REFERENCES wallets(id))""",

            // --- Chia chi tiêu nhóm ---
            """CREATE TABLE expense_groups(
                id TEXT PRIMARY KEY, owner_id TEXT NOT NULL, name TEXT NOT NULL)""",
            """CREATE TABLE group_members(
                id TEXT PRIMARY KEY, group_id TEXT NOT NULL, user_id TEXT, display_name TEXT NOT NULL,
                FOREIGN KEY(group_id) REFERENCES expense_groups(id) ON DELETE CASCADE)""",
            """CREATE TABLE group_expenses(
                id TEXT PRIMARY KEY, group_id TEXT NOT NULL, paid_by_member_id TEXT NOT NULL,
                amount INTEGER NOT NULL,
                FOREIGN KEY(group_id) REFERENCES expense_groups(id) ON DELETE CASCADE,
                FOREIGN KEY(paid_by_member_id) REFERENCES group_members(id))""",
            """CREATE TABLE group_expense_shares(
                id TEXT PRIMARY KEY, expense_id TEXT NOT NULL, member_id TEXT NOT NULL,
                share_amount INTEGER NOT NULL,
                FOREIGN KEY(expense_id) REFERENCES group_expenses(id) ON DELETE CASCADE,
                FOREIGN KEY(member_id) REFERENCES group_members(id))""",
            """CREATE TABLE group_settlements(
                id TEXT PRIMARY KEY, group_id TEXT NOT NULL, from_member_id TEXT NOT NULL,
                to_member_id TEXT NOT NULL, amount INTEGER NOT NULL,
                FOREIGN KEY(group_id) REFERENCES expense_groups(id) ON DELETE CASCADE)"""
        )

        private val INDEXES = listOf(
            "CREATE INDEX idx_tx_wallet ON transactions(wallet_id)",
            "CREATE INDEX idx_tx_category ON transactions(category_id)",
            "CREATE INDEX idx_tx_datetime ON transactions(datetime)"
        )
    }
}

/**
 * UPSERT an toàn với FK. KHÔNG dùng CONFLICT_REPLACE vì REPLACE = xóa rồi chèn,
 * sẽ kích hoạt ON DELETE CASCADE và xóa nhầm bảng con (vd: ghi đè ví làm mất giao dịch).
 */
fun SQLiteDatabase.upsert(table: String, id: String, values: ContentValues) {
    if (update(table, values, "id = ?", arrayOf(id)) == 0) insertOrThrow(table, null, values)
}
