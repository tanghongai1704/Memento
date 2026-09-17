# Flow ảnh: chọn, xử lý, upload, retry và xóa post

Tài liệu này mô tả lifecycle đầy đủ của một post ảnh trong code hiện tại, từ lúc người dùng chọn ảnh cho đến khi ảnh bị xóa khỏi Storage.

## 1. Sơ đồ tổng quát

```text
Chọn connection + ảnh
        ↓
Giữ URI trong UI state
        ↓
Tạo post local PENDING + sinh postId/mediaId
        ↓
Đọc ảnh → sửa EXIF → resize → nén JPEG ≤ 5 MiB
        ↓
Lưu file private + metadata vào Room
        ↓
Upload Firebase Storage
        ↓
Callable finalizePhotoPost xác minh file
        ↓
Firestore transaction tạo post + cập nhật lastPostAt
        ↓
Room: PENDING/FAILED → SYNCED
        ↓
Các thiết bị nhận snapshot → lưu Room → tải ảnh về cache
        ↓
Tác giả xóa post → soft delete Firestore → xóa cache local
        ↓
Scheduled cleanup xóa file Storage sau 7 ngày
```

Các trạng thái local quan trọng:

```text
PENDING ──upload/finalize thành công──> SYNCED
   │
   └──xử lý/upload/finalize lỗi──────> FAILED
                                          │
                                          ├──retry thành công──> SYNCED
                                          └──discard───────────> xóa draft local
```

`PostStatus.ACTIVE/DELETED` là trạng thái nghiệp vụ của post. `LocalSyncStatus.PENDING/FAILED/SYNCED` là trạng thái đồng bộ trên thiết bị. Hai loại trạng thái này giải quyết hai vấn đề khác nhau.

## 2. Những object chính

### `LocalMediaItem`

Chỉ đại diện cho ảnh vừa được chọn trên thiết bị. Nó giữ URI local và chưa phải dữ liệu post persisted.

Đọc tại `core/domain/src/main/kotlin/com/tangai/memento/domain/model/LocalMediaItem.kt`.

### `PostEntity` và `MediaItemEntity`

- `PostEntity`: caption, author, connection, timestamp, `PostStatus`, `LocalSyncStatus`.
- `MediaItemEntity`: `mediaId`, Storage path, MIME, width, height, size và position.
- Hai object được lưu ở hai bảng Room `posts` và `media_items`.

Đọc tại:

- `core/database/.../model/PostEntity.kt`
- `core/database/.../model/MediaItemEntity.kt`
- `core/database/.../dao/PostDao.kt`

### `PendingPhotoPost`

Gói một `Post` đã có ID/metadata với URI của file JPEG đã xử lý trong private storage. Object này là đầu vào của upload và retry.

Đọc tại `feature/post/domain/.../PendingPhotoPost.kt`.

## 3. Chọn nơi đăng và chọn ảnh

### 3.1 Mở flow tạo post

Nút `+` trong `MementoNavGraph` điều hướng tới `CreatePost`. Từ đây người dùng chọn một DIRECT connection đang ACTIVE.

`CreatePostViewModel` tải:

- danh sách connection từ `ConnectionRepository.getCurrentUserConnections()`;
- profile người còn lại để tạo label;
- draft PENDING/FAILED gần nhất từ `PostRepository.getLatestPendingPhoto()`.

Điểm đọc code:

- `app/src/main/java/com/tangai/memento/navigation/MementoNavGraph.kt`
- `feature/post/presentation/.../ui/CreatePostScreen.kt`
- `feature/post/presentation/.../viewmodel/CreatePostViewModel.kt` — khối `init`

### 3.2 Chọn ảnh

`MediaPickerScreen` mở Android photo picker. Ảnh trả về được chuyển thành `LocalMediaItem` và đưa vào `CreatePostViewModel.addSelectedMedia()`.

Hàm này:

1. Bổ sung display name nếu thiếu.
2. Ép type thành `IMAGE`.
3. Giữ đúng một item bằng `selectedMedia = listOf(...)`.
4. Xóa reference tới pending cũ trong UI state khi user chọn ảnh mới.

`MediaPreviewScreen` hiển thị ảnh, cho nhập caption hoặc bỏ ảnh đã chọn. Caption được giới hạn 1.000 ký tự ngay ở `onCaptionChanged()`.

Điểm đọc code:

- `MediaPickerScreen.kt`
- `MediaPreviewScreen.kt`
- `CreatePostViewModel.kt` — `addSelectedMedia`, `removeSelectedMedia`, `onCaptionChanged`

## 4. Khi bấm đăng

Entry point là `CreatePostViewModel.confirmAndUploadSelectedMedia()`.

Hàm xử lý theo hai nhánh:

- Chưa có draft: gọi `preparePhotoPost()` để xử lý ảnh và tạo draft.
- Đã có `pendingPhoto`: dùng lại draft cũ, không tạo ID và không nén ảnh lần nữa.

Sau khi có `PendingPhotoPost`, ViewModel gọi `uploadPendingPhoto()` và cập nhật `uploadProgress` cho UI.

Đọc tại `CreatePostViewModel.kt` — `confirmAndUploadSelectedMedia`.

## 5. Chuẩn bị draft local

Entry point: `PostRepositoryImpl.preparePhotoPost(connectionId, media, caption)`.

### 5.1 Validation trước khi xử lý

Repository kiểm tra:

1. User vẫn đăng nhập.
2. Không tồn tại draft PENDING/FAILED hợp lệ khác.
3. Media là IMAGE.
4. Caption sau khi trim không vượt quá 1.000 ký tự.
5. Connection trong Room là ACTIVE.
6. Membership của UID hiện tại trong connection là ACTIVE.

Nếu đang có draft, user phải đăng tiếp hoặc discard draft đó trước khi tạo post mới.

### 5.2 Sinh ID một lần

Repository sinh:

- `postId` bằng Firestore auto-ID;
- `mediaId` bằng UUID;
- `clientCreatedAt` bằng thời gian thiết bị.

Những giá trị này được giữ nguyên trong toàn bộ vòng đời retry. Nhờ vậy retry không tạo thêm post hoặc Storage path mới.

### 5.3 Tạo record PENDING

Một `PostEntity` được lưu vào Room với:

- `postType = PHOTO`;
- `layoutType = SINGLE`;
- `status = ACTIVE`;
- `localSyncStatus = PENDING`;
- `createdAt/updatedAt = null` vì server chưa xác nhận.

Sau đó ảnh mới được xử lý. Nếu xử lý ảnh lỗi, record được đổi sang `FAILED`.

Điểm đọc code:

- `feature/post/data/.../PostRepositoryImpl.kt` — `preparePhotoPost`
- `core/database/.../dao/PostDao.kt` — `upsertPost`, `updateSyncState`

## 6. Xử lý ảnh local

Entry point: `PhotoProcessor.process()`.

Pipeline:

1. Kiểm tra MIME từ `ContentResolver` phải là image nếu MIME có tồn tại.
2. Decode bounds trước để chọn `inSampleSize`, tránh decode ảnh cực lớn nguyên kích thước.
3. Đọc EXIF orientation.
4. Xoay/lật bitmap theo EXIF.
5. Resize cạnh dài nhất xuống tối đa 1.920 px.
6. Encode JPEG ban đầu với quality 82.
7. Nếu lớn hơn 5 MiB, giảm quality từng bước 8 cho tới quality 46.
8. Nếu vẫn lớn, tiếp tục giảm kích thước bitmap còn 82% qua từng vòng và encode quality 74.
9. Nếu cuối cùng vẫn vượt 5 MiB thì báo lỗi.

File được lưu tại:

```text
<app files>/pending_media/{connectionId}/{postId}/{mediaId}.jpg
```

Sau khi xử lý thành công, repository lưu `MediaItemEntity` gồm Storage path, kích thước ảnh và số byte thật. MIME persisted luôn là `image/jpeg`.

Storage path tương ứng:

```text
connections/{connectionId}/posts/{postId}/{mediaId}.jpg
```

Đọc tại:

- `feature/post/data/.../PhotoProcessor.kt` — `process`, `decodeSampled`, `applyOrientation`, `resizeLongestEdge`
- `PostRepositoryImpl.kt` — phần tạo `MediaItemEntity` trong `preparePhotoPost`

## 7. Upload lên Firebase Storage

Entry point: `PostRepositoryImpl.uploadPendingPhoto()`.

Trước khi upload, repository kiểm tra:

1. Pending post thuộc đúng UID hiện tại.
2. Post có đúng một media item.
3. File local còn tồn tại.
4. Kích thước file local đúng bằng `sizeBytes` đã persisted.

Upload sử dụng đúng Storage path đã tạo khi prepare. Metadata gồm:

- `contentType = image/jpeg`;
- custom metadata `authorId = uid`.

`awaitUpload()` chuyển Firebase progress thành `Float` từ 0 đến 1 để ViewModel hiển thị tiến độ. Nếu coroutine bị cancel, upload task cũng bị cancel.

Điểm đọc code:

- `PostRepositoryImpl.kt` — `uploadPendingPhoto`, `awaitUpload`
- `storage.rules` — quyền tạo/đọc/xóa Storage object

## 8. Finalize post trên backend

Upload Storage thành công chưa có nghĩa là post đã được publish. Android phải gọi Callable `finalizePhotoPost`.

Payload gồm:

- connection/post/media ID;
- `clientCreatedAt` và caption;
- Storage path;
- MIME, width, height và size.

### 8.1 Validate request

`functions/src/postCore.ts::parseFinalizePhotoInput()` kiểm tra kiểu dữ liệu, ID/path, caption và các giới hạn như 5 MiB, cạnh tối đa 1.920 px.

### 8.2 Xác minh object Storage

Callable đọc metadata thật của object vừa upload và yêu cầu:

- content type là `image/jpeg`;
- byte size đúng bằng payload;
- metadata `authorId` đúng bằng UID đang gọi.

Client không thể chỉ gửi metadata giả để tạo post không có file hợp lệ.

### 8.3 Firestore transaction

`postService.finalizePhotoPost()` đọc trong transaction:

- connection;
- membership của caller;
- post cùng ID nếu đã tồn tại.

Nếu post chưa tồn tại, backend yêu cầu connection ACTIVE, UID nằm trong `memberIds` và membership ACTIVE. Sau đó transaction:

1. Tạo `connections/{connectionId}/posts/{postId}`.
2. Ghi một media item PHOTO/SINGLE.
3. Ghi timestamp server và `status = ACTIVE`.
4. Cập nhật `connection.lastPostAt` và `updatedAt`.

Điểm đọc code:

- `functions/src/index.ts` — Callable `finalizePhotoPost`
- `functions/src/postCore.ts`
- `functions/src/postService.ts` — `finalizePhotoPost`
- `firestore.rules`

## 9. Idempotency và retry

Retry sử dụng lại hoàn toàn:

- `postId`;
- `mediaId`;
- Storage path;
- file JPEG đã xử lý;
- caption và `clientCreatedAt` đã persisted.

Nếu lần gọi trước đã tạo Firestore post nhưng response về điện thoại bị mất, lần retry sẽ gặp post đã tồn tại. Backend so sánh toàn bộ field quan trọng:

- author, connection, post/layout type;
- caption, `clientCreatedAt`, status;
- toàn bộ metadata media.

Nếu giống hệt, backend trả timestamp cũ như một lần thành công bình thường. Nếu cùng ID nhưng nội dung khác, backend trả `POST_CONFLICT`. Do đó retry không tạo post trùng.

### Các kiểu lỗi

```text
Xử lý ảnh lỗi
→ local post chuyển FAILED; thường chưa có MediaItem/file hợp lệ.

Upload Storage lỗi
→ local post chuyển FAILED; file local và ID còn để retry.

Upload xong nhưng finalize lỗi
→ local post chuyển FAILED; object Storage có thể đã tồn tại.
→ retry upload đè đúng path rồi finalize lại.

Finalize đã commit nhưng client mất response
→ local post chuyển FAILED.
→ retry được backend nhận diện là cùng post và trả success.

Account đổi giữa lúc upload
→ repository chặn cập nhật Room bằng kiểm tra lại UID.
```

Mọi exception trong khối upload/finalize đều gọi `updateSyncState(..., FAILED)` rồi trả lỗi cho ViewModel.

Đọc tại:

- `PostRepositoryImpl.kt` — `uploadPendingPhoto`
- `functions/src/postService.ts` — nhánh `existingPost.exists`

## 10. Khôi phục retry sau khi mở lại app

Trong `CreatePostViewModel.init`, app gọi `getLatestPendingPhoto()`.

Repository lấy các post của UID có `localSyncStatus IN ('PENDING', 'FAILED')`, mới nhất trước. Với từng record:

1. Lấy media item tương ứng.
2. Dựng lại đường dẫn file bằng `PhotoProcessor.outputFile()`.
3. Kiểm tra có đúng một media, file tồn tại và size đúng.
4. Nếu hợp lệ, trả `PendingPhotoPost` cho UI.
5. Nếu không hợp lệ, xóa draft Room và cả thư mục file hỏng rồi xét draft tiếp theo.

UI khôi phục:

- recipient từ `connectionId`;
- ảnh preview từ local URI;
- caption cũ;
- `pendingPhoto` để lần bấm đăng sau bỏ qua bước xử lý ảnh.

Đọc tại:

- `PostDao.kt` — `getRetryablePosts`
- `PostRepositoryImpl.kt` — `getLatestPendingPhoto`
- `CreatePostViewModel.kt` — khối `init`

## 11. Discard draft

User có thể bỏ draft PENDING/FAILED bằng `CreatePostViewModel.discardPendingPhoto()`.

Repository:

1. Xác minh draft thuộc UID hiện tại.
2. Gọi `PostDao.deleteLocalDraft()`; query chỉ xóa record PENDING hoặc FAILED.
3. Xóa thư mục local của post.

Discard không gọi xóa Storage. Nếu object đã upload nhưng finalize chưa tạo post, nó trở thành orphan và được scheduled cleanup xử lý theo rule orphan của backend.

Đọc tại:

- `CreatePostViewModel.kt` — `discardPendingPhoto`
- `PostRepositoryImpl.kt` — `discardPendingPhoto`
- `PostDao.kt` — `deleteLocalDraft`
- `functions/src/cleanupService.ts`

## 12. Sau khi publish thành công

Backend trả `createdAtMillis` và `updatedAtMillis`. Android chạy một Room transaction:

1. Đổi post thành `LocalSyncStatus.SYNCED`.
2. Ghi timestamp server.
3. Cập nhật activity của connection.

File JPEG local không bị xóa ngay. Nó đồng thời là cache để Home của người gửi render ảnh mà không cần tải lại.

ViewModel:

- đặt progress thành 100%;
- xóa `pendingPhoto`;
- hiện thông báo thành công;
- gọi callback điều hướng.

## 13. Đồng bộ post và tải ảnh ở thiết bị nhận

`HomeRepositoryImpl.observePosts()` theo dõi các connection ACTIVE của UID. Với mỗi connection, repository gắn:

- listener lấy 20 post mới nhất theo `createdAt`;
- listener riêng lấy tombstone `DELETED`.

Khi nhận post ACTIVE:

1. Parse document thành `PostEntity` và `MediaItemEntity`.
2. Upsert cả hai vào Room transaction.
3. Tải IMAGE từ Storage về file `.download` tạm.
4. Kiểm tra số byte đúng với metadata.
5. Rename file tạm thành file cache chính thức.
6. Đọc feed từ Room rồi phát `PostFeedPage` cho ViewModel.

Cache phía nhận dùng cùng cấu trúc:

```text
<app files>/pending_media/{connectionId}/{postId}/{mediaId}.jpg
```

Đọc tại `feature/home/data/.../HomeRepositoryImpl.kt` — `observePosts`, `syncDocuments`, `storePost`, `cacheMedia`.

## 14. Xóa một post đã publish

Đây là soft delete, không xóa Firestore document ngay.

### 14.1 Kiểm tra tại UI và Android repository

`HomeViewModel.canDelete(post)` chỉ cho tác giả thấy thao tác xóa. Khi xác nhận:

1. `HomeViewModel.deletePost()` gọi `HomeRepository.deletePost()`.
2. Repository kiểm tra lại `post.authorId == current uid`.
3. Repository gọi Callable `softDeletePost(connectionId, postId)`.

Client check chỉ phục vụ UX; quyền thật vẫn được backend kiểm tra.

### 14.2 Backend soft delete

Trong Firestore transaction, backend kiểm tra:

- connection ACTIVE;
- caller có membership ACTIVE;
- post tồn tại;
- caller là author.

Sau đó cập nhật:

```text
status = DELETED
deletedAt = server time
deletedBy = uid
updatedAt = server time
```

Nếu chính tác giả gọi lại với post đã DELETED, backend trả success cũ, nên thao tác xóa cũng idempotent.

### 14.3 Cập nhật local và các thiết bị khác

Thiết bị gọi xóa:

1. `PostDao.markDeleted()` cập nhật tombstone local và `localSyncStatus = SYNCED`.
2. Xóa file ảnh cache.

Thiết bị khác:

1. Deletion listener nhận post `DELETED`.
2. Upsert tombstone vào Room.
3. Xóa file cache.
4. Query feed chỉ lấy `status = ACTIVE`, nên post biến mất khỏi UI.

Đọc tại:

- `feature/home/presentation/.../viewmodel/HomeViewModel.kt` — `canDelete`, `deletePost`
- `feature/home/data/.../HomeRepositoryImpl.kt` — `deletePost`, `syncDocuments`
- `PostDao.kt` — `markDeleted`, query `getPosts`
- `functions/src/index.ts` — Callable `softDeletePost`
- `functions/src/lifecycleService.ts` — `softDeletePost`

## 15. Xóa file vật lý khỏi Storage

Soft delete không xóa ảnh Storage ngay. Scheduled Function `cleanupExpiredMedia` chạy mỗi ngày lúc 03:00 theo timezone `Asia/Ho_Chi_Minh`.

Cleanup duyệt media và quyết định xóa khi:

- object thuộc post đã DELETED quá grace period; hoặc
- object là orphan, nghĩa là upload đã tồn tại nhưng không có post hợp lệ sau grace period.

Grace period hiện là 7 ngày (`MEDIA_CLEANUP_GRACE_MS`). Khoảng chờ này giảm nguy cơ xóa nhầm object khi upload/finalize hoặc retry đang dang dở.

Đọc tại:

- `functions/src/index.ts` — scheduled `cleanupExpiredMedia`
- `functions/src/cleanupCore.ts` — `MEDIA_CLEANUP_GRACE_MS`, `shouldDeleteStoredMedia`
- `functions/src/cleanupService.ts` — `cleanupExpiredMedia`

## 16. Các invariant cần giữ khi sửa code

1. `postId`, `mediaId` và Storage path phải được sinh một lần rồi giữ nguyên khi retry.
2. Không đổi caption/recipient của một pending draft trong lúc retry; nếu muốn đổi phải discard và tạo draft mới.
3. File upload phải đúng file JPEG đã persisted metadata.
4. Remote post chỉ được tạo qua Callable sau khi backend xác minh Storage object và membership.
5. Một connection không ACTIVE không được prepare/finalize post mới.
6. Firestore commit thành công nhưng local update thất bại phải vẫn có khả năng hồi phục qua realtime sync/retry.
7. Feed chỉ hiển thị post ACTIVE và phải guard theo membership của UID hiện tại.
8. Soft delete phải phát tombstone đủ lâu để thiết bị khác đồng bộ.
9. Cache phải bị xóa khi post bị delete hoặc user mất quyền connection.
10. Cleanup không được xóa object quá sớm vì sẽ phá retry.

## 17. Thứ tự đọc code đề xuất

1. `CreatePostViewModel.confirmAndUploadSelectedMedia()`.
2. `PostRepositoryImpl.preparePhotoPost()`.
3. `PhotoProcessor.process()`.
4. `PostRepositoryImpl.uploadPendingPhoto()`.
5. `functions/src/postCore.ts` và `postService.ts`.
6. `PostRepositoryImpl.getLatestPendingPhoto()` và `discardPendingPhoto()`.
7. `HomeRepositoryImpl.observePosts()` và `cacheMedia()`.
8. `HomeRepositoryImpl.deletePost()` và `lifecycleService.softDeletePost()`.
9. `cleanupService.ts` và `cleanupCore.ts`.
10. Cuối cùng đọc `PostDao.kt`, `firestore.rules` và `storage.rules` để kiểm tra toàn bộ invariant.
