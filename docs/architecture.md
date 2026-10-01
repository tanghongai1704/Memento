# Kiến trúc và luồng dữ liệu

`app` quản lý navigation và binding Hilt. Mỗi feature tách `presentation`, `domain`, `data`; `core/domain` chứa model dùng chung, `core/database` chứa Room v3, `core/network` cung cấp Firebase/network status, `core/media` xử lý media dùng chung, `core/ui` và `core/designsystem` phục vụ Compose.

## Auth và profile

`AuthRepository` gọi Firebase Auth, đồng bộ `users/{uid}` bằng transaction rồi cache profile vào Room. Login chỉ hoàn tất khi profile sẵn sàng, trừ lỗi mạng khi đúng UID đã có cache hợp lệ. Profile cho phép sửa display name, username và bio; forgot password dùng Firebase Auth.

Sau khi profile sẵn sàng, repository gọi `getMyInviteCode`. Backend lưu mã cố định trong collection private và chỉ trả raw code cho chính chủ.

## Connection

`ConnectionRepository` query các connection ACTIVE có UID trong `memberIds`, tải member/profile rồi cập nhật `connections`, `connection_members` và `users` trong Room transaction. Snapshot listener giúp hai phía nhận thay đổi mà không cần đăng nhập lại.

`redeemDirectInvite` chạy trong Callable Function tại `asia-southeast1`. Backend kiểm tra profile, chặn self-connect, dùng `directConnectionLocks` để chống trùng và tạo connection cùng hai member trong một transaction. Nhập lại mã của một cặp đang kết nối trả connection cũ.

`disconnectDirect` đóng connection, xóa `memberIds`, chuyển hai membership sang LEFT và giải phóng lock. Android cập nhật Room và xóa cache media của connection.

## Post ảnh

Create Post chọn một connection ACTIVE và 1–5 ảnh. `PhotoProcessor` sửa EXIF, giới hạn cạnh dài 1.920 px và nén mỗi file JPEG không quá 5 MiB. Repository tạo `postId`/`mediaId` một lần, lưu Room PENDING rồi upload tuần tự lên Storage.

Callable `finalizePhotoPost` xác minh metadata object thật và dùng Firestore transaction để tạo Post, cập nhật `connection.lastPostAt`. Retry giữ nguyên ID, path và nội dung nên idempotent; lỗi được giữ ở trạng thái FAILED để khôi phục sau khi mở lại app.

## Feed và offline

`HomeRepository` theo dõi connection ACTIVE. Với mỗi connection, nó listen trang 20 post mới nhất và một query tombstone DELETED, upsert metadata vào Room, tải ảnh về cache riêng rồi phát feed từ Room. Pagination có cursor riêng cho từng connection; Home lọc All hoặc theo connection.

Room là nguồn hiển thị cho Home. Splash có thể dùng profile cache khi Firebase lỗi mạng và session vẫn thuộc đúng UID. Cache media có giới hạn 200 MiB, nhưng chưa có TTL; quyền remote bị thu hồi ngay khi disconnect còn bản sao người dùng đã lưu ngoài app không thể bị thu hồi. DAO vẫn có thể hỗ trợ các bộ lọc nâng cao trực tiếp trong Home khi sản phẩm cần mở rộng.

## Xóa và cleanup

Tác giả gọi `softDeletePost`; backend đánh dấu DELETED thay vì xóa document. Listener tombstone gỡ bài và cache trên các thiết bị. Scheduled Function `cleanupExpiredMedia` dọn đúng Storage path của app sau grace period; metadata Firestore vẫn được giữ.

## Giới hạn

GROUP và VIDEO có enum/schema dự phòng nhưng chưa có flow sản phẩm. Chưa có avatar upload, bộ lọc Home nâng cao hoặc retry upload nền bằng WorkManager. Room chỉ có migration đã kiểm chứng từ v2 lên v3; không dùng destructive fallback cho database cũ không được hỗ trợ.

Chi tiết trường dữ liệu ở [data-schema](data-schema.md), lifecycle ảnh ở [photo-post-lifecycle](photo-post-lifecycle.md), và bản đồ code ở [code-review-guide](code-review-guide.md).
