# Tiến độ MVP

Đây là nơi theo dõi chính sau mỗi đợt làm việc. Mỗi đợt cập nhật: đã làm → bằng chứng kiểm tra → giới hạn/còn thiếu → việc tiếp theo. Không đánh dấu hoàn tất chỉ vì có model hoặc màn hình.

## Lộ trình

| Bước | Nội dung | Trạng thái |
|---|---|---|
| 1 | Môi trường Firebase, dữ liệu cũ, Rules/index và quy tắc MVP | Hoàn tất — 13/09/2026 |
| 2 | Auth/profile tối thiểu và quên mật khẩu | Hoàn tất — 13/09/2026 |
| 3 | Direct invite và transaction chống trùng | Hoàn tất — 13/09/2026 |
| 4 | Đăng một ảnh thật: Room → Storage → Firestore | Hoàn tất — 14/09/2026 |
| 5 | Đồng bộ người nhận, Home, pagination | Hoàn tất — 17/09/2026 |
| 6 | History, soft delete và disconnect | Hoàn tất — 17/09/2026 |
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

## 14/09/2026 — bước 4

### Đã làm

- Create Post đã dùng luồng thật cho đúng một ảnh PHOTO/SINGLE và caption tùy chọn tối đa 1.000 ký tự. App sinh `postId` và `mediaId` một lần, ghi Room `PENDING`, sửa hướng EXIF, resize cạnh dài tối đa 1.920 px, nén JPEG bắt đầu ở quality 82 và giữ file dưới 5 MiB.
- File đã xử lý được lưu ổn định trong thư mục riêng của app và upload tới `connections/{connectionId}/posts/{postId}/{mediaId}.jpg`. Khi upload lỗi, Room chuyển `FAILED`; mở lại app khôi phục bài và nút retry dùng lại đúng ID, file và Storage path cũ.
- Thêm Callable `finalizePhotoPost` yêu cầu Auth + App Check. Backend kiểm tra path, MIME, kích thước, dung lượng và metadata `authorId` của object thật; sau đó transaction kiểm tra connection/membership ACTIVE, tạo Post và cập nhật `connection.lastPostAt/updatedAt` cùng lúc. Gọi lại cùng dữ liệu trả kết quả cũ, không tạo bài trùng hoặc đổi `createdAt`.
- Storage Rules chỉ cho member ACTIVE đọc; chỉ uploader được tạo/ghi lại đúng file JPEG, đúng metadata và tối đa 5 MiB. Client không được xóa media. Quyền liên dịch vụ `roles/firebaserules.firestoreServiceAgent` đã cấp cho Storage service agent để rule thật có thể đọc connection/member từ Firestore.
- Home của người đăng đọc bài local từ Room và hiển thị đúng file đã xử lý. Đã sửa mapping cache local theo connection/post/media thay vì ghép trực tiếp Storage path.
- Đã deploy `finalizePhotoPost` và Storage Rules lên `memento-fre`.

### Bằng chứng kiểm tra

- Android debug build thành công; APK cuối đã cài lên cả hai emulator.
- 5 unit tests Functions đạt. Integration emulator đạt create/redeem invite và post finalize: membership, object metadata, atomic Post + `lastPostAt`, từ chối media sai và retry idempotent.
- 9 kiểm tra Storage Rules đạt: member/outsider, path, MIME, dung lượng, overwrite của uploader khác và delete.
- Kiểm thử thật trên U1: lần đầu giữ bài `FAILED` khi Storage từ chối; sau khi bổ sung IAM, mở lại app khôi phục bài, retry upload đạt 100% và quay về Home. Log callable xác nhận `auth: VALID`, `app: VALID`; Room xác nhận `SYNCED` và timestamp của Post trùng `connection.lastPostAt`.

### Chốt bước 4

Phía người gửi đã có đường đăng một ảnh thật, retry qua lần mở app mới và commit metadata an toàn. Firestore không thể atomic cùng Storage, nên file upload xong nhưng finalize thất bại vẫn có thể trở thành orphan; retry hiện tái sử dụng file đó, còn cleanup định kỳ thuộc bước 7.

U2 chưa tự tải bài mới và chưa hiển thị ảnh remote vì post listener, pagination, download/cache là phạm vi bước 5. Vì vậy bước 4 xác nhận việc gửi và lưu server, chưa khẳng định hành trình chia sẻ hai chiều đã hoàn tất.

### Tiếp theo — bước 5

Mở quyền đọc Post đúng membership, đồng bộ từng connection theo trang 20 bài, nghe bài mới, lưu metadata vào Room, tải/cache ảnh Storage và hiển thị cùng một bài trên U2. Hoàn thiện trạng thái loading/lỗi và kiểm tra lại bằng hai tài khoản thật, gồm đóng/mở app và tránh ghi trùng Room.

## 15/09/2026 — đơn giản hóa mã kết nối trước bản nộp

- Thay invite tạm thời bằng một mã cố định cho mỗi user: tạo một lần sau khi profile sẵn sàng, hiển thị/share tại Profile, không expire, revoke hoặc consume.
- `getMyInviteCode` trả lại cùng code khi gọi nhiều lần; tài khoản cũ được cấp code ở lần login/Profile đầu tiên nên không cần migration bắt buộc.
- `redeemDirectInvite` dùng code lookup để tìm owner. Một code có thể được nhiều user khác nhau dùng; nhập lại bởi cùng một cặp trả connection cũ, không tạo document trùng.
- Vẫn giữ Auth, App Check, self-connect guard, kiểm tra hai profile và `directConnectionLocks` transaction. Đã bỏ UI create/revoke/countdown và các nhánh expiry/rate-limit/usedCount của flow nộp bài.
- Android compile debug, Functions type-check, integration emulator và 22 Rules checks đều đạt. `getMyInviteCode`, `redeemDirectInvite`, `finalizePhotoPost` cùng Rules mới đã deploy lên Firebase thật; hai function create/revoke cũ đã xóa.

### Dọn dẹp trước bước 5 — 14/09/2026

- Đã xóa các nhánh thư mục source trống còn lại từ migration, mapper/model connection và media pipeline cũ.
- Đã chạy Gradle clean để xóa output build có thể tái tạo, gồm các thư mục bản sao có hậu tố ` 2`.
- Sau khi dọn, toàn project không còn thư mục trống ngoài ba thư mục nội bộ chuẩn của Git (`objects/info`, `objects/pack`, `refs/tags`). Không có source hoặc cấu hình chức năng nào bị xóa trong đợt dọn dẹp này.

### Sửa tên người nhận ở Create Post — 14/09/2026

- DIRECT connection theo schema có `name = null`; Create Post trước đây fallback thành `Direct connection`, nên người dùng không biết đang chọn ai khi có nhiều connection.
- Create Post giờ ghép member ACTIVE với profile đã đồng bộ và hiển thị `displayName (@username)`. ID dùng để đăng vẫn là `connectionId`, không gửi trực tiếp theo userId.
- Preview dùng cùng nhãn người nhận để người dùng kiểm tra lại trước khi upload. Group sau này ưu tiên `connection.name`, có fallback riêng nếu thiếu tên.
- Android debug build đạt; bản mới đã cài trên hai emulator. Kiểm tra thật: U1 thấy `u2 (@u2)` và U2 thấy `u1 (@u1)` trong Share with.
- Home trước đó vẫn hiện `Direct connection` vì khi đọc `ConnectionEntity` từ Room, repository chuyển sang domain với danh sách members rỗng. Đã sửa mapping để nạp đầy đủ member của từng connection và dùng chung hàm tạo nhãn connection.
- Kiểm tra bản cuối: chip cạnh `All` và nhãn trên Post của U1 đều là `u2 (@u2)`; chip Home của U2 là `u1 (@u1)`.

## 16/09/2026 — bắt đầu bước 5: listener bài ảnh

- Home nghe danh sách connection ACTIVE của tài khoản hiện tại rồi mở một Post listener riêng cho từng connection, query `status == ACTIVE`, `createdAt DESC`, giới hạn 20 bài mới nhất.
- Snapshot được map và upsert vào Room với trạng thái `SYNCED`; media ảnh được tải từ Storage vào cache ổn định `pending_media/{connectionId}/{postId}/{mediaId}.jpg`. File tải tạm được kiểm tra đúng byte size trước khi đổi tên vào cache.
- Home cập nhật từ Room sau mỗi snapshot, tăng cache revision sau khi download để Compose nạp ảnh vừa xuất hiện, hiển thị ảnh remote bằng cùng đường cache với ảnh sender và giữ danh sách hiện tại nếu listener/tải ảnh lỗi. Listener được gỡ khi connection rời danh sách hoặc ViewModel bị hủy.
- Firestore Rules đã mở read Post cho member ACTIVE nhưng vẫn khóa toàn bộ client write. Rule emulator kiểm tra member đọc/query được, outsider bị chặn và member không thể tự ghi Post.
- Media picker tiếp tục dùng Android Photo Picker (`PickVisualMedia`): hệ thống chỉ cấp URI người dùng chọn nên không xin quyền đọc toàn bộ thư viện. Manifest yêu cầu module Photo Picker backport từ Google Play services cho thiết bị hỗ trợ.
- Android debug build, 5 unit tests Functions, 9 Storage Rules checks và Firestore Rules emulator đều đạt. Firestore Rules mới đã deploy lên `memento-fre`; APK được cài giữ dữ liệu trên hai emulator và xác nhận U2 nhận metadata, tải file cache rồi render ảnh U1 đã đăng, không còn `PERMISSION_DENIED`.
- Tiêu đề mỗi Post card lấy profile theo `post.authorId`; chip filter vẫn dùng tên người còn lại của connection. Nhờ vậy Alice thấy bài mình đăng mang tên Alice, còn Andy cũng thấy đúng Alice là tác giả.

## 17/09/2026 — hoàn thiện phân trang bước 5

- Mỗi connection ACTIVE có cursor Firestore riêng. Listener tiếp tục nghe 20 bài mới nhất; nút `Load older moments` dùng `startAfter(lastDocument)` để lấy tiếp tối đa 20 bài cho từng connection còn dữ liệu.
- Trang cũ được upsert vào Room bằng composite key hiện có nên snapshot lặp hoặc retry không tạo bài trùng. Khi bài mới đẩy ranh giới trang đầu xuống, cursor chưa phân trang được cập nhật; sau khi đã tải trang cũ, listener không ghi đè cursor đó.
- Trạng thái `hasMore` và `isLoadingMore` được đưa lên Home UI. Nút tải thêm tự ẩn khi mọi connection đã hết trang; lỗi giữ nguyên feed hiện tại để người dùng retry.
- Key Compose của Post card gồm cả `connectionId:postId`, đúng với khóa dữ liệu và tránh va chạm nếu hai connection tình cờ có cùng postId.
- Android debug build, 5 unit tests Functions và Firestore Rules emulator đều đạt sau thay đổi.

## 17/09/2026 — bước 6: soft delete và disconnect

### Đã làm

- Tác giả có nút `Delete` trên bài của mình và phải xác nhận trước khi xóa. Người nhận không thấy thao tác này. Callable `softDeletePost` kiểm tra Auth, App Check, membership ACTIVE và `authorId`, sau đó chỉ đổi metadata sang `DELETED` cùng `deletedAt/deletedBy/updatedAt`; gọi lại an toàn và không hard-delete document.
- Home nghe cả trang bài mới lẫn 20 bản ghi `DELETED` cập nhật gần nhất cho từng connection. Khi nhận trạng thái xóa, Room đổi trạng thái, feed/History tự ẩn bài và file cache do app quản lý được xóa.
- Mỗi direct connection có thao tác `Disconnect` với hộp xác nhận. Callable `disconnectDirect` đóng connection, làm rỗng `memberIds`, chuyển cả hai member sang `LEFT`, ghi `leftAt` và đóng direct lock trong cùng transaction.
- Sau disconnect, query connection không còn trả document cho hai user; Firestore/Storage Rules từ chối đọc lịch sử và media. App thu hồi membership local, ẩn Home/History và xóa cache ảnh của connection. Dùng lại mã kết nối sẽ tạo connection ID mới, không khôi phục lịch sử cũ.
- Thêm composite index `posts(status, updatedAt DESC)` cho listener xóa và đã deploy hai callable cùng index lên `memento-fre`.

### Bằng chứng kiểm tra

- 7 unit tests Functions đạt. Integration Firestore Emulator đạt author-only delete, recipient/outsider bị từ chối, delete/disconnect idempotent, finalize bị từ chối sau disconnect và reconnect tạo lịch sử mới.
- Firestore Rules Emulator đạt query ACTIVE/DELETED khi còn membership và từ chối connection/member/post sau khi đóng. Storage Rules Emulator từ chối download/upload sau khi membership chuyển LEFT.
- Toàn bộ Android `testDebugUnitTest` và debug APK build thành công. APK được cài đè giữ dữ liệu trên hai emulator: Alice thấy `Delete` ở bài của Alice, Andy không thấy; cả hai thấy `Disconnect` ở danh sách kết nối.
- Không bấm xác nhận xóa hoặc disconnect trên dữ liệu thật Alice/Andy; kiểm thử mutation đầy đủ dùng emulator để giữ nguyên dữ liệu demo.

### Chốt bước 6

Soft delete và direct disconnect đã hoàn chỉnh từ UI, local cache đến backend/rules. Storage object của bài soft-delete chưa bị xóa ngay để giữ thứ tự metadata trước cleanup; job cleanup orphan/soft-deleted media thuộc bước 7.

### Tiếp theo — bước 7

Hoàn thiện offline/retry và phục hồi tiến trình khi app bị dừng giữa upload/finalize; định nghĩa rồi triển khai cleanup an toàn cho orphan object và media của bài đã soft-delete, có thời gian chờ để không đua với retry.
