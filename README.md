# Memento

Ứng dụng Android chia sẻ ảnh/video trong connection DIRECT hoặc GROUP, dùng Kotlin, Compose, Hilt, Firebase và Room.

## Trạng thái hiện tại

- Auth email/password thật; tạo/khôi phục profile tại `users/{Firebase Auth UID}` và cache Room.
- Connection đọc Firestore theo `memberIds`, cache connection/members trong một Room transaction. Search username chính xác, tối đa 20 kết quả.
- Home/History đọc cache Room. Home chọn All hoặc connection; không còn dữ liệu mẫu.
- Model/Room đã chuẩn bị cho invite, post và metadata media. UI picker/nén media vẫn hoạt động.
- Chưa triển khai tạo/redeem/revoke invite, đồng bộ/pagination post, upload Storage, retry/cleanup hoặc chỉnh sửa profile. Nút đăng bài báo chưa khả dụng; không báo upload thành công giả.
- Luồng gửi/duyệt connection request cũ đã gỡ. Chưa có cách tạo connection mới trên UI cho đến khi triển khai invite.

## Chạy và kiểm tra

Mở bằng Android Studio với Android SDK 37. Gradle daemon dùng JDK 25 theo `gradle/gradle-daemon-jvm.properties`; Java source compatibility là 11.
Cần cấu hình Firebase riêng của dự án, bật Email/Password và Firestore.

```sh
./gradlew :app:assembleDebug
python3 tools/check_schema.py
./gradlew :core:domain:test
firebase emulators:exec --only firestore --project demo-memento-schema "python3 tools/check_firestore_rules.py"
```

Rules/index trong repo là cấu hình chuẩn bị, chưa được deploy tự động. Đọc phần chuyển đổi dữ liệu trước khi dùng với Firebase đã có dữ liệu cũ.

## Tài liệu

- [Schema và kế hoạch invite](docs/data-schema.md)
- [Kiến trúc](docs/architecture.md)
- [Auth và profile](docs/register-firebase-auth.md)
- [Luồng sản phẩm](docs/mvp-user-flow.md)
- [Moment Recap — đề xuất](docs/differentiating-feature.md)
