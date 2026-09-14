# Memento

Ứng dụng Android chia sẻ ảnh/video trong connection DIRECT hoặc GROUP, dùng Kotlin, Compose, Hilt, Firebase và Room.

Theo dõi từng đợt tại [Tiến độ MVP](docs/mvp-progress.md); phạm vi/giới hạn tại [MVP baseline](docs/mvp-baseline.md).

## Trạng thái hiện tại

- Auth email/password thật; tạo/khôi phục profile tại `users/{Firebase Auth UID}`, xem/sửa displayName/username/bio, cache Room và gửi password reset.
- Direct invite đã hoạt động qua Callable Functions: tạo/chia sẻ/revoke mã 8 ký tự, redeem tạo connection + hai member bằng transaction và khóa chống cặp trùng.
- Connection đọc Firestore theo `memberIds`, cache connection/members trong một Room transaction. Repository vẫn hỗ trợ search username chính xác nhưng UI MVP dùng invite làm luồng kết nối.
- Home/History đọc cache Room. Home chọn All hoặc connection; không còn dữ liệu mẫu.
- Model/Room đã chuẩn bị cho post và metadata media. UI picker/nén media vẫn hoạt động.
- Chưa triển khai đồng bộ/pagination post, upload Storage hoặc retry/cleanup. Nút đăng bài báo chưa khả dụng; không báo upload thành công giả.
- Luồng gửi/duyệt connection request cũ đã gỡ. Connection mới chỉ được tạo qua backend invite.

## Chạy và kiểm tra

Mở bằng Android Studio với Android SDK 37. Gradle daemon dùng JDK 25 theo `gradle/gradle-daemon-jvm.properties`; Java source compatibility là 11.
Cần cấu hình Firebase riêng của dự án, bật Email/Password và Firestore.

```sh
./gradlew :app:assembleDebug
python3 tools/check_schema.py
./gradlew :core:domain:test
cd functions && npm test && npm run test:emulator && cd ..
firebase emulators:exec --only auth,firestore --project demo-memento-schema \
  "python3 tools/check_firestore_rules.py && python3 tools/check_auth_profile.py"
```

Ngày 13/09/2026 đã hoàn tất bước 1–3: migrate profile, hoàn thiện Auth/profile và deploy direct invite backend cùng Rules lên `memento-fre`. Storage vẫn deny-all. Chi tiết kết nối, kiểm tra và phát hành ở [Firebase environment](docs/firebase-environment.md). Commit không tự deploy những thay đổi tiếp theo.

## Tài liệu

- [Tiến độ và việc tiếp theo](docs/mvp-progress.md)
- [Phạm vi và quy tắc MVP](docs/mvp-baseline.md)
- [Môi trường Firebase](docs/firebase-environment.md)

- [Schema và kế hoạch invite](docs/data-schema.md)
- [Kiến trúc](docs/architecture.md)
- [Auth và profile](docs/register-firebase-auth.md)
- [Luồng sản phẩm](docs/mvp-user-flow.md)
- [Moment Recap — đề xuất](docs/differentiating-feature.md)
