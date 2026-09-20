# Moment Recap — đề xuất, chưa triển khai

Tổng hợp ảnh theo tuần hoặc tháng trong một connection, dùng `posts`/`media_items` ở Room. Chỉ lấy post ACTIVE của connection ACTIVE mà current user còn membership ACTIVE. VIDEO và GROUP chỉ được đưa vào recap sau khi hai flow đó được triển khai và chốt quyền lịch sử.

Tuần bắt đầu thứ hai, tháng theo lịch; khoảng trống không tạo recap. Ảnh sắp theo `createdAt`; không chọn ảnh bằng AI ở bản đầu.

Nếu bổ sung bảng recap, phải tham chiếu `connectionId` và post IDs, tránh sao chép display name/avatar. Không có model Message hay schema Media riêng trong dự án hiện tại. Realtime post sync, cache media, soft delete và thu hồi membership đã có; phần còn thiếu là model/DAO/UI recap và quy tắc chọn khoảng thời gian.
