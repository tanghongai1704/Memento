# Tiến độ MVP

Đây là nơi theo dõi chính sau mỗi đợt làm việc. Mỗi đợt cập nhật: đã làm → bằng chứng kiểm tra → giới hạn/còn thiếu → việc tiếp theo. Không đánh dấu hoàn tất chỉ vì có model hoặc màn hình.

## Lộ trình

| Bước | Nội dung | Trạng thái |
|---|---|---|
| 1 | Môi trường Firebase, dữ liệu cũ, Rules/index và quy tắc MVP | Hoàn tất — 13/09/2026 |
| 2 | Auth/profile tối thiểu và quên mật khẩu | Tiếp theo |
| 3 | Direct invite và transaction chống trùng | Chưa bắt đầu |
| 4 | Đăng một ảnh thật: Room → Storage → Firestore | Chưa bắt đầu |
| 5 | Đồng bộ người nhận, Home, pagination | Chưa bắt đầu |
| 6 | History, soft delete và disconnect | Chưa bắt đầu |
| 7 | Offline, retry, khôi phục sau tắt app, cleanup | Chưa bắt đầu |
| 8 | Kiểm thử toàn hành trình trên hai thiết bị | Chưa bắt đầu |

Mốc demo: hoàn thành bước 5. Mốc MVP tạm ổn: hoàn thành bước 8. Quy tắc làm việc ở [mvp-baseline](mvp-baseline.md).

## Nền trước bước 1 — đã có

Refactor schema User/Connection/Member/Post/Media/Invite; Room v3 và migration v2→v3; Auth/profile recovery; đọc connection theo membership; Home/History đọc Room. Đã gỡ request cũ và thành công upload giả. Đã qua build Android, 1 unit test username, 5 kiểm tra SQL và 16 kiểm tra Rules Emulator ở đợt trước.

Đây chưa phải luồng chia sẻ hoàn chỉnh. Chưa triển khai invite, upload hay post sync. Forgot Password còn placeholder; profile chưa có dữ liệu hiển thị/chỉnh sửa đầy đủ.

## 13/09/2026 — bước 1

### Đã làm

- Xác định app trỏ project `memento-fre`, database `(default)` ở `asia-southeast1`, bucket `memento-fre.firebasestorage.app`.
- Kiểm kê từ API thật: Email/Password đã bật; 3 tài khoản không bị disable; chỉ có 3 user profile legacy, không có collection connection/request tại thời điểm kiểm tra.
- Phát hiện Firestore dùng test-mode mở read/write đến 14/09/2026; Storage deny-all; chưa có composite index.
- Chuẩn bị snapshot Rules và backup 3 profiles trong `.local/firebase-audit/` (gitignored); không đưa email/UID người dùng vào docs hoặc commit.
- Thêm alias project rõ ràng, cấu hình Emulator Auth/Firestore và baseline Storage Rules deny-all.
- Chốt phạm vi direct + một ảnh cùng giới hạn và chính sách vòng đời trong `mvp-baseline.md`.

- Đã chuyển cả 3 profile thật sang 9 field schema mới bằng atomic batch có updateTime precondition. Giữ UID, username và createdAt; email vẫn ở Auth. Không xóa tài khoản hay collection.
- Đã deploy Firestore Rules và 2 composite indexes lên `memento-fre`. Rules thật khớp file trong repo; hai index connections/posts đều READY. Storage giữ nguyên deny-all; file local bổ sung để quản lý phiên bản.

### Bằng chứng kiểm tra

- 16 kiểm tra Rules trên Firestore Emulator đạt.
- 12 kiểm tra Auth/Profile trên Auth + Firestore Emulator đạt: hai tài khoản đăng ký, tạo profile, đăng nhập lại giữ đúng UID/profile, chặn sửa profile chéo và truy cập không đăng nhập.
- Đọc lại Firebase thật: 3 tài khoản Auth vẫn hoạt động; 3 profile đủ field và giữ thông tin gốc; 2 indexes READY; Rules release ngày 13/09/2026 khớp repo.
- Probe không đăng nhập vào collection users trên Firebase thật trả HTTP 403.
- Chỉ thay đổi cấu hình/docs/test trong bước 1, không đổi Android code; không chạy lại build Android không cần thiết. Build của đợt nền trước đã đạt.

### Chốt bước 1

Môi trường Firebase thật đã dùng baseline mới, không còn phụ thuộc Rules test-mode hết hạn. Có thể dùng tài khoản cũ đăng nhập với code hiện tại. Chưa kiểm thử UI Android trên hai thiết bị hoặc mật khẩu tài khoản thật; smoke test hai tài khoản ở trên là emulator, không phải kiểm thử app end-to-end.

Invite, upload/post sync và quyền Storage media chưa triển khai. Home trống là dự kiến vì chưa có connection/post. GROUP/VIDEO nằm ngoài bản MVP đầu. Bản sao lưu `.local/firebase-audit/` chỉ nằm trên máy và bị gitignore; đây không phải backup đầy đủ cho cả Firebase.

### Tiếp theo — bước 2

Hiển thị profile từ Room, chỉnh sửa displayName/username/bio qua repository; validation đồng nhất; gửi email reset password; kiểm tra đăng ký/login lỗi profile và chuyển tài khoản. Avatar upload chờ hạ tầng Storage. Không bắt đầu invite trước khi chốt bước 2.
