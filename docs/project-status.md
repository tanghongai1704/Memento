# Trạng thái và phạm vi project

Tài liệu này là snapshot của implementation hiện tại. Kiến trúc, schema và lifecycle được mô tả trong các tài liệu chuyên biệt để tránh lặp nội dung.

## Đã triển khai

- Firebase Authentication bằng email/password, profile theo UID, chỉnh sửa profile và reset password.
- Invite code cố định cho mỗi tài khoản; redeem DIRECT qua backend transaction, chặn self-connect và connection trùng.
- Realtime connection sync, nhãn người dùng, disconnect và thu hồi quyền remote.
- Post PHOTO gồm 1–5 ảnh với bốn layout; xử lý JPEG, draft local, progress upload, finalize idempotent và retry sau process death.
- Feed Room-first, collection-group query lấy 20 post mới nhất trên toàn bộ connection, cache media và phân trang cho All hoặc từng connection.
- Soft delete có tombstone; scheduled cleanup dọn orphan và media đã xóa sau grace period.
- Offline fallback cho profile và feed đã cache; media cache có giới hạn dung lượng và được guard theo UID/membership.

## Chưa triển khai hoặc cần phát triển thêm

- GROUP connection, group invite và chính sách xem lịch sử.
- VIDEO upload, transcoding, thumbnail, playback và cache policy.
- Avatar upload.
- Bộ lọc feed nâng cao theo chiều gửi/nhận, loại post và khoảng thời gian. DAO đã có điều kiện lọc nhưng UI hiện chỉ cung cấp All hoặc từng connection.
- Telemetry cho background upload và chính sách retry dài hạn. WorkManager hiện đã chờ mạng, retry theo backoff, sau đó cho phép người dùng retry hoặc discard thủ công.
- Hard-delete metadata Firestore và khả năng thu hồi bản sao đã được người dùng lưu ra ngoài app.

## Invariant cần giữ

- Client không tự ghi connection, membership, invite lookup hoặc post remote; các mutation này đi qua Callable Functions.
- `postId`, `mediaId`, Storage path và nội dung draft không đổi trong suốt quá trình retry.
- Home chỉ đọc post ACTIVE, `localSyncStatus = SYNCED`, thuộc connection và membership ACTIVE của UID hiện tại.
- Disconnect tạo ranh giới lịch sử: kết nối lại dùng connection ID mới và không mở lại dữ liệu cũ.
- Cleanup chỉ xử lý path hợp lệ sau grace period và không xóa media của post ACTIVE.

## Tài liệu liên quan

- [Kiến trúc](architecture.md)
- [Schema](data-schema.md)
- [Lifecycle post ảnh](photo-post-lifecycle.md)
- [Môi trường Firebase](firebase-environment.md)
- [Hướng dẫn review code](code-review-guide.md)

