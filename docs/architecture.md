# Kiến trúc và luồng dữ liệu

`app` quản lý navigation, WorkManager và binding Hilt. Mỗi feature tách `presentation`, `domain`, `data`; `core/domain` chứa model dùng chung, `core/database` chứa Room schema v3, `core/network` cung cấp Firebase/network status, còn `core/ui` và `core/designsystem` phục vụ Compose. Xử lý ảnh hiện thuộc `feature/post/data` vì chỉ flow tạo post sử dụng.

Repository và data source phải main-safe. Room suspend API và Firebase async API được gọi trực tiếp; các thao tác blocking với `File`, `ContentResolver` và xử lý bitmap chạy bằng IO dispatcher được inject từ `core/network`. Presentation chỉ nhận URI/path media đã được data layer xác nhận, không tự dựng đường dẫn hoặc kiểm tra file.

Mọi Firebase `Task` đi qua adapter coroutine dùng chung, bao phủ success, failure và canceled. Với Storage, coroutine caller bị hủy sẽ hủy transfer; Firebase tự hủy transfer được trả về như lỗi có thể retry. `CancellationException` của coroutine caller luôn được propagate, không chuyển thành lỗi nghiệp vụ.

## Nguyên tắc offline-first

Với dữ liệu đã cache, UI luôn đọc và hiển thị Room trước; không chờ Firestore, Functions hoặc Storage để mở màn hình. Đồng bộ server chạy nền, ghi kết quả vào Room, rồi presentation đọc lại Room để cập nhật UI. Dữ liệu remote trả về không được dùng làm nguồn hiển thị song song với Room.

Home, Connections, Profile và Create Post đều ưu tiên dữ liệu local đã xác nhận. Trạng thái loading toàn màn hình chỉ dành cho dữ liệu bắt buộc chưa từng có local; refresh nền không được che nội dung hoặc empty state đang hiển thị. Metadata post SYNCED được phát từ Room trước khi tải media; từng ảnh có loading/error riêng. Draft PENDING/FAILED nằm trong upload queue, không xuất hiện như một post đã publish trên Home.

Các mutation cần server xác nhận như login, đăng bài, redeem, disconnect, xóa post và lưu profile vẫn hiển thị tiến trình tại đúng control đang thao tác. Nội dung local hiện có phải được giữ trên màn hình; sau thành công repository ghi Room và UI đọc lại local. Draft post là ngoại lệ được ghi Room trước upload để hỗ trợ retry idempotent.

## Auth và profile

`AuthRepository` gọi Firebase Auth, đồng bộ `users/{uid}` bằng transaction rồi cache profile vào Room. Login chỉ hoàn tất khi profile sẵn sàng, trừ lỗi mạng khi đúng UID đã có cache hợp lệ. Profile cho phép sửa display name, username và bio; forgot password dùng Firebase Auth.

Sau khi profile sẵn sàng, repository gọi `getMyInviteCode`. Backend lưu mã cố định trong collection private và chỉ trả raw code cho chính chủ.

## Connection

`ConnectionRepository` query các connection ACTIVE có UID trong `memberIds`, tải member/profile rồi cập nhật `connections`, `connection_members` và `users` trong Room transaction. Snapshot listener giúp hai phía nhận thay đổi mà không cần đăng nhập lại.

`redeemDirectInvite` chạy trong Callable Function tại `asia-southeast1`. Backend kiểm tra profile, chặn self-connect, dùng `directConnectionLocks` để chống trùng và tạo connection cùng hai member trong một transaction. Nhập lại mã của một cặp đang kết nối trả connection cũ.

`disconnectDirect` đóng connection, xóa `memberIds`, chuyển hai membership sang LEFT và giải phóng lock. Android cập nhật Room và xóa cache media của connection.

## Post ảnh

Create Post chọn một connection ACTIVE và 1–5 ảnh. `PhotoProcessor` sửa EXIF, giới hạn cạnh dài 1.920 px và nén mỗi file JPEG không quá 5 MiB. Repository tạo `postId`/`mediaId` một lần, lưu Room PENDING rồi upload tuần tự lên Storage.

Callable `finalizePhotoPost` xác minh metadata object thật và dùng Firestore transaction để tạo Post, sao chép `memberIds` từ connection làm audience/query index, rồi cập nhật `connection.lastPostAt`. Retry giữ nguyên ID, path và nội dung nên idempotent; lỗi được giữ ở trạng thái FAILED để khôi phục sau khi mở lại app.

## Feed và offline

`HomeRepository` dùng một collection-group listener lấy đúng 20 post ACTIVE mới nhất có `memberIds` chứa UID hiện tại, cùng một listener tombstone DELETED. Metadata được upsert vào Room với trạng thái local SYNCED, ảnh tải về cache riêng rồi feed phát từ Room. Feed All có một cursor toàn cục; filter connection bổ sung `connectionId` và có cursor riêng khi người dùng tải thêm.

Room là nguồn hiển thị cho Home. Splash có thể dùng profile cache khi Firebase lỗi mạng và session vẫn thuộc đúng UID. Cache media có giới hạn 200 MiB, nhưng chưa có TTL; quyền remote bị thu hồi ngay khi disconnect còn bản sao người dùng đã lưu ngoài app không thể bị thu hồi. DAO vẫn có thể hỗ trợ các bộ lọc nâng cao trực tiếp trong Home khi sản phẩm cần mở rộng.

## Xóa và cleanup

Tác giả gọi `softDeletePost`; backend đánh dấu DELETED thay vì xóa document. Listener tombstone gỡ bài và cache trên các thiết bị. Scheduled Function `cleanupExpiredMedia` dọn đúng Storage path của app sau grace period; metadata Firestore vẫn được giữ.

## Giới hạn

GROUP và VIDEO có enum/schema dự phòng nhưng chưa có flow sản phẩm. Chưa có avatar upload hoặc bộ lọc Home nâng cao. Upload nền đã dùng WorkManager với network constraint và exponential backoff; sau khi hết retry tự động, user vẫn có thể retry hoặc discard thủ công. Room chỉ có migration đã kiểm chứng từ v2 lên v3; không dùng destructive fallback cho database cũ không được hỗ trợ.

Chi tiết trường dữ liệu ở [data-schema](data-schema.md), lifecycle ảnh ở [photo-post-lifecycle](photo-post-lifecycle.md), trạng thái phạm vi ở [project-status](project-status.md), và bản đồ code ở [code-review-guide](code-review-guide.md).
