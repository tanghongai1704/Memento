# Kiến trúc hiện tại

`app` quản lý navigation và binding Hilt. Mỗi feature có `presentation`, `domain`, `data`.
`core/domain` chứa model Kotlin; `core/database` chứa Room v3; `core/network` cung cấp Firebase; `core/designsystem` chứa theme.

## Luồng dữ liệu

AuthRepository → Firebase Auth → transaction profile → UserDao. Login chỉ thành công sau khi profile sẵn sàng. ProfileViewModel cũng dùng AuthRepository để đọc/sửa profile; Firestore là nguồn xác nhận và Room được cập nhật sau khi remote thành công. ForgotPasswordViewModel gọi Firebase Auth qua cùng repository.
ConnectionRepository → snapshot listener trên query Firestore có membership → transaction Room cập nhật connection/members. Profile của thành viên được đọc trực tiếp theo UID. Vì listener theo dõi query chứa `memberIds`, creator nhận connection mới ngay khi người khác redeem code mà không cần đăng nhập lại hoặc tự refresh.
Các màn hình hiển thị connection DIRECT ghép membership ACTIVE với profile của người còn lại và dùng nhãn `displayName (@username)`; `connectionId` vẫn là khóa chọn/lọc. Khi dựng Connection từ Room phải truyền cả member entities, tránh mất quan hệ và rơi về nhãn chung `Direct connection`.
Sau khi profile được tạo hoặc đồng bộ, AuthRepository gọi `getMyInviteCode` để đảm bảo user có một mã cố định; Profile hiển thị và cho share mã đó. Redeem đi từ ConnectionRepository tới Callable Functions ở `asia-southeast1`. Functions dùng Admin SDK và Firestore transaction để đọc lookup, tạo connection, hai member và unique lock; Android không có quyền ghi trực tiếp các document này. Nhập lại code của một cặp đã connect trả về connection cũ, không tạo trùng.
HomeRepository/HistoryRepository → Room, không truy vấn Firestore từ Compose. Home lọc theo connectionId. PostDao hỗ trợ All/My/Received, loại media và khoảng thời gian.
PostRepository → Room PENDING/FAILED/SYNCED + xử lý JPEG local → Firebase Storage → Callable `finalizePhotoPost`. Callable đọc metadata object thật rồi dùng Firestore transaction tạo Post và cập nhật `connection.lastPostAt`; Android không tự ghi Post remote. Retry giữ nguyên postId/mediaId/path nên không nhân đôi bài. Home người gửi render file trong cache local; downloader cho người nhận được bổ sung ở bước 5.

Room query kiểm tra cả connection ACTIVE lẫn membership ACTIVE của tài khoản hiện tại. Đồng bộ connection đánh dấu membership local LEFT khi không còn xuất hiện ở truy vấn server. Room connection dùng Upsert để không vô tình cascade-delete members như INSERT OR REPLACE.

`LocalMediaItem` chỉ dùng picker/nén với URI local. `MediaItem` là metadata persisted, dùng Storage path, không mang bitmap/Base64. Post dùng connectionId và postType PHOTO/VIDEO.

## Giới hạn của giai đoạn này

Chưa có worker/realtime listener/pagination đồng bộ posts, nên Home/History chỉ hiển thị dữ liệu đã nằm trong Room. Thu hồi quyền offline chỉ có hiệu lực sau đồng bộ; chưa có TTL cache riêng.
History UI hiện vẫn hiển thị danh sách chuỗi từ post thật; bộ lọc đã có ở DAO nhưng chưa có UI đầy đủ.
Direct code đã có backend và UI. Code được backend lưu trong collection private để Profile có thể hiển thị lại sau khi mở app; client khác không được đọc/list collection này. Code cố định, không expire và không revoke. GROUP invite chưa triển khai.
Pipeline gửi một ảnh đã hoạt động. Chưa có listener/pagination/download post phía người nhận; Home/History chỉ hiển thị các Post đã có trong Room của tài khoản đó. Cleanup orphan Storage chưa tự động.

## Migration

Room v2 → v3 giữ users và membership, bỏ email public cache và bảng request cũ, thêm metadata schema và bảng posts/media_items. Connections legacy được giữ dưới CLOSED cho đến khi dữ liệu remote được chuyển đổi và sync lại. Không dùng destructive fallback. Database v1 chưa có migration được kiểm chứng; không tự xóa dữ liệu khi gặp phiên bản không hỗ trợ.
Firestore không tự migrate khi tăng Room version; xem [data-schema](data-schema.md).
