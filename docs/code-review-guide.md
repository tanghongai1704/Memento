# Hướng dẫn đọc hiểu và review code Memento

Tài liệu này mô tả code trong repository hiện tại. Cách đọc một feature nên đi theo chiều:

`Screen (sự kiện UI) → ViewModel (state/điều phối) → Repository interface → Repository implementation/data source → Room/Firebase → Cloud Function → cập nhật Room → UI nhận state mới`

Khi review threading, kiểm tra UI không gọi `File`/`ContentResolver`, repository tự chuyển disk I/O sang injected IO dispatcher, và Firebase Task luôn xử lý đủ success/failure/canceled. Không dùng `runCatching` trực tiếp quanh suspend call nếu nó có thể nuốt `CancellationException`; project dùng `runSuspendCatching` cho trường hợp này.

## 1. Bản đồ tổng thể

### Điểm khởi động và điều hướng

1. Android khởi động `MementoApplication` để bật Hilt.
2. `MainActivity.onCreate()` gọi `MementoNavGraph()`.
3. `MementoNavGraph` khai báo toàn bộ màn hình và chuyển route.
4. Route đầu tiên là `Splash`; sau khi kiểm tra session sẽ đi `Login` hoặc `Home`.
5. Sau đăng nhập, thanh điều hướng chính gồm Home, Connections, nút tạo Post và Profile.

Đọc tại:

- `app/src/main/java/com/tangai/memento/MementoApplication.kt`
- `app/src/main/java/com/tangai/memento/MainActivity.kt`
- `app/src/main/java/com/tangai/memento/navigation/MementoNavGraph.kt` — function `MementoNavGraph`
- `app/src/main/java/com/tangai/memento/navigation/MementoRoute.kt`
- `app/src/main/java/com/tangai/memento/di/AppModule.kt` — binding repository
- `core/network/.../FirebaseModule.kt` và `core/database/.../DatabaseModule.kt` — cung cấp Firebase/Room cho Hilt

### Cách chia source code

- `presentation`: Compose Screen + ViewModel + UI state.
- `domain`: model, rule/validation, repository interface; không phụ thuộc UI.
- `data`: repository implementation, Firebase data source, mapper.
- `core/database`: Room database, entity và DAO.
- `core/domain`: model dùng chung giữa các feature.
- `functions`: backend Firebase Callable Functions và scheduled cleanup.
- `firestore.rules`, `storage.rules`: lớp kiểm soát quyền cuối cùng ở Firebase.

## 2. Auth: Splash, đăng ký, đăng nhập, quên mật khẩu, đăng xuất

### Splash và giữ phiên đăng nhập

Flow:

1. `SplashScreen` gọi `SplashViewModel.shouldNavigateToHome()`.
2. ViewModel hỏi `AuthRepository.isUserLoggedIn()`.
3. Nếu Firebase Auth đang có user thì vào Home, ngược lại vào Login.

Đọc theo thứ tự:

- `feature/auth/presentation/.../ui/SplashScreen.kt` — `SplashScreen`
- `feature/auth/presentation/.../viewmodel/SplashViewModel.kt` — `shouldNavigateToHome`
- `feature/auth/data/.../AuthRepositoryImpl.kt` — `isUserLoggedIn`

Lưu ý review: Splash chỉ kiểm tra session Firebase, không đồng bộ profile tại đây. Việc đồng bộ profile được đảm bảo trong login/sign-up.

### Đăng ký

Flow đầu-cuối:

1. `RegisterScreen` nhận email/password/confirm password và gọi các hàm của `RegisterViewModel`.
2. `RegisterViewModel.onRegisterClick()` gọi `ValidateRegisterInputUseCase`; nếu hợp lệ mới gọi `SignUpUseCase`.
3. `SignUpUseCase` gọi `AuthRepository.signUp()`.
4. `AuthRepositoryImpl.signUp()` gọi `FirebaseAuthDataSource.signUp()` để tạo Firebase Auth user.
5. Sau khi Auth thành công, `syncCurrentUserProfile()` chạy Firestore transaction:
   - tạo/khôi phục `users/{uid}`;
   - chuẩn hóa `usernameNormalized`;
   - lưu profile vào Room `users`;
   - gọi Callable `getMyInviteCode` để bảo đảm user có mã kết nối.
6. Thành công thì UI phát effect điều hướng vào Home. Nếu profile setup lỗi, app logout để tránh session nửa hoàn chỉnh.

Đọc tại:

- `RegisterScreen.kt` — điểm nhập UI
- `RegisterViewModel.kt` — `onRegisterClick`
- `ValidateRegisterInputUseCase.kt`, `ValidateEmailUseCase.kt`, `ValidatePasswordUseCase.kt`
- `SignUpUseCase.kt`
- `AuthRepositoryImpl.kt` — `signUp`, `syncCurrentUserProfile`
- `FirebaseAuthDataSource.kt` — `signUp`
- `functions/src/index.ts` — Callable `getMyInviteCode`
- `functions/src/inviteService.ts` — `getMyInviteCode`

### Đăng nhập

Flow:

1. `LoginScreen` → `LoginViewModel.onLoginClick()`.
2. ViewModel gọi `AuthRepository.login(account, password)`.
3. `FirebaseAuthDataSource.login()` đăng nhập Firebase Auth bằng email/password.
4. `AuthRepositoryImpl.login()` bắt buộc chạy `syncCurrentUserProfile()` sau khi Auth thành công.
5. Profile remote được sửa schema nếu cần, cache vào Room, invite code được bảo đảm tồn tại.
6. Nếu bước đồng bộ thất bại (trừ trường hợp offline nhưng đã có cache), app logout; nếu thành công thì đi Home.

Đọc tại `LoginScreen.kt` → `LoginViewModel.onLoginClick` → `AuthRepositoryImpl.login/syncCurrentUserProfile` → `FirebaseAuthDataSource.login`.

### Quên mật khẩu và đăng xuất

- Quên mật khẩu: `ForgotPasswordScreen` → `ForgotPasswordViewModel.sendResetEmail` → `AuthRepositoryImpl.sendPasswordResetEmail` → Firebase Auth.
- Đăng xuất: `ProfileScreen` → `LogoutViewModel.logout` → `LogoutUseCase` → repository logout → effect `NavigateToLogin` được `MementoNavGraph` thu nhận.

## 3. Profile: xem, sửa và lấy invite code

Flow:

1. `ProfileViewModel.loadProfile()` gọi song song theo luồng coroutine để lấy profile và invite code.
2. `getCurrentUserProfile()` đọc `users/{uid}` từ Firestore rồi upsert Room.
3. `getCurrentUserInviteCode()` gọi backend; backend trả mã cũ hoặc tạo mã mới trong transaction.
4. Khi bấm lưu, `ProfileViewModel.saveProfile()` validate display name/username/bio.
5. `AuthRepositoryImpl.updateCurrentUserProfile()` chạy Firestore transaction, giữ các field hệ thống và cập nhật các field người dùng.
6. Repository đọc lại remote và cập nhật Room; state mới được đẩy lên UI.

Đọc tại:

- `feature/home/presentation/.../ui/ProfileScreen.kt`
- `feature/home/presentation/.../viewmodel/ProfileViewModel.kt` — `loadProfile`, `saveProfile`
- `feature/auth/domain/.../model/ProfileValidation.kt` — `ProfileValidator.validate`
- `feature/auth/data/.../AuthRepositoryImpl.kt` — `getCurrentUserProfile`, `getCurrentUserInviteCode`, `updateCurrentUserProfile`
- `functions/src/inviteCore.ts` — format/generate/hash mã
- `functions/src/inviteService.ts` — `getMyInviteCode`

## 4. Connection: nhập invite, đồng bộ realtime và ngắt kết nối

### Tạo DIRECT connection bằng invite

Flow đầu-cuối:

1. User B nhập mã của user A tại `ConnectionScreen`.
2. `ConnectionViewModel.redeemInvite()` gọi `ConnectionRepository.redeemDirectInvite(code)`.
3. `ConnectionFirestoreDataSource.redeemDirectInvite()` gọi Callable `redeemDirectInvite` ở region `asia-southeast1`.
4. Backend chuẩn hóa và hash code, tìm chủ mã, chặn tự kết nối.
5. Backend tạo `directKey` từ hai UID và kiểm tra `directConnectionLocks`.
6. Nếu cặp đã kết nối, trả connection cũ; nếu chưa, một Firestore transaction tạo:
   - `connections/{connectionId}`;
   - hai document `members/{uid}`;
   - `directConnectionLocks/{directKey}` để chống tạo trùng.
7. Android gọi refresh connection; snapshot listener cũng nhận thay đổi và cập nhật Room.
8. UI hiển thị người còn lại bằng `displayName (@username)` nhưng mọi thao tác lưu bằng `connectionId`.

Đọc tại:

- `ConnectionScreen.kt`
- `ConnectionViewModel.kt` — `onRedeemCodeChanged`, `redeemInvite`
- `ConnectionRepositoryImpl.kt` — `redeemDirectInvite`, `getCurrentUserConnections`, `observeConnections`
- `ConnectionFirestoreDataSource.kt` — `redeemDirectInvite`, `getConnectionsForCurrentUser`, `observeCurrentUserConnectionChanges`
- `functions/src/index.ts` — Callable `redeemDirectInvite`
- `functions/src/inviteService.ts` — `redeemDirectInvite`
- `functions/src/inviteCore.ts` — `normalizeInviteCode`, `directKeyFor`

### Đồng bộ connection

`ConnectionFirestoreDataSource` listen query `connections` chứa UID hiện tại. Repository tải members/profile, rồi trong Room transaction upsert `connections`, `connection_members`, `users`. Nếu membership trước đó có trong cache nhưng không còn xuất hiện ở server, local membership được đánh `LEFT` và media cache của connection bị xóa.

### Ngắt DIRECT connection

1. UI mở xác nhận qua `requestDisconnect`/`confirmDisconnect`.
2. Repository gọi Callable `disconnectDirect`.
3. Backend transaction kiểm tra DIRECT + membership ACTIVE, đổi connection thành `CLOSED`, xóa `memberIds`, đổi cả hai member thành `LEFT`, đóng direct lock.
4. Android cập nhật Room và xóa thư mục media cache của connection.

Đọc `ConnectionViewModel.confirmDisconnect` → `ConnectionRepositoryImpl.disconnectDirect` → `ConnectionFirestoreDataSource.disconnectDirect` → `functions/src/lifecycleService.ts::disconnectDirect`.

## 5. Tạo và đăng post ảnh

### Chọn recipient và ảnh

1. Nút `+` tại navigation mở flow `CreatePost` trên `MediaPickerScreen`.
2. `CreatePostViewModel` tải connection ACTIVE, user label và draft PENDING/FAILED gần nhất.
3. Connection đầu tiên được chọn mặc định; user có thể đổi tại **Sharing with**. App dùng `connectionId`, không dùng userId làm recipient.
4. Cùng một màn hình mở Android photo picker, cho xem/thêm/xóa ảnh, chọn layout và nhập caption.
5. Một post nhận 1–5 IMAGE. Một ảnh dùng SINGLE; nhiều ảnh dùng GRID, COLLAGE hoặc CAROUSEL.

### Prepare local draft

Khi xác nhận:

1. `CreatePostViewModel.confirmAndUploadSelectedMedia()` gọi `PostRepository.preparePhotoPost()`.
2. Repository kiểm tra user, connection và membership đều ACTIVE.
3. Tạo cố định `postId` và một `mediaId` cho mỗi ảnh.
4. `PhotoProcessor.process()` lần lượt đọc URI, sửa EXIF orientation, scale cạnh tối đa và nén JPEG vào private app storage.
5. Lưu `PostEntity` trạng thái `PENDING` cùng các `MediaItemEntity` trong Room.
6. Draft này sống qua lúc app bị đóng; lần mở sau `getLatestPendingPhoto()` khôi phục để retry.

### Upload và finalize remote

1. `uploadPendingPhoto()` upload tuần tự từng file đã xử lý lên `connections/{connectionId}/posts/{postId}/{mediaId}.jpg`, kèm metadata `authorId`.
2. Storage Rules kiểm tra đường dẫn và membership.
3. Android gọi Callable `finalizePhotoPost` với cùng ID và metadata.
4. Backend đọc metadata object thật trong Storage, kiểm tra MIME, size và author.
5. Firestore transaction kiểm tra connection/member ACTIVE, tạo post và cập nhật `connection.lastPostAt`.
6. Nếu post cùng ID đã tồn tại và toàn bộ nội dung giống nhau, backend trả thành công — retry là idempotent; nếu khác thì báo conflict.
7. Android đổi local post thành `SYNCED`; nếu lỗi đổi thành `FAILED` nhưng giữ nguyên ID/file để retry.

Đọc tại:

- `CreatePostViewModel.kt` — `confirmAndUploadSelectedMedia`
- `PostRepositoryImpl.kt` — `preparePhotoPost`, `uploadPendingPhoto`, `getLatestPendingPhoto`, `discardPendingPhoto`
- `PhotoProcessor.kt` — `process`
- `core/database/.../dao/PostDao.kt`
- `storage.rules`
- `functions/src/index.ts` — Callable `finalizePhotoPost`
- `functions/src/postCore.ts` — validate input và giới hạn
- `functions/src/postService.ts` — `finalizePhotoPost`

## 6. Home feed: realtime, cache ảnh, filter và phân trang

Feed đã có đồng bộ phía người nhận và cache offline.

Flow:

1. `HomeViewModel` khởi tạo ba luồng: load dữ liệu ban đầu, observe connected users và observe posts.
2. `HomeRepositoryImpl.observePosts()` mở collection-group query `memberIds ARRAY_CONTAINS uid`, lấy đúng 20 post ACTIVE mới nhất trên toàn bộ connection.
3. Repository gắn thêm một collection-group listener cho post `DELETED` để local nhận tombstone.
4. Snapshot được parse thành `PostEntity` + `MediaItemEntity`, upsert Room trong transaction.
5. Ảnh IMAGE được tải từ Storage vào private cache bằng file `.download`, kiểm tra size, rồi rename atomically.
6. Repository đọc feed từ Room và phát `PostFeedPage`; ViewModel cập nhật `HomeUiState`.
7. `HomeScreen` render All hoặc một connection qua `FeedFilter`.
8. Khi load older, feed All dùng một cursor toàn cục; filter connection thêm `connectionId` và dùng cursor riêng, rồi merge kết quả vào Room.

Đọc tại:

- `HomeScreen.kt` — `HomeScreen`, `PostCard`, hành vi scroll/filter/delete
- `HomeViewModel.kt` — `init`, `loadHomeData`, `onFilterSelected`, `loadOlderPosts`, `getFilteredPosts`
- `HomeRepositoryImpl.kt` — `observePosts`, `activePostsQuery`, `syncDocumentMetadata`, `cacheMedia`, `loadOlderPosts`, `currentPage`
- `HomeRepository.kt` — `PostFeedPage`
- `PostDao.kt` — `loadPosts` và mapper entity/domain

Điểm quan trọng khi review: UI đọc feed từ Room; Firestore listener có nhiệm vụ làm đầy/cập nhật Room. Đây là thiết kế offline-first, không phải Compose đọc trực tiếp Firestore.

## 7. Xóa post

Flow:

1. Home chỉ cho hiện thao tác xóa khi `HomeViewModel.canDelete(post)` thấy author là UID hiện tại.
2. `deletePost()` gọi repository.
3. Repository kiểm tra lại author rồi gọi Callable `softDeletePost`.
4. Backend transaction kiểm tra member ACTIVE và caller đúng là author, sau đó đổi status thành `DELETED` và ghi `deletedAt/deletedBy`.
5. Android mark deleted trong Room và xóa ảnh cache.
6. Thiết bị khác nhận tombstone qua deletion listener và cũng xóa cache.
7. Scheduled Function `cleanupExpiredMedia` chạy hằng ngày lúc 03:00, xóa Storage object của post đã delete sau grace period 7 ngày.

Đọc tại:

- `HomeViewModel.kt` — `canDelete`, `deletePost`
- `HomeRepositoryImpl.kt` — `deletePost`, nhánh DELETED trong `syncDocuments`
- `functions/src/lifecycleService.ts` — `softDeletePost`
- `functions/src/cleanupCore.ts`, `cleanupService.ts`
- `functions/src/index.ts` — scheduled `cleanupExpiredMedia`

## 8. Room và schema nên đọc thế nào

Đọc theo thứ tự:

1. `core/domain/.../model` để hiểu object nghiệp vụ.
2. `core/database/.../model` để hiểu dữ liệu lưu local và mapper `toDomain/toEntity`.
3. `MementoDatabase.kt` để biết version, entity và DAO.
4. `SchemaMigration.kt` để hiểu dữ liệu cũ được nâng cấp ra sao.
5. DAO để hiểu điều kiện thật sự khi UI đọc dữ liệu.
6. `docs/data-schema.md`, `firestore.rules`, `storage.rules` để so với remote schema/quyền.

Các bảng chính: `users`, `connections`, `connection_members`, `posts`, `media_items`. Post/media dùng khóa ghép theo connection để cùng một ID không bị đọc nhầm giữa connection.

## 9. Checklist review từng feature

Khi review một flow, lần theo đủ các câu hỏi sau:

1. UI gọi event nào và có thể gọi lặp hay không?
2. ViewModel khóa loading/chống double click và giữ state lỗi thế nào?
3. Validation chỉ có ở client hay backend cũng kiểm tra lại?
4. Repository có kiểm tra account bị đổi giữa coroutine hay không?
5. Remote write có transaction/idempotent hay không?
6. Sau remote success nhưng local cache fail thì UI báo gì?
7. Room transaction có giữ tính nhất quán giữa parent/child không?
8. Listener được tháo trong `awaitClose`/lifecycle hay không?
9. File tạm/cache được xóa ở success, failure, delete và revoke access hay chưa?
10. Firestore/Storage Rules có thực sự chặn client bypass Cloud Function hay không?

## 10. Thứ tự đọc đề xuất

Để nắm dự án nhanh nhất:

1. `MementoNavGraph.kt` và `MementoRoute.kt`.
2. Toàn bộ `core/domain/model`.
3. Auth flow: `LoginViewModel` → `AuthRepositoryImpl`.
4. Connection flow: `ConnectionViewModel` → repository/data source → `inviteService.ts`.
5. Create post: `CreatePostViewModel` → `PostRepositoryImpl` → `postService.ts`.
6. Feed: `HomeViewModel` → `HomeRepositoryImpl` → `PostDao`.
7. Delete/disconnect/cleanup: `lifecycleService.ts`, `cleanupService.ts`.
8. Cuối cùng đọc rules, migrations và test để xác nhận invariant.

## 11. Phần đã có và phần chưa hoàn thiện

Đã có trong code: email/password auth, profile, invite code cố định, DIRECT connection, realtime connection/profile, disconnect, post PHOTO 1–5 ảnh với bốn layout, draft/retry, upload/finalize idempotent, realtime post sync, pagination, download cache, soft delete và scheduled cleanup.

Chưa hoàn thiện hoặc chưa có: GROUP connection/invite, VIDEO, bộ lọc Home nâng cao, avatar upload và các trải nghiệm production sâu hơn như retry nền bằng WorkManager.
