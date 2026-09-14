# Phạm vi và quy tắc MVP

Chốt làm việc ngày 13/09/2026 cho lộ trình trong [mvp-progress](mvp-progress.md). Các giá trị dưới đây là quyết định thiết kế để triển khai dần, **không có nghĩa code đã enforce tất cả**. Thay đổi quyết định phải cập nhật tài liệu này và ghi lý do vào nhật ký tiến độ.

## Mốc sử dụng đầu tiên

A đăng ký → tạo direct invite → B redeem → A đăng một ảnh → B xem được → mở lại app vẫn thấy lịch sử đã tải.

MVP ưu tiên DIRECT, PHOTO, SINGLE. Có nhiều connection direct, mỗi cặp UID chỉ có tối đa một direct ACTIVE. GROUP, VIDEO, nhiều ảnh/layout và Recap triển khai sau khi luồng đầu tiên ổn. Username không unique; ghép đôi bằng invite, không dựa vào search để xác định một người duy nhất.

## Profile (bước 2)

- displayName: 1–100 ký tự sau trim; không chấp nhận tên chỉ có whitespace.
- username: 2–30 ký tự, bắt đầu bằng chữ/số; chỉ gồm chữ Latin, số, dấu chấm hoặc gạch dưới.
- usernameNormalized bằng username lowercase Locale.ROOT; dữ liệu sai chuẩn bị Rules từ chối.
- bio: nullable, tối đa 500 ký tự.
- Email/password ở Firebase Auth. Profile công khai không có email, uid field hoặc friendList.
- Profile tối thiểu đã hoàn thành ở bước 2: hiển thị dữ liệu thật, sửa displayName/username/bio và reset password. Avatar upload làm cùng hạ tầng Storage sau, không chặn MVP ảnh.
- Không công bố khởi động offline đã hoàn thiện: Splash hiện cần mạng. Cải thiện session/cache thuộc bước 7.

## Direct invite (bước 3)

- Code ngẫu nhiên mật mã, 8 ký tự từ alphabet `ABCDEFGHJKLMNPQRSTUVWXYZ23456789`; hiển thị dạng `XXXX-XXXX`.
- Normalize: trim, bỏ dấu '-', uppercase Locale.ROOT; kiểm tra độ dài/alphabet. Firestore dùng SHA-256 của code làm document ID, không lưu raw code.
- Hiệu lực 10 phút, maxUses = 1. Mỗi người tạo có tối đa một direct invite ACTIVE chưa hết hạn; tạo lại phải revoke invite trước trong cùng thao tác backend.
- Không tự redeem; không tạo direct thứ hai khi cặp đã ACTIVE. Đã kết nối hoặc redeem lỗi không tiêu lượt dùng.
- Mutation dùng Callable Cloud Function và transaction; app không tự ghi memberIds/role/usedCount. App Check và giới hạn thử sai phải được triển khai cùng redeem, trước khi mở cho người dùng ngoài nhóm test.
- Mốc giới hạn thử sai ban đầu: 5 lần/10 phút/UID; trả lỗi chung cho mã không hợp lệ. Đây chưa phải chống lạm dụng đầy đủ theo IP/thiết bị.
- Khóa unique direct phía backend: định danh hash từ JSON array hai UID đã sort, dùng lock document + connection Auto ID trong cùng transaction. Lock là dữ liệu backend, không mở quyền đọc/ghi cho client.
- Code hết hạn không làm connection hoặc bài viết hết hạn. Backend dùng thời gian server; không tin đồng hồ điện thoại.
- Implementation bước 3 dùng ba collection nội bộ `directInviteOwners`, `directConnectionLocks` và `inviteRedeemRateLimits`; client bị cấm đọc/ghi. Creator và redeemer đều phải có `users/{uid}` trước khi connection được tạo.
- Callable chạy Node.js 22 tại `asia-southeast1` và enforce App Check. Debug build dùng Debug provider; release dùng Play Integrity.

## Ảnh và bài viết (bước 4–5)

| Thuộc tính | Quyết định |
|---|---|
| postType / layoutType | PHOTO / SINGLE |
| Media mỗi post | Chính xác một ảnh |
| Caption | Nullable, tối đa 1.000 ký tự |
| File sau xử lý | JPEG, chất lượng khởi điểm 82 |
| Kích thước sau resize | Cạnh dài tối đa 1.920 px, giữ tỉ lệ, không upscale |
| Dung lượng upload | Tối đa 5 MiB = 5.242.880 bytes; không đạt thì báo lỗi hoặc nén lại |
| ID | postId/mediaId sinh một lần trước upload, giữ nguyên qua retry |
| Storage path | `connections/{connectionId}/posts/{postId}/{mediaId}.jpg` |
| Phân trang | 20 post/trang/connection; tiếp tục tải lịch sử cũ |

Các ràng buộc cần có ở app và phía Rules/backend phù hợp, không chỉ UI. Metadata sizeBytes/width/height phải đo từ file thật. Pipeline ảnh hiện resize trong khung 1920×1440; bước 4 cần điều chỉnh để thống nhất cạnh dài 1920 và giới hạn byte.

Video hiện chỉ tạo thumbnail và còn dùng số dung lượng ước lượng trong pipeline local; **chưa có nén video thật**. Không đưa VIDEO vào bản MVP đầu, không dùng con số ước lượng đó làm metadata remote.

## Xóa, disconnect và quyền lịch sử (bước 6)

- Tác giả được soft-delete bài của mình; người nhận direct không được xóa bài của người kia. Metadata đánh dấu DELETED trước khi cleanup file.
- Một trong hai thành viên direct có thể disconnect. Backend đóng connection (`CLOSED`), làm rỗng memberIds và chuyển cả hai membership sang LEFT, ghi leftAt; không hard-delete lịch sử.
- Sau đồng bộ, connection đóng và bài của nó không hiện trong Home/History; Firestore/Storage từ chối truy cập mới. Thu hồi quyền không thể lấy lại file đã được người nhận tải hoặc sao chép ngoài app.
- Ghép lại tạo connection mới; không mở lại lịch sử connection cũ. Có thể giải phóng khóa direct cũ trong transaction khi đóng.
- Bài ACTIVE trong connection ACTIVE không có thời hạn tự xóa. Cleanup orphan/soft-deleted media sẽ có chính sách cụ thể ở bước 7.
- GROUP chưa trong MVP; quyền xem trước joinedAt, chuyển owner và rejoin phải được chốt riêng trước khi bật group. Tuyệt đối không biến DIRECT thành GROUP.

## Điều kiện hoàn tất MVP

Hai thiết bị chạy được toàn bộ vòng chia sẻ. Test cả offline/retry/tắt app giữa upload, pagination, xóa bài, disconnect và đổi tài khoản. Không nhân đôi bài, không báo thành công khi ghi thất bại và không lẫn cache giữa tài khoản. Rules/Storage Rules phải được kiểm tra và deploy đúng project.
