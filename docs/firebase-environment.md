# Firebase: môi trường và vận hành

## Đích kết nối

| Mục | Giá trị |
|---|---|
| Project Firebase thật | `memento-fre` |
| Android package | `com.tangai.memento` |
| Firestore database/location | `(default)` / `asia-southeast1` |
| Storage bucket | `memento-fre.firebasestorage.app` |
| Alias CLI | `live` → memento-fre; `emulator` → demo-memento-schema |
| Emulator | Auth 127.0.0.1:9099; Firestore 127.0.0.1:8080 |

App hiện vẫn kết nối Firebase thật theo `app/google-services.json`. Cấu hình emulator trong firebase.json chỉ dành cho test script; chưa tự chuyển Android app vào Emulator.

Không đặt default project cho CLI: lệnh thay đổi remote phải ghi `--project memento-fre` rõ ràng. Không dùng project demo để deploy. Không đổi billing hoặc tạo Firebase project mới trong bước 1.

## Trạng thái sau bước 1 — 13/09/2026

Cả 3 profile đã migrate và kiểm tra lại; tài khoản Auth giữ nguyên. Firestore Rules đã deploy và khớp file repo. Hai composite indexes connections/posts đều READY. Probe users không đăng nhập trả 403. Storage Rules remote giữ nguyên deny-all; file local có cùng chính sách. Backup trước và báo cáo sau nằm trong `.local/firebase-audit/` và `.local/firebase-audit/after-step1/`.

## File nên commit

`.firebaserc`, `firebase.json`, `firestore.rules`, `firestore.indexes.json`, `storage.rules`, các script test và docs. Không commit `.local/`, `.firebase/`, log, credential hoặc backup dữ liệu user.

## Kiểm tra local

Cần Firebase CLI đã cài và Java phù hợp để chạy Emulator. Script Auth/Profile dùng tài khoản mới chỉ trong emulator, không cần mật khẩu tài khoản thật.

```sh
firebase emulators:exec --only auth,firestore --project demo-memento-schema \
  'python3 tools/check_firestore_rules.py && python3 tools/check_auth_profile.py'
```

Test này xác minh Firebase Auth Emulator + payload profile + Rules; không thay cho test Android trên thiết bị thật.

## Baseline trước thay đổi ngày 13/09/2026

Auth có 3 tài khoản, Email/Password enabled. Firestore chỉ có `users` với 3 profile cũ (uid/username/email/createdAt, một profile có friendList); không thấy connection, request hoặc subcollection. Chưa có composite index. Firestore test-mode mở cho mọi người đến 14/09/2026; Storage deny-all.

Snapshot Rules và backup profile lưu ở `.local/firebase-audit/`, chỉ dùng khôi phục local khi cần; không phải managed export đầy đủ Firebase. Không export password hash/Auth credential. Migration profile giữ document UID/username/createdAt, bỏ field công khai ngoài schema, thêm field còn thiếu và updatedAt server timestamp. Batch dùng updateTime precondition để không ghi đè thay đổi đồng thời.

## Phát hành cấu hình

Chỉ deploy sau khi dữ liệu profile đúng schema và test đạt. Không deploy tất cả dịch vụ vô tình.

```sh
firebase deploy --only firestore:rules,firestore:indexes --project memento-fre
```

Storage giữ deny-all ở bước này; mở avatar/media chỉ khi có implementation và test phù hợp. Rules hiện khóa mutation connection/invite/post. Bước 3–6 cập nhật dần quyền tương ứng; không bật allow-all để thử app.

Rollback Rules cần dùng snapshot đã kiểm tra; không tự quay lại test-mode mở cho mọi người. Backup profiles chỉ restore có đối chiếu updateTime, tránh ghi đè profile user đã sửa sau migration. Trạng thái phát hành thực tế được ghi ở [mvp-progress](mvp-progress.md).
