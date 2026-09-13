# Moment Recap — đề xuất, chưa triển khai

Tổng hợp ảnh/video theo tuần hoặc tháng trong một connection, dùng posts/media_items ở Room. Chỉ lấy post ACTIVE của connection ACTIVE mà current user còn membership ACTIVE. Group cần cùng chính sách quyền lịch sử như feed.

Tuần bắt đầu thứ hai, tháng theo lịch; khoảng trống không tạo recap. Ảnh theo createdAt, video dùng thumbnailPath; không ghép video hoặc chọn ảnh bằng AI ở bản đầu.

Nếu bổ sung bảng recap, phải tham chiếu connectionId và post IDs, tránh sao chép displayName/avatar. Không có model Message hay schema Media riêng trong dự án hiện tại. Cần hoàn thành đồng bộ post, cache media và xử lý xóa/thu hồi membership trước tính năng này.
