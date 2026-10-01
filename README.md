# Memento

Ứng dụng Android chia sẻ khoảnh khắc riêng tư trong connection DIRECT, xây dựng bằng Kotlin, Jetpack Compose, Hilt, Room và Firebase.

## Trạng thái hiện tại

- Email/password Auth; profile theo Firebase Auth UID; xem/sửa display name, username và bio; gửi email reset password.
- Mỗi tài khoản có một mã kết nối cố định 8 ký tự. Callable Functions tạo connection DIRECT và hai membership bằng transaction, đồng thời dùng khóa theo cặp UID để không tạo connection trùng.
- Connection và post được đồng bộ từ Firestore vào Room. Home đọc Room, hỗ trợ All/từng connection, realtime feed và phân trang 20 bài cho mỗi connection.
- Mỗi post hỗ trợ 1–5 ảnh JPEG với layout SINGLE, GRID, COLLAGE hoặc CAROUSEL. Ảnh được sửa EXIF, resize, nén, upload tuần tự và finalize qua backend.
- Draft PENDING/FAILED được giữ để retry cùng ID sau khi app bị đóng. Ảnh phía nhận được tải vào cache riêng của app để xem lại offline.
- Tác giả có thể soft-delete post; một trong hai thành viên có thể disconnect. Scheduled cleanup dọn orphan và media của post đã xóa sau grace period.
- Chưa triển khai GROUP, VIDEO, avatar upload, bộ lọc Home nâng cao hoặc retry nền bằng WorkManager.

## Chạy và kiểm tra

Mở bằng Android Studio với Android SDK 37. Gradle daemon dùng JDK 25 theo `gradle/gradle-daemon-jvm.properties`; Java source compatibility là 11. Cần cấu hình Firebase của dự án và bật Email/Password.

```sh
./gradlew :app:assembleDebug
python3 tools/check_schema.py
./gradlew testDebugUnitTest
cd functions && npm test && npm run test:emulator && cd ..
cd functions && npm run test:storage-rules && cd ..
firebase emulators:exec --only auth,firestore --project demo-memento-schema \
  "python3 tools/check_firestore_rules.py && python3 tools/check_auth_profile.py"
```

Các thay đổi trong repository không tự deploy. Xem [môi trường Firebase](docs/firebase-environment.md) trước khi chạy lệnh tác động đến project thật.

## Tài liệu

- [Trạng thái và phạm vi hiện tại](docs/mvp-progress.md)
- [Kiến trúc và luồng dữ liệu](docs/architecture.md)
- [Schema Firestore/Room](docs/data-schema.md)
- [Lifecycle post ảnh](docs/photo-post-lifecycle.md)
- [Hướng dẫn đọc và review code](docs/code-review-guide.md)
- [Firebase Auth và profile](docs/register-firebase-auth.md)
- [Môi trường Firebase](docs/firebase-environment.md)
- [Moment Recap — đề xuất](docs/differentiating-feature.md)
