# Tiến độ MVP

Đây là nơi theo dõi chính sau mỗi đợt làm việc. Mỗi đợt cập nhật: đã làm → bằng chứng kiểm tra → giới hạn/còn thiếu → việc tiếp theo. Không đánh dấu hoàn tất chỉ vì có model hoặc màn hình.

## Lộ trình

| Bước | Nội dung | Trạng thái |
|---|---|---|
| 1 | Môi trường Firebase, dữ liệu cũ, Rules/index và quy tắc MVP | Hoàn tất — 13/09/2026 |
| 2 | Auth/profile tối thiểu và quên mật khẩu | Hoàn tất — 13/09/2026 |
| 3 | Direct invite và transaction chống trùng | Hoàn tất — 13/09/2026 |
| 4 | Đăng một ảnh thật: Room → Storage → Firestore | Tiếp theo |
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

## 13/09/2026 — bước 2

### Đã làm

- Profile screen tải profile hiện tại từ Firestore, cập nhật cache Room và hiển thị email read-only từ Firebase Auth.
- Cho sửa displayName, username và bio. Sau khi transaction Firestore hoàn tất, app đọc lại document và chỉ khi đó cập nhật Room/UI.
- Validation dùng chung ở presentation và repository: displayName 1–100 ký tự; username 2–30 ký tự, bắt đầu bằng chữ/số và chỉ dùng chữ Latin, số, `.` hoặc `_`; bio tối đa 500 ký tự.
- Rules thật kiểm tra cùng giới hạn và bắt buộc `usernameNormalized == username.lower()`, tránh client ghi cặp username/search key không nhất quán.
- Username mặc định sau đăng ký được lọc từ phần trước `@`; nếu không đủ điều kiện, dùng fallback ổn định từ UID.
- Thay màn hình Forgot Password placeholder bằng form gọi Firebase Auth gửi email reset, có validation, loading, lỗi và thông báo chung sau khi gửi.
- Cập nhật API AuthRepository cho đọc/sửa profile, reset password và email tài khoản; thay các import Hilt Compose đã deprecated.

### Bằng chứng kiểm tra

- Android debug build thành công; Hilt/Compose và toàn bộ module compile.
- 2 unit tests ProfileValidator đạt; unit test username nền trước vẫn đạt.
- Firestore Emulator: 17 kiểm tra Rules đạt, gồm trường hợp từ chối usernameNormalized giả.
- Auth + Firestore Emulator: 13 kiểm tra đạt với hai tài khoản, đăng ký/đăng nhập lại, giữ profile, gửi password reset, chặn sửa chéo và chặn truy cập không đăng nhập.
- Firestore Rules mới đã deploy lên `memento-fre` và nội dung remote khớp file repo. Đọc lại xác nhận 3 profile vẫn đúng schema; 3 tài khoản Auth vẫn hoạt động; hai index vẫn READY.

### Chốt bước 2

Auth/profile tối thiểu đã có implementation thật. Người dùng có thể đăng ký, đăng nhập, xem/sửa profile, đăng xuất và yêu cầu email reset password. Username vẫn được phép trùng theo schema; invite là cơ chế kết nối chính.

Chưa gửi email reset tới tài khoản thật trong quá trình tự động để tránh làm phiền; đường đi đã được kiểm tra trên Auth Emulator. Chưa kiểm thử UI thủ công trên thiết bị, giao diện chưa có avatar và Splash vẫn cần mạng. Các phần này không chặn bước direct invite; avatar và offline recovery nằm ở bước 4/7.

### Tiếp theo — bước 3

Triển khai direct invite end-to-end: Callable Cloud Function tạo/redeem/revoke code, transaction tạo connection/member và lock chống direct trùng, giới hạn thử sai, Rules/App Check phù hợp, UI tạo/nhập/chia sẻ code và test concurrent redeem. Không mở quyền client ghi trực tiếp connection/member/invite.

## 13/09/2026 — bước 3

### Đã làm

- Thêm ba Callable Cloud Functions Gen 2 Node.js 22 tại `asia-southeast1`: tạo, redeem và revoke direct invite; cả ba yêu cầu Firebase Auth và App Check.
- Mã được sinh ngẫu nhiên mật mã từ alphabet không gây nhầm, hiển thị `XXXX-XXXX`, hết hạn sau 10 phút, dùng một lần. Firestore chỉ lưu SHA-256; raw code chỉ nằm trong bộ nhớ UI để hiển thị/chia sẻ.
- Tạo mã mới revoke mã active trước đó trong transaction. Revoke dùng con trỏ backend theo UID, không cần client biết codeHash.
- Redeem transaction kiểm tra invite, thời gian server, lượt dùng, hai profile, self-redeem và direct lock. Thành công tạo connection Auto ID, hai member ACTIVE, lock unique và chuyển invite sang USED cùng một commit.
- Thêm giới hạn 5 lần thử/10 phút/UID. Lỗi invalid/self/duplicate không tăng `usedCount`; cặp đã ACTIVE không thể tạo direct thứ hai kể cả khi chạy đồng thời.
- Android Connection screen có tạo mã, tạo lại, share sheet, revoke, nhập/redeem code và danh sách người đã kết nối. Redeem remote đã thành công không bị báo sai là thất bại nếu refresh cache Room ngay sau đó gặp lỗi. Danh sách connection dùng Firestore snapshot listener nên creator và redeemer đều tự nhận thay đổi.
- App Check: debug build dùng Debug provider, release dùng Play Integrity; đã đăng ký SHA-256 debug certificate và xác nhận Play Integrity config TTL 1 giờ.
- Firestore Rules cấm client đọc/ghi `invites`, `directInviteOwners`, `directConnectionLocks`, `inviteRedeemRateLimits`; connection/member vẫn chỉ đọc theo membership và không cho client tự ghi.
- Đã deploy ba Functions và Rules lên `memento-fre`; đặt Artifact Registry cleanup 7 ngày.

### Bằng chứng kiểm tra

- Functions TypeScript build; 3 unit tests cho normalize/format/random/hash/directKey đạt; `npm audit --omit=dev` không còn vulnerability được báo.
- Firestore Emulator integration đạt các trường hợp create, replace, revoke, self-redeem, expiry, 5 lần/10 phút, duplicate direct không tiêu invite và hai user redeem đồng thời chỉ một người thành công.
- 22 kiểm tra Firestore Rules đạt; 13 kiểm tra Auth/Profile vẫn đạt sau Rules mới.
- Android debug và release build thành công, xác nhận App Check provider đúng theo build variant.
- Firebase thật liệt kê đủ ba Callable v2 ở `asia-southeast1`; request không xác thực bị HTTP 401. Rules remote đã release cùng đợt deploy.

### Chốt bước 3

Direct invite đã có implementation từ UI tới backend thật; client không thể tự sửa invite, membership hoặc khóa chống trùng. Backend chỉ tạo connection khi cả hai Auth UID có profile. GROUP invite vẫn chưa bật.

Đã chạy hành trình thật và phát hiện danh sách creator từng chỉ tải khi Auth đổi; lỗi này đã được sửa bằng Firestore snapshot listener. Khi chạy debug lần đầu trên thiết bị/emulator mới vẫn phải đăng ký App Check debug token lấy từ log; token này không được commit. Raw code không khôi phục sau khi app bị đóng; tạo code mới là hành vi dự kiến cho MVP. Rate limit hiện theo UID, chưa bao phủ IP/device. Abuse nâng cao được kiểm tra tiếp ở bước 8 hoặc trước khi mở rộng người dùng.

### Bổ sung sau kiểm thử hai tài khoản — 14/09/2026

- U2 redeem thành công từng thấy U1 ngay, nhưng U1 không thấy U2 do `ConnectionViewModel` chỉ tải connection lúc Auth state thay đổi.
- Đã thêm listener realtime cho đúng query membership (`memberIds`, `ACTIVE`, `lastPostAt`). Mỗi snapshot mới kích hoạt lại đồng bộ connection/member vào Room và tải profile của thành viên còn lại.
- Listener được hủy khi đổi tài khoản hoặc ViewModel bị giải phóng, tránh dữ liệu U1 chảy sang state U2.
- Đã build, cài đè bản debug lên hai emulator và giữ nguyên session. Kiểm tra trực tiếp xác nhận màn hình U1 hiển thị `u2 (@u2)` và màn hình U2 hiển thị `u1 (@u1)`.

### Tiếp theo — bước 4

Triển khai đăng đúng một ảnh vào một connection ACTIVE: tạo cố định postId/mediaId, ghi Room PENDING, resize cạnh dài tối đa 1.920 px và nén JPEG ≤ 5 MiB, mở Storage Rules đúng path/member, upload file trước, batch ghi Post + `lastPostAt`, rồi đổi local sang SYNCED. Bổ sung retry dùng lại ID và kiểm thử lỗi giữa Storage/Firestore; chưa làm listener/pagination người nhận cho đến bước 5.
