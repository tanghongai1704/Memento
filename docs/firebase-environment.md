# Firebase: môi trường và vận hành

## Đích kết nối

| Mục | Giá trị |
|---|---|
| Project Firebase thật | `memento-fre` |
| Android package | `com.tangai.memento` |
| Firestore database/location | `(default)` / `asia-southeast1` |
| Storage bucket | `memento-fre.firebasestorage.app` |
| Alias CLI | `live` → memento-fre; `emulator` → demo-memento-schema |
| Functions region/runtime | `asia-southeast1` / Node.js 22 |
| Emulator | Auth 127.0.0.1:9099; Firestore 127.0.0.1:8080; Functions 127.0.0.1:5001 |

App hiện vẫn kết nối Firebase thật theo `app/google-services.json`. Cấu hình emulator trong firebase.json chỉ dành cho test script; chưa tự chuyển Android app vào Emulator.

Không đặt default project cho CLI: lệnh thay đổi remote phải ghi `--project memento-fre` rõ ràng. Không dùng project demo để deploy. Không đổi billing hoặc tạo Firebase project mới trong bước 1.

## Trạng thái sau bước 3 — 13/09/2026

Cả 3 profile đã migrate và kiểm tra lại; tài khoản Auth giữ nguyên. Firestore Rules đã deploy và khớp file repo. Hai composite indexes connections/posts đều READY. Probe users không đăng nhập trả 403. Storage Rules remote giữ nguyên deny-all; file local có cùng chính sách. Backup trước và báo cáo sau nằm trong `.local/firebase-audit/` và `.local/firebase-audit/after-step1/`.

Bước 2 đã deploy Rules validation username chặt hơn và xác nhận remote khớp repo. Snapshot kiểm tra sau bước 2 nằm trong `.local/firebase-audit/after-step2/`. Profile edit và password reset được kiểm tra bằng Emulator; không gửi reset mail tới tài khoản thật.

Bước 3 đã deploy `createDirectInvite`, `redeemDirectInvite`, `revokeDirectInvite` Gen 2 và Rules khóa invite/collection nội bộ. Android App Check đã tích hợp Debug provider cho debug và Play Integrity cho release; SHA-256 debug certificate đã đăng ký trên Firebase. Lần đầu chạy debug app trên thiết bị/emulator, lấy debug token trong log và đăng ký tại Firebase Console → App Check → Manage debug tokens; không commit token. Play Integrity config có TTL 1 giờ; signing certificate phát hành thật cần bổ sung khi có release key/Play App Signing.

Artifact Registry ở `asia-southeast1` tự xóa function images cũ hơn 7 ngày để tránh tích lũy storage.

## File nên commit

`.firebaserc`, `firebase.json`, `firestore.rules`, `firestore.indexes.json`, `storage.rules`, `functions/package.json`, `functions/package-lock.json`, source/test Functions, các script test và docs. Không commit `functions/node_modules`, `functions/lib`, `.local/`, `.firebase/`, log, debug token, credential hoặc backup dữ liệu user.

## Kiểm tra local

Cần Firebase CLI đã cài và Java phù hợp để chạy Emulator. Script Auth/Profile dùng tài khoản mới chỉ trong emulator, không cần mật khẩu tài khoản thật.

```sh
firebase emulators:exec --only auth,firestore --project demo-memento-schema \
  'python3 tools/check_firestore_rules.py && python3 tools/check_auth_profile.py'
cd functions
npm test
npm run test:emulator
```

Test này xác minh Firebase Auth Emulator + payload profile + Rules; không thay cho test Android trên thiết bị thật.

## Baseline trước thay đổi ngày 13/09/2026

Auth có 3 tài khoản, Email/Password enabled. Firestore chỉ có `users` với 3 profile cũ (uid/username/email/createdAt, một profile có friendList); không thấy connection, request hoặc subcollection. Chưa có composite index. Firestore test-mode mở cho mọi người đến 14/09/2026; Storage deny-all.

Snapshot Rules và backup profile lưu ở `.local/firebase-audit/`, chỉ dùng khôi phục local khi cần; không phải managed export đầy đủ Firebase. Không export password hash/Auth credential. Migration profile giữ document UID/username/createdAt, bỏ field công khai ngoài schema, thêm field còn thiếu và updatedAt server timestamp. Batch dùng updateTime precondition để không ghi đè thay đổi đồng thời.

## Phát hành cấu hình

Chỉ deploy sau khi dữ liệu profile đúng schema và test đạt. Không deploy tất cả dịch vụ vô tình.

```sh
firebase deploy --only firestore:rules,firestore:indexes --project memento-fre
firebase deploy --only functions --project memento-fre
```

Storage giữ deny-all ở bước này; mở avatar/media chỉ khi có implementation và test phù hợp. Rules khóa client mutation connection/invite/post; direct invite dùng Admin backend. Bước 4–6 cập nhật dần quyền phù hợp; không bật allow-all để thử app.

Rollback Rules cần dùng snapshot đã kiểm tra; không tự quay lại test-mode mở cho mọi người. Backup profiles chỉ restore có đối chiếu updateTime, tránh ghi đè profile user đã sửa sau migration. Trạng thái phát hành thực tế được ghi ở [mvp-progress](mvp-progress.md).
