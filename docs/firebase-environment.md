# Firebase: môi trường và vận hành

## Đích kết nối

| Mục | Giá trị |
|---|---|
| Project thật | `memento-fre` |
| Android package | `com.tangai.memento` |
| Firestore database/location | `(default)` / `asia-southeast1` |
| Storage bucket | `memento-fre.firebasestorage.app` |
| Alias CLI | `live` → `memento-fre`; `emulator` → `demo-memento-schema` |
| Functions region/runtime | `asia-southeast1` / Node.js 22 |
| Emulator | Auth 9099; Firestore 8080; Functions 5001; Storage 9199 |

Không có default project trong `.firebaserc`. Mọi lệnh thay đổi remote phải ghi rõ `--project memento-fre`; không dùng project demo để deploy. Trạng thái trong repository không chứng minh trạng thái đang chạy trên Firebase, vì commit không tự deploy.

## Runtime và App Check

Android dùng `app/google-services.json`. Debug build kết nối emulator khi private app storage có file marker `use_firebase_emulators`; trên Android Emulator, các SDK trỏ tới `10.0.2.2`. File `firebase_emulator_account` có thể chứa email và password test trên hai dòng để debug build tự đăng nhập. Không commit marker, credential hoặc App Check debug token.

Production Callable Functions enforce Firebase Auth và App Check. Debug dùng Debug provider; release dùng Play Integrity. Release signing certificate/Play App Signing phải được đăng ký trước khi phát hành.

Các Functions được export từ source hiện tại:

- `getMyInviteCode`
- `redeemDirectInvite`
- `finalizePhotoPost`
- `softDeletePost`
- `disconnectDirect`
- `cleanupExpiredMedia` (scheduled)

Storage Rules gọi Firestore để kiểm tra membership; service agent của Storage cần quyền `roles/firebaserules.firestoreServiceAgent`, nếu thiếu upload/read sẽ bị từ chối dù Auth hợp lệ.

## File và dữ liệu nhạy cảm

Commit cấu hình, Rules, indexes, source/test Functions và scripts kiểm tra. Không commit `functions/node_modules`, `functions/lib`, `.local/`, `.firebase/`, log, debug token, credential hoặc backup dữ liệu người dùng.

Các file trong `.local/firebase-audit/` chỉ là audit/backup local, không phải managed export đầy đủ của Firebase và không chứa password hash/Auth credential. Khi restore profile, phải đối chiếu `updateTime` để không ghi đè thay đổi mới hơn.

## Kiểm tra local

Cloud Functions yêu cầu Node.js 22. Nếu dùng `nvm`, chạy `nvm use` trong thư mục `functions` để đọc phiên bản từ `.nvmrc` trước khi cài dependency hoặc chạy emulator.

```sh
firebase emulators:exec --only auth,firestore --project demo-memento-schema \
  'python3 tools/check_firestore_rules.py && python3 tools/check_auth_profile.py'
cd functions
npm test
npm run test:emulator
npm run test:storage-rules
```

Các test này xác minh Auth/Profile, Functions và Firestore/Storage Rules trong emulator. Ở root project, chạy thêm `./gradlew test testDebugUnitTest lintDebug :app:assembleDebug` và `python3 tools/check_schema.py`.

App loại toàn bộ database, shared preferences và file riêng tư khỏi cloud backup lẫn device transfer. Firebase session và cache phải được tạo lại trên thiết bị mới thay vì sao chép ngầm.

## Deploy có chủ đích

Chỉ deploy sau khi test đạt và đã kiểm tra project đích. Tách từng dịch vụ để tránh phát hành ngoài ý muốn:

```sh
firebase deploy --only firestore:rules,firestore:indexes --project memento-fre
firebase deploy --only functions --project memento-fre
firebase deploy --only storage --project memento-fre
```

Sau deploy, đối chiếu danh sách Functions, Rules và indexes từ Firebase CLI/Console. Không rollback về Rules allow-all; rollback phải dùng bản Rules đã được kiểm tra.
