# Trạng thái và phạm vi hiện tại

Tài liệu này là snapshot hiện trạng, không phải nhật ký theo đợt. Chi tiết kiến trúc, schema và lifecycle được giữ ở các tài liệu chuyên biệt để tránh lặp và lệch nội dung.

## Đã triển khai

- Firebase Auth email/password, profile theo UID, chỉnh sửa profile và reset password.
- Mã kết nối cố định, redeem DIRECT qua backend transaction, chống self-connect và connection trùng.
- Realtime connection sync, nhãn người dùng, disconnect và thu hồi quyền remote.
- Post PHOTO gồm 1–5 ảnh với bốn layout; xử lý JPEG, draft local, progress upload, finalize idempotent và retry sau process death.
- Feed Room-first, một collection-group query lấy 20 bài mới nhất trên toàn bộ connection, tải/cache media, phân trang All/từng connection.
- Soft delete có tombstone; cleanup Storage cho orphan/media đã xóa sau grace period.
- Offline fallback cho profile/feed đã cache; cache media giới hạn dung lượng và có guard theo UID/membership.

## Chưa triển khai hoặc chưa hoàn thiện

- GROUP connection/invite và chính sách lịch sử cho group.
- VIDEO upload/transcode/playback.
- Avatar upload.
- Bộ lọc Home nâng cao theo chiều gửi/nhận, loại post và khoảng thời gian dù DAO đã có nền tảng.
- Retry nền tự động; hiện người dùng chủ động retry hoặc discard draft.
- Hard-delete metadata Firestore và cơ chế thu hồi bản sao đã được lưu ngoài app.

## Invariant cần giữ

- Client không được tự ghi connection, membership, invite lookup hoặc Post remote; mutation đi qua Callable Functions.
- `postId`, `mediaId`, Storage path và nội dung draft không đổi qua retry.
- Home chỉ đọc post ACTIVE trong connection/membership ACTIVE của UID hiện tại.
- Disconnect tạo ranh giới lịch sử: kết nối lại dùng connection ID mới và không mở lại dữ liệu cũ.
- Cleanup chỉ xử lý path hợp lệ sau grace period; không xóa media của post ACTIVE.

## Tài liệu nguồn

- [Kiến trúc](architecture.md)
- [Schema](data-schema.md)
- [Lifecycle post ảnh](photo-post-lifecycle.md)
- [Môi trường Firebase](firebase-environment.md)
- [Hướng dẫn review code](code-review-guide.md)
