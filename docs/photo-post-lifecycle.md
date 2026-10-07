# Flow ảnh: chọn, xử lý, upload, retry và xóa post

Tài liệu này mô tả lifecycle đầy đủ của một post ảnh trong code hiện tại, từ lúc người dùng chọn ảnh cho đến khi ảnh bị xóa khỏi Storage.

## 1. Sơ đồ tổng quát

```text
Chọn connection + 1–5 ảnh
        ↓
Giữ URI trong UI state
        ↓
Tạo post local PENDING + sinh postId/mediaId
        ↓
Lần lượt đọc ảnh → sửa EXIF → resize → nén mỗi JPEG ≤ 5 MiB
        ↓
Lưu file private + metadata vào Room
        ↓
Đưa job vào WorkManager queue, chờ có mạng
        ↓
Lần lượt upload các file lên Firebase Storage
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
   └──upload/finalize lỗi────────────> FAILED
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

## 3. Tạo moment trên một màn hình

### 3.1 Mở flow tạo post

Nút `+` trong `MementoNavGraph` điều hướng tới `CreatePost`, nơi `MediaPickerScreen` gom toàn bộ thao tác chọn người nhận, chọn ảnh, đổi layout, viết caption và đăng. Connection ACTIVE đầu tiên được chọn mặc định; người dùng có thể đổi tại mục **Sharing with**.

`CreatePostViewModel` tải:

- danh sách connection từ `ConnectionRepository.getCurrentUserConnections()`;
- profile người còn lại để tạo label.

`UploadQueueViewModel` khởi động queue dùng chung và gọi `PostRepository.getPendingPhotos()` để khôi phục mọi draft PENDING/FAILED.

Điểm đọc code:

- `app/src/main/java/com/tangai/memento/navigation/MementoNavGraph.kt`
- `feature/post/presentation/.../ui/MediaPickerScreen.kt`
- `feature/post/presentation/.../viewmodel/CreatePostViewModel.kt` — khối `init`

### 3.2 Chọn ảnh và layout

`MediaPickerScreen` mở Android photo picker với giới hạn 5 ảnh. Các URI trả về được chuyển thành `LocalMediaItem`, loại trùng theo URI và giữ nguyên thứ tự chọn.

ViewModel:

1. Bổ sung display name nếu thiếu.
2. Chỉ nhận tối đa 5 item IMAGE khác URI.
3. Dùng SINGLE cho một ảnh và GRID mặc định khi có nhiều ảnh.
4. Cho đổi giữa GRID, COLLAGE và CAROUSEL ngay trên cùng màn hình.

`MediaPickerScreen` dùng chung `PhotoLayout` với Home, đồng thời cho thêm/xóa ảnh, chọn layout, nhập caption hoặc bỏ bộ ảnh đã chọn mà không phải chuyển màn hình. Caption được giới hạn 1.000 ký tự ngay ở `onCaptionChanged()`.

Điểm đọc code:

- `MediaPickerScreen.kt`
- `CreatePostViewModel.kt` — `setSelectedMedia`, `removeSelectedMedia`, `onCaptionChanged`

## 4. Khi bấm đăng

Entry point là `CreatePostViewModel.confirmAndUploadSelectedMedia()`.

Hàm thêm media/caption/recipient vào `PostUploadQueue` và điều hướng về Home ngay. Queue cấp ứng dụng tiếp tục prepare dù user chuyển giữa Home, Connections, Profile hoặc mở màn hình tạo bài khác. Sau khi draft local hoàn chỉnh, app giao upload cho WorkManager; job vẫn được hệ điều hành giữ khi app rời foreground hoặc process bị dừng. Nhiều bài được nối vào một unique work chain theo thứ tự đã thêm.

`PostUploadStatusBar` nằm phía trên nội dung chính, có thể mở rộng để xem từng bài, tiến độ, bài đang chờ và thao tác Retry/Retry all/Remove.

Đọc tại `CreatePostViewModel.kt` — `confirmAndUploadSelectedMedia`.

## 5. Chuẩn bị draft local

Entry point: `PostRepositoryImpl.preparePhotoPost(connectionId, media, layoutType, caption, onProgress)`.

### 5.1 Validation trước khi xử lý

Repository kiểm tra:

1. User vẫn đăng nhập.
2. Có 1–5 media và tất cả đều là IMAGE.
3. Một ảnh bắt buộc SINGLE; nhiều ảnh bắt buộc GRID, COLLAGE hoặc CAROUSEL.
4. Caption sau khi trim không vượt quá 1.000 ký tự.
5. Connection trong Room là ACTIVE.
6. Membership của UID hiện tại trong connection là ACTIVE.

### 5.2 Sinh ID một lần

Repository sinh:

- `postId` bằng Firestore auto-ID;
- mỗi ảnh có một `mediaId` bằng UUID;
- `clientCreatedAt` bằng thời gian thiết bị.

Những giá trị này được giữ nguyên trong toàn bộ vòng đời retry. Nhờ vậy retry không tạo thêm post hoặc Storage path mới.

### 5.3 Tạo record PENDING

Một `PostEntity` được lưu vào Room với:

- `postType = PHOTO`;
- `layoutType` đúng với số ảnh và lựa chọn ở Preview;
- `status = ACTIVE`;
- `localSyncStatus = PENDING`;
- `createdAt/updatedAt = null` vì server chưa xác nhận.

Sau đó ảnh mới được xử lý. Nếu xử lý ảnh lỗi, record/file chưa hoàn chỉnh được xóa; item vẫn ở trạng thái lỗi trong queue của process hiện tại để user có thể retry bằng media đã chọn.

Điểm đọc code:

- `feature/post/data/.../PostRepositoryImpl.kt` — `preparePhotoPost`
- `core/database/.../dao/PostDao.kt` — `upsertPost`, `updateSyncState`

## 6. Xử lý ảnh local

Entry point: `PhotoProcessor.process()`, được repository gọi tuần tự trên `Dispatchers.IO` để không chặn UI và không giữ nhiều bitmap lớn trong bộ nhớ cùng lúc.

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

Mỗi file được lưu tại:

```text
<app files>/pending_media/{connectionId}/{postId}/{mediaId}.jpg
```

Sau mỗi ảnh, repository cập nhật tiến độ tổng. Khi toàn bộ xử lý thành công, repository lưu danh sách `MediaItemEntity` gồm position, Storage path, kích thước ảnh và số byte thật. MIME persisted luôn là `image/jpeg`.

Storage path tương ứng:

```text
connections/{connectionId}/posts/{postId}/{mediaId}.jpg
```

Đọc tại:

- `feature/post/data/.../PhotoProcessor.kt` — `process`, `decodeSampled`, `applyOrientation`, `resizeLongestEdge`
- `PostRepositoryImpl.kt` — phần tạo `MediaItemEntity` trong `preparePhotoPost`

## 7. Upload lên Firebase Storage

`WorkManagerPostUploadScheduler` tạo `PostUploadWorker` với constraint `NetworkType.CONNECTED`. Mỗi post có tag riêng để app mở lại có thể gắn vào job đang tồn tại thay vì chạy upload cạnh tranh. Tất cả job nằm trong unique chain `post-upload-queue` dùng `APPEND_OR_REPLACE`, nên chỉ chạy tuần tự.

Worker khôi phục đúng draft bằng `connectionId/postId`, sau đó gọi `PostRepositoryImpl.uploadPendingPhoto()`.

Trước khi upload, repository kiểm tra:

1. Pending post thuộc đúng UID hiện tại.
2. Post có 1–5 media item và số local URI phải khớp.
3. Mọi file local còn tồn tại.
4. Kích thước từng file local đúng bằng `sizeBytes` đã persisted.

Upload tuần tự theo `position`, dùng đúng Storage path đã tạo khi prepare. Metadata mỗi file gồm:

- `contentType = image/jpeg`;
- custom metadata `authorId = uid`.

`awaitUpload()` chuyển progress của từng file thành progress tổng từ 0 đến 1; worker ghi progress vào WorkManager để thanh trạng thái hiển thị. Mỗi file có timeout riêng; nếu coroutine bị cancel, upload task đang chạy cũng bị cancel. Lỗi tạm thời được WorkManager retry với exponential backoff; sau giới hạn tự động, draft vẫn ở trạng thái FAILED để user retry thủ công.

Điểm đọc code:

- `PostRepositoryImpl.kt` — `uploadPendingPhoto`, `awaitUpload`
- `PostUploadWorker.kt` — chạy upload bền vững và retry
- `WorkManagerPostUploadScheduler.kt` — constraint, unique chain và progress
- `storage.rules` — quyền tạo/đọc/xóa Storage object

## 8. Finalize post trên backend

Upload Storage thành công chưa có nghĩa là post đã được publish. Android phải gọi Callable `finalizePhotoPost`.

Payload gồm connection/post ID, `clientCreatedAt`, caption, layout và danh sách media có media ID, Storage path, MIME, width, height, size, position.

### 8.1 Validate request

`functions/src/postCore.ts::parseFinalizePhotoInput()` kiểm tra kiểu dữ liệu, ID/path, caption, layout, thứ tự liên tục và giới hạn 1–5 ảnh như 5 MiB/file, cạnh tối đa 1.920 px. Parser vẫn nhận payload một ảnh kiểu cũ để retry draft tạo trước khi nâng cấp.

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
2. Ghi danh sách 1–5 media item theo đúng position và layout.
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
- toàn bộ `mediaId` theo position;
- toàn bộ Storage path;
- các file JPEG đã xử lý;
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

Mọi exception trong khối upload/finalize đều cập nhật local sync state. Worker quyết định retry tự động hoặc trả trạng thái lỗi về queue UI.

Đọc tại:

- `PostRepositoryImpl.kt` — `uploadPendingPhoto`
- `functions/src/postService.ts` — nhánh `existingPost.exists`

## 10. Khôi phục retry sau khi mở lại app

WorkManager tự giữ các job đã schedule qua lúc process bị dừng. Khi UI mở lại, `UploadQueueViewModel` khởi động `PostUploadQueue`, app gọi `getPendingPhotos()` rồi gắn từng draft vào job WorkManager đang tồn tại; draft chưa có job sẽ được schedule lại.

Repository lấy các post của UID có `localSyncStatus IN ('PENDING', 'FAILED')`, cũ nhất trước để giữ đúng thứ tự queue. Với từng record:

1. Lấy media item tương ứng.
2. Dựng lại đường dẫn file bằng `PhotoProcessor.outputFile()`.
3. Kiểm tra có 1–5 media, đủ mọi file và size từng file đúng.
4. Nếu hợp lệ, trả `PendingPhotoPost` vào danh sách queue với trạng thái cần retry.
5. Nếu không hợp lệ, xóa draft Room và cả thư mục file hỏng rồi xét draft tiếp theo.

Thanh upload hiển thị lại toàn bộ bài hợp lệ. Khi có mạng, job đang chờ tự chạy; sau khi hết số lần retry tự động, user vẫn có thể retry từng bài, retry tất cả hoặc xóa bài khỏi thiết bị. Retry dùng lại `postId`, media metadata và file đã xử lý, không nén lại ảnh.

Đọc tại:

- `PostDao.kt` — `getRetryablePosts`
- `PostRepositoryImpl.kt` — `getPendingPhotos`
- `PostUploadQueue.kt` — `start`, `retry`, `discard`
- `WorkManagerPostUploadScheduler.kt` — nối lại job theo tag

## 11. Discard draft

User có thể bỏ draft PENDING/FAILED từ danh sách mở rộng của `PostUploadStatusBar`.

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

Queue xóa item đã hoàn tất khỏi thanh trạng thái rồi tiếp tục item kế tiếp. Home nhận post mới qua realtime listener; việc điều hướng đã xảy ra ngay từ lúc item được thêm vào queue.

## 13. Đồng bộ post và tải ảnh ở thiết bị nhận

`HomeRepositoryImpl.observePosts()` mở hai collection-group listener theo UID hiện tại:

- listener lấy 20 post ACTIVE mới nhất trên toàn bộ connection theo `createdAt`;
- listener riêng lấy tối đa 20 tombstone DELETED mới nhất theo `updatedAt`.

Hai query đều dùng `memberIds ARRAY_CONTAINS uid`. Khi người dùng tải thêm ở feed All, repository dùng một cursor toàn cục; khi đang lọc theo connection, query bổ sung `connectionId` và dùng cursor riêng cho connection đó.

Khi nhận post ACTIVE:

1. Parse document thành `PostEntity` và `MediaItemEntity`.
2. Upsert cả hai vào Room transaction.
3. Tải IMAGE từ Storage về file `.download` tạm.
4. Kiểm tra số byte đúng với metadata.
5. Rename file tạm thành file cache chính thức.
6. Ghi post remote vào Room với `localSyncStatus = SYNCED`, đọc feed từ Room rồi phát `PostFeedPage` cho ViewModel. Draft PENDING/FAILED tiếp tục nằm trong upload queue và không xuất hiện như bài đã publish.

Cache phía nhận dùng cùng cấu trúc:

```text
<app files>/pending_media/{connectionId}/{postId}/{mediaId}.jpg
```

Đọc tại `feature/home/data/.../HomeRepositoryImpl.kt` — `observePosts`, `syncDocumentMetadata`, `storePost`, `cacheMedia`.

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
- `feature/home/data/.../HomeRepositoryImpl.kt` — `deletePost`, `syncDocumentMetadata`
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
6. `PostUploadQueue` cùng `PostRepositoryImpl.getPendingPhotos()` và `discardPendingPhoto()`.
7. `HomeRepositoryImpl.observePosts()` và `cacheMedia()`.
8. `HomeRepositoryImpl.deletePost()` và `lifecycleService.softDeletePost()`.
9. `cleanupService.ts` và `cleanupCore.ts`.
10. Cuối cùng đọc `PostDao.kt`, `firestore.rules` và `storage.rules` để kiểm tra toàn bộ invariant.
