package com.endterm.smart_spend

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment

class MainActivity : AppCompatActivity() {

    private lateinit var navItems: List<View>
    private lateinit var fab: View

    private val tabs: List<() -> Fragment> = listOf(
        { HomeFragment() },
        { HistoryFragment() },
        { BudgetFragment() },
        { GroupFragment() },
        { ReportFragment() }
    )

    // Tab nào có nút "+" (theo thiết kế: Trang chủ và Lịch sử)
    private val tabsWithFab = setOf(0, 1)

    private var currentTab = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fab = findViewById(R.id.fabAdd)
        navItems = listOf(
            R.id.navHome, R.id.navHistory, R.id.navBudget, R.id.navGroup, R.id.navReport
        ).map { findViewById(it) }

        navItems.forEachIndexed { index, item ->
            item.setOnClickListener { selectTab(index) }
        }

        fab.setOnClickListener {
            // TODO: mở màn "Thêm giao dịch" (layout activity_add_transaction)
        }

        if (savedInstanceState == null) {
            selectTab(0)
        } else {
            // Fragment đã được hệ thống khôi phục, chỉ cập nhật lại giao diện thanh điều hướng
            currentTab = savedInstanceState.getInt(KEY_TAB, 0)
            updateNavState(currentTab)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_TAB, currentTab)
    }

    private fun selectTab(index: Int) {
        if (index == currentTab && supportFragmentManager.findFragmentById(R.id.fragmentContainer) != null) return
        currentTab = index
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, tabs[index]())
            .commit()
        updateNavState(index)
    }

    private fun updateNavState(index: Int) {
        navItems.forEachIndexed { i, item -> item.isSelected = i == index }
        fab.visibility = if (index in tabsWithFab) View.VISIBLE else View.GONE
    }

    companion object {
        private const val KEY_TAB = "current_tab"
    }
}
