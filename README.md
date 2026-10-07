# Memento

Memento là ứng dụng Android theo đề bài **Private Stories**, tập trung vào việc chia sẻ khoảnh khắc ảnh giữa hai người đã kết nối. Project ưu tiên quyền riêng tư, khả năng sử dụng khi mạng không ổn định và quy trình upload có thể tiếp tục sau khi người dùng rời màn hình.

## Chức năng chính

- Đăng ký, đăng nhập, đăng xuất và đặt lại mật khẩu bằng Firebase Authentication.
- Đồng bộ profile theo Firebase UID; cho phép cập nhật display name, username và bio.
- Kết nối hai người bằng invite code cố định; backend ngăn self-connect và connection trùng.
- Tạo post gồm 1–5 ảnh với bốn layout: SINGLE, GRID, COLLAGE và CAROUSEL.
- Sửa EXIF, resize và nén JPEG trên thiết bị trước khi upload.
- Lưu draft trong Room, chạy upload bằng WorkManager và retry với cùng post ID.
- Đồng bộ feed realtime từ Firestore về Room; UI đọc dữ liệu local trước và hỗ trợ xem ảnh đã cache khi offline.
- Soft-delete post, disconnect và thu hồi quyền đọc dữ liệu remote.
- Kiểm tra Functions, Room schema, Firestore Rules và Storage Rules bằng test và Firebase Emulator.

## Kiến trúc

```text
Compose UI
    │
ViewModel
    │
Domain repository contract
    │
Repository implementation
    ├── Room ───────────────> nguồn dữ liệu cho UI
    ├── Firebase Auth ──────> identity và session
    ├── Firestore ──────────> metadata và realtime events
    ├── Firebase Storage ───> JPEG objects
    └── Cloud Functions ────> mutation cần kiểm tra quyền
```

Source code được chia theo feature và layer. `app` giữ navigation, Hilt composition và WorkManager; các module `feature/*` chứa presentation, domain và data; `core/*` chứa model, Room, network adapter và UI dùng chung.

Chi tiết thiết kế nằm trong [tài liệu kiến trúc](docs/architecture.md).

## Công nghệ

| Nhóm | Công nghệ |
|---|---|
| Android | Kotlin, Jetpack Compose, Material 3 |
| Architecture | MVVM, repository pattern, Hilt |
| Local data | Room schema v3, private app files, Coil cache |
| Background work | WorkManager |
| Backend | Firebase Auth, Firestore, Storage, Cloud Functions |
| Verification | JUnit, Python schema checks, Firebase Emulator Suite |

## Cấu trúc repository

```text
app/                     Application shell, navigation và background worker
core/                    Domain model, Room, network, design system và shared UI
feature/auth/            Authentication và profile synchronization
feature/connection/      Invite code, DIRECT connection và disconnect
feature/home/            Feed, pagination, media cache và delete post
feature/post/            Create post, xử lý ảnh và upload queue
functions/               Callable Functions, scheduled cleanup và backend tests
docs/                    Tài liệu kiến trúc, schema và hướng dẫn review
submission/              Slide PDF và thông tin demo dùng khi nộp bài
```

## Yêu cầu môi trường

- Android Studio và Android SDK 37.
- Gradle daemon sử dụng JDK 25 theo `gradle/gradle-daemon-jvm.properties`; Java source compatibility là 11.
- Node.js 22 cho Cloud Functions, được khai báo trong `functions/.nvmrc`.
- Firebase CLI để chạy emulator tests.
- Firebase project đã bật Email/Password Authentication.

## Clone và build ứng dụng

1. Clone repository và chuyển vào thư mục project:

   ```sh
   git clone ssh://git@fgit.zapps.vn:8022/aith_fresher/memento.git
   cd Memento
   ```

2. Chuẩn bị cấu hình Firebase Android:

   - Trong Firebase Console, mở project `memento-fre` và tải file cấu hình của Android app có package `com.tangai.memento`.
   - Đặt file đúng tại `app/google-services.json`.
   - File này được loại khỏi Git; người clone repository phải được chủ project cung cấp file hoặc cấp quyền tải từ Firebase Console.

3. Mở thư mục project bằng Android Studio. IDE sẽ tự tạo `local.properties` theo Android SDK trên máy. Nếu cần tạo thủ công, file chỉ cần khai báo đường dẫn SDK phù hợp, ví dụ trên macOS:

   ```properties
   sdk.dir=/Users/<username>/Library/Android/sdk
   ```

   Không copy `local.properties` từ máy khác vì đường dẫn SDK phụ thuộc từng máy.

4. Chờ Gradle Sync hoàn tất, chọn build variant `debug`, sau đó chạy app trên emulator hoặc thiết bị Android. Có thể build từ Terminal tại root project:

   ```sh
   ./gradlew :app:assembleDebug
   ```

APK debug được tạo tại `app/build/outputs/apk/debug/app-debug.apk`.

Build và chạy app thông thường không yêu cầu Firebase Admin service account, App Check debug token hoặc Firebase Emulator Suite. Các thông tin này chỉ cần cho tác vụ quản trị, kiểm thử App Check hoặc emulator test tương ứng. Nếu thiết bị từng được cấu hình dùng Firebase Emulator, cần xóa app data hoặc marker `use_firebase_emulators` trước khi demo với Firebase thật.

## Kiểm tra project

Chạy Android build trước để Room sinh schema implementation phục vụ script kiểm tra:

```sh
./gradlew :app:assembleDebug
python3 tools/check_schema.py
./gradlew test testDebugUnitTest lintDebug
```

Kiểm tra Cloud Functions và Firebase Rules:

```sh
cd functions
npm ci
npm test
npm run test:emulator
npm run test:storage-rules
cd ..

firebase emulators:exec --only auth,firestore --project demo-memento-schema \
  "python3 tools/check_firestore_rules.py && python3 tools/check_auth_profile.py"
```

Các thay đổi trong repository không tự deploy lên Firebase. Đọc [hướng dẫn môi trường Firebase](docs/firebase-environment.md) trước khi chạy lệnh tác động đến project thật.

## Tài liệu

- [Danh mục tài liệu](docs/README.md)
- [Trạng thái và phạm vi project](docs/project-status.md)
- [Kiến trúc và luồng dữ liệu](docs/architecture.md)
- [Schema Firestore và Room](docs/data-schema.md)
- [Lifecycle post ảnh](docs/photo-post-lifecycle.md)
- [Hướng dẫn đọc và review code](docs/code-review-guide.md)
- [Firebase Auth và user profile](docs/register-firebase-auth.md)
- [Môi trường Firebase](docs/firebase-environment.md)

## Submission

Slide PDF và thông tin demo được đặt trong folder `submission/`. Folder này chỉ chứa artifact phục vụ nộp bài, không tham gia vào Android build.

## Phạm vi hiện tại

Project đã hoàn thiện journey PHOTO cho connection DIRECT. GROUP, VIDEO, avatar upload và bộ lọc feed nâng cao chưa nằm trong bản hiện tại; hướng mở rộng được mô tả trong [project status](docs/project-status.md).
