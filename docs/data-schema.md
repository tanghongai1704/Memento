# Schema v1 — nguồn tham chiếu

Firestore dùng Timestamp cho thời gian server; domain/Room dùng epoch milliseconds (`Long`, nullable khi pending). schemaVersion remote bằng 1, độc lập Room version 3.

## users/{uid}

`displayName: String`, `username: String`, `usernameNormalized: String`, `avatarPath: String?`, `bio: String?`, `createdAt: Timestamp`, `updatedAt: Timestamp`, `schemaVersion: Number`.
UID = Firebase Auth UID. Không lưu email, uid field, friendList hoặc connection snapshots. Username dài 2–30 ký tự, bắt đầu bằng chữ/số, chỉ gồm chữ Latin/số/`.`/`_`; `usernameNormalized` bằng lowercase Locale.ROOT và được Rules kiểm tra. Search exact `whereEqualTo(usernameNormalized, normalized).limit(20)`; lookup UID dùng document get. Không cam kết username unique. Bio tối đa 500, displayName tối đa 100 ký tự.

## Mã kết nối cố định

`userInviteCodes/{uid}` lưu `code`, `codeHash`, `createdAt`, `schemaVersion`. `inviteCodeLookup/{codeHash}` lưu `ownerUid`, `createdAt`, `schemaVersion`. Cả hai collection chỉ backend được đọc/ghi; raw code chỉ được Callable `getMyInviteCode` trả cho đúng user đang đăng nhập.

Code được normalize bằng trim, bỏ dấu `-`, uppercase và SHA-256 để tra lookup. Mỗi user có một code được tạo một lần sau khi profile sẵn sàng; tài khoản cũ được tạo lazy ở lần login/Profile đầu tiên. Code không expire, không revoke, không có `usedCount` và được dùng lại để nhiều user khác nhau kết nối với owner.

Redeem direct đọc lookup, hai profile và khóa unique trong một transaction. Backend chặn tự kết nối. Nếu cặp đã có direct ACTIVE, backend trả lại connectionId cũ để retry không tạo trùng. Nếu chưa có, transaction tạo connection Auto ID và hai member documents. `directKey` là SHA-256 của JSON array hai UID đã sort; document cùng ID trong `directConnectionLocks` bảo đảm uniqueness khi concurrent.

Disconnect DIRECT giữ member document, cập nhật hai membership thành LEFT và đồng thời xóa UID khỏi `memberIds`. GROUP chưa có flow triển khai; owner transfer, remove/rejoin và quyền xem lịch sử trước `joinedAt` phải được thiết kế trước khi bật.

## connections/{connectionId}

Auto ID; fields: `type: DIRECT | GROUP`, `name: String?`, `memberIds: List<UID>` (chỉ ACTIVE), `createdBy: UID`, `ownerId: UID?`, `maxMembers: Number`, `status: ACTIVE | CLOSED`, `directKey: String?`, `lastPostAt: Timestamp?`, `createdAt: Timestamp`, `updatedAt: Timestamp`, `schemaVersion: 1`. Group được tạo hoàn chỉnh trong một transaction nên connection không có trạng thái `PENDING`; nếu sau này cần chờ người được mời xác nhận thì trạng thái chờ thuộc member, không thuộc connection.

Phải lưu `lastPostAt: null` ngay cả khi chưa có post để document không thiếu field orderBy. Domain/Room dùng members làm dữ liệu membership; memberIds là chỉ mục remote, không cần sao chép JSON array vào Room.

Query: memberIds ARRAY_CONTAINS uid, status == ACTIVE, lastPostAt DESC. Composite index trong `firestore.indexes.json`.

### members/{uid}

`userId: UID`, `role: OWNER | ADMIN | MEMBER`, `status: ACTIVE | LEFT | REMOVED`, `joinedAt: Timestamp`, `leftAt: Timestamp?`, `invitedBy: UID?`, `removedBy: UID?`.
Member ID = UID; không dùng collection root `connection_members` (tên bảng Room vẫn hợp lệ).

## connections/{connectionId}/posts/{postId}

`connectionId`, `memberIds: List<UID>`, `authorId`, `postType: PHOTO | VIDEO`, `layoutType: SINGLE | GRID | COLLAGE | CAROUSEL`, `caption: String?`, `mediaItems: List<Map>`, `clientCreatedAt: Number`, `createdAt: Timestamp`, `updatedAt: Timestamp`, `status: ACTIVE | DELETED`, `deletedAt: Timestamp?`, `deletedBy: UID?`, `schemaVersion: 1`. Backend sao chép `memberIds` từ connection ACTIVE khi finalize; client không được tự đặt audience.

Media map: `mediaId`, `mediaType: IMAGE | VIDEO`, `storagePath`, `thumbnailPath: String?`, `mimeType`, `width`, `height`, `durationMs: Number?`, `sizeBytes`, `position` (0-based). Media metadata phản ánh file sau nén; video cần thumbnail. LocalMediaItem chỉ là picker model, không serialize vào Firestore.

Feed tổng dùng một collection-group query `memberIds ARRAY_CONTAINS uid`, `status == ACTIVE`, `createdAt DESC`, limit 20 và `startAfter(lastDocument)`. Firestore vì vậy sắp xếp và giới hạn trên toàn bộ connection trước khi trả dữ liệu, không mở 20 post cho từng connection. Khi lọc một connection, query bổ sung `connectionId == selectedId` và giữ cursor riêng. Một collection-group listener `DELETED` theo `updatedAt DESC` bảo đảm tombstone của bài cũ đã cache được đồng bộ. `memberIds` trên post là audience snapshot và cũng giúp Functions xử lý post mà không cần đọc lại connection.

Tạo postId/mediaId trước upload; giữ nguyên khi retry. Luồng đã triển khai: Room PENDING → xử lý JPEG → Storage → Callable `finalizePhotoPost` → Firestore transaction set Post + update connection.lastPostAt/updatedAt bằng cùng server timestamp → Room SYNCED. Backend đọc object thật, kiểm tra `contentType`, byte size và custom metadata `authorId`; post đã tồn tại chỉ được coi là retry thành công khi dữ liệu bất biến khớp, nên không reset createdAt. Firestore không atomic với Storage: retry dùng lại object và scheduled cleanup xử lý orphan sau grace period. Soft delete metadata trước cleanup Storage.

`softDeletePost(connectionId, postId)` chỉ cho member ACTIVE là tác giả gọi. Transaction đổi `status = DELETED`, ghi `deletedAt`, `deletedBy` và `updatedAt`; gọi lại cùng tác giả là idempotent. Client nghe thêm query `status == DELETED, updatedAt DESC, limit 20` để bài cũ đã tải cũng bị gỡ khỏi Room feed/cache dù không còn nằm trong trang 20 bài mới nhất. Composite index tương ứng có trong `firestore.indexes.json`.

`disconnectDirect(connectionId)` chỉ áp dụng cho DIRECT và một trong hai member ACTIVE. Transaction đổi connection sang `CLOSED`, xóa `memberIds`, chuyển cả hai member sang `LEFT` với `leftAt`, và đóng `directConnectionLocks/{directKey}`. Sau đó backend dọn `memberIds` khỏi post theo các batch 400 document trước khi trả success, để chúng không còn khớp collection-group feed query; retry tiếp tục cleanup an toàn. Rules thu hồi ngay scoped read của connection/post/media, app đổi membership local và xóa cache. Kết nối lại tạo connection ID mới nên lịch sử cũ không tái xuất hiện.

## Retry, offline và cleanup

Draft upload hợp lệ nằm trong Room với `localSyncStatus = PENDING | FAILED`, giữ nguyên `postId`, `mediaId`, metadata và file JPEG riêng của app. Sau process death, Create Post khôi phục draft mới nhất và đưa người dùng thẳng tới `Resume pending upload`; retry ghi lại cùng Storage path rồi gọi finalize idempotent. Draft thiếu media hoặc file sai byte size được dọn local vì không thể retry. Người dùng có thể xác nhận discard; object đã upload nhưng chưa finalize được backend cleanup sau thời gian chờ.

Splash chỉ dùng profile Room làm fallback khi Firebase báo lỗi mạng và Auth vẫn còn đúng UID; permission/data error không được che bằng cache. Feed Home tiếp tục được chặn bằng membership local của UID hiện tại.

Scheduled Function `cleanupExpiredMedia` chạy mỗi ngày lúc 03:00 `Asia/Ho_Chi_Minh`. Nó chỉ nhận path chính xác `connections/{connectionId}/posts/{postId}/{mediaId}.jpg`; object phải cũ ít nhất 7 ngày. Orphan không có Post được xóa, hoặc media của Post `DELETED` chỉ được xóa khi `deletedAt` cũng đã qua 7 ngày và `mediaItems.storagePath` khớp. Bài ACTIVE, path ngoài phạm vi, media mới và Post metadata luôn được giữ.

## Room

PostDao join posts/connections/membership theo current UID và chỉ trả post ACTIVE có `localSyncStatus = SYNCED`; draft PENDING/FAILED được đọc bằng query riêng cho upload queue. DAO hỗ trợ filter All/connection/My/Received/type/time và sắp xếp `COALESCE(createdAt, clientCreatedAt) DESC`. Lọc khoảng thời gian chỉ xét `createdAt`. Composite PK `(connectionId, postId)` tránh giả định postId unique toàn cục. Các index `posts(connectionId)`, `posts(authorId)`, `posts(createdAt)`, `posts(status,createdAt)`, `posts(connectionId,status,createdAt)`, `connections(status)` và `media_items(postId)` có trong entities.

## Chuyển dữ liệu remote cũ

Với dữ liệu legacy, export/backup trước khi dùng Admin migration: chuyển users sang schema chuẩn và bỏ email; ánh xạ connection ID cũ sang Auto ID mới; chuyển root `connection_members` vào subcollection; tạo `memberIds` từ ACTIVE members; bổ sung `maxMembers`, `ownerId`, `directKey`, `lastPostAt`, `schemaVersion`; đổi ARCHIVED thành CLOSED; đổi milliseconds sang Timestamp. Trước khi bật collection-group feed cho dữ liệu đã có, chạy `npm run backfill:post-members` trong `functions` để dry-run, kiểm tra số lượng rồi thêm `-- --apply`; script sao chép member ACTIVE vào post và để post của connection CLOSED có audience rỗng. Chỉ xóa collection cũ sau khi đối soát.

## Rules và triển khai

`firestore.rules` cho phép profile owner writes và connection/member/post reads đúng membership ACTIVE. Mã kết nối, lookup, lock, connection/member mutation và post write vẫn khóa client; Admin SDK trong Functions thực hiện redeem, finalize post, soft delete và disconnect. `storage.rules` cho member ACTIVE đọc, giới hạn upload JPEG đúng path ≤ 5 MiB và chỉ uploader được retry object của mình; connection CLOSED hoặc member LEFT không còn quyền đọc/ghi. `firebase.json` quản lý Rules, index, Functions và Emulator, gồm Storage Emulator. Xem [trạng thái hiện tại](project-status.md) và [môi trường Firebase](firebase-environment.md).

Rules không lọc dữ liệu sau query; điều kiện query phải phù hợp quyền đọc. Tham khảo [Firebase query rules](https://firebase.google.com/docs/firestore/security/rules-query) và [transaction](https://firebase.google.com/docs/firestore/manage-data/transactions).
