// ===== MainActivity.kt : thay 2 chỗ =====

// (1) Nút "+" mở màn thêm giao dịch (thay đoạn TODO)
fab.setOnClickListener {
    startActivity(Intent(this, AddTransactionActivity::class.java))
}

// (2) Trong onCreate, sau setContentView(...): tạo ví + danh mục mặc định ở lần chạy đầu
SeedController(this).ensureDefaults { /* bỏ qua kết quả; lỗi sẽ hiện khi dùng */ }

// Nếu đã đăng nhập Firebase, kéo dữ liệu cloud về TRƯỚC rồi mới seed để không bị trùng:
// SyncController(this).pullAll { SeedController(this).ensureDefaults { } }
