# Kiến trúc hiện tại

`app` quản lý navigation và binding Hilt. Mỗi feature có `presentation`, `domain`, `data`.
`core/domain` chứa model Kotlin; `core/database` chứa Room v3; `core/network` cung cấp Firebase; `core/designsystem` chứa theme.

## Luồng dữ liệu

AuthRepository → Firebase Auth → transaction profile → UserDao. Login chỉ thành công sau khi profile sẵn sàng. ProfileViewModel cũng dùng AuthRepository để đọc/sửa profile; Firestore là nguồn xác nhận và Room được cập nhật sau khi remote thành công. ForgotPasswordViewModel gọi Firebase Auth qua cùng repository.
ConnectionRepository → snapshot listener trên query Firestore có membership → transaction Room cập nhật connection/members. Profile của thành viên được đọc trực tiếp theo UID. Vì listener theo dõi query chứa `memberIds`, creator nhận connection mới ngay khi người khác redeem code mà không cần đăng nhập lại hoặc tự refresh.
Tạo/redeem/revoke invite đi từ ConnectionRepository tới Callable Functions ở `asia-southeast1`. Functions dùng Admin SDK và Firestore transaction để cập nhật invite, connection, hai member, unique lock và rate limit; Android không có quyền ghi trực tiếp các document này. Redeem đã commit vẫn được báo thành công nếu lần refresh Room ngay sau đó lỗi, tránh hướng người dùng dùng lại code đã tiêu.
HomeRepository/HistoryRepository → Room, không truy vấn Firestore từ Compose. Home lọc theo connectionId. PostDao hỗ trợ All/My/Received, loại media và khoảng thời gian.

Room query kiểm tra cả connection ACTIVE lẫn membership ACTIVE của tài khoản hiện tại. Đồng bộ connection đánh dấu membership local LEFT khi không còn xuất hiện ở truy vấn server. Room connection dùng Upsert để không vô tình cascade-delete members như INSERT OR REPLACE.

`LocalMediaItem` chỉ dùng picker/nén với URI local. `MediaItem` là metadata persisted, dùng Storage path, không mang bitmap/Base64. Post dùng connectionId và postType PHOTO/VIDEO.

## Giới hạn của giai đoạn này

Chưa có worker/realtime listener/pagination đồng bộ posts, nên Home/History chỉ hiển thị dữ liệu đã nằm trong Room. Thu hồi quyền offline chỉ có hiệu lực sau đồng bộ; chưa có TTL cache riêng.
History UI hiện vẫn hiển thị danh sách chuỗi từ post thật; bộ lọc đã có ở DAO nhưng chưa có UI đầy đủ.
Direct invite đã có backend và UI. Code chỉ giữ trong UI memory để chia sẻ, không lưu raw vào Firestore; mở lại app thì tạo mã mới. GROUP invite chưa triển khai.
PostRepository hiện trả lỗi chưa khả dụng, tránh thành công giả trước khi pipeline Storage/Firestore/Room được triển khai.

## Migration

Room v2 → v3 giữ users và membership, bỏ email public cache và bảng request cũ, thêm metadata schema và bảng posts/media_items. Connections legacy được giữ dưới CLOSED cho đến khi dữ liệu remote được chuyển đổi và sync lại. Không dùng destructive fallback. Database v1 chưa có migration được kiểm chứng; không tự xóa dữ liệu khi gặp phiên bản không hỗ trợ.
Firestore không tự migrate khi tăng Room version; xem [data-schema](data-schema.md).
