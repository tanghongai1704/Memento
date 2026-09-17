# Schema v1 — nguồn tham chiếu

Firestore dùng Timestamp cho thời gian server; domain/Room dùng epoch milliseconds (`Long`, nullable khi pending). schemaVersion remote bằng 1, độc lập Room version 3.

## users/{uid}

`displayName: String`, `username: String`, `usernameNormalized: String`, `avatarPath: String?`, `bio: String?`, `createdAt: Timestamp`, `updatedAt: Timestamp`, `schemaVersion: Number`.
UID = Firebase Auth UID. Không lưu email, uid field, friendList hoặc connection snapshots. Username dài 2–30 ký tự, bắt đầu bằng chữ/số, chỉ gồm chữ Latin/số/`.`/`_`; `usernameNormalized` bằng lowercase Locale.ROOT và được Rules kiểm tra. Search exact `whereEqualTo(usernameNormalized, normalized).limit(20)`; lookup UID dùng document get. Không cam kết username unique. Bio tối đa 500, displayName tối đa 100 ký tự.

## Mã kết nối cố định

`userInviteCodes/{uid}` lưu `code`, `codeHash`, `createdAt`, `schemaVersion`. `inviteCodeLookup/{codeHash}` lưu `ownerUid`, `createdAt`, `schemaVersion`. Cả hai collection chỉ backend được đọc/ghi; raw code chỉ được Callable `getMyInviteCode` trả cho đúng user đang đăng nhập.

Code được normalize bằng trim, bỏ dấu `-`, uppercase và SHA-256 để tra lookup. Mỗi user có một code được tạo một lần sau khi profile sẵn sàng; tài khoản cũ được tạo lazy ở lần login/Profile đầu tiên. Code không expire, không revoke, không có `usedCount` và được dùng lại để nhiều user khác nhau kết nối với owner.

Redeem direct đọc lookup, hai profile và khóa unique trong một transaction. Backend chặn tự kết nối. Nếu cặp đã có direct ACTIVE, backend trả lại connectionId cũ để retry không tạo trùng. Nếu chưa có, transaction tạo connection Auto ID và hai member documents. `directKey` là SHA-256 của JSON array hai UID đã sort; document cùng ID trong `directConnectionLocks` bảo đảm uniqueness khi concurrent.

Leave/remove giữ member document, cập nhật LEFT/REMOVED và audit đồng thời với memberIds. Owner phải chuyển quyền hoặc đóng group trước khi rời. Rejoin và quyền xem bài trước joinedAt cần chốt trước triển khai; hiện chưa expose posts remote.

## connections/{connectionId}

Auto ID; fields: `type: DIRECT | GROUP`, `name: String?`, `memberIds: List<UID>` (chỉ ACTIVE), `createdBy: UID`, `ownerId: UID?`, `maxMembers: Number`, `status: ACTIVE | CLOSED`, `directKey: String?`, `lastPostAt: Timestamp?`, `createdAt: Timestamp`, `updatedAt: Timestamp`, `schemaVersion: 1`. Group được tạo hoàn chỉnh trong một transaction nên connection không có trạng thái `PENDING`; nếu sau này cần chờ người được mời xác nhận thì trạng thái chờ thuộc member, không thuộc connection.

Phải lưu `lastPostAt: null` ngay cả khi chưa có post để document không thiếu field orderBy. Domain/Room dùng members làm dữ liệu membership; memberIds là chỉ mục remote, không cần sao chép JSON array vào Room.

Query: memberIds ARRAY_CONTAINS uid, status == ACTIVE, lastPostAt DESC. Composite index trong `firestore.indexes.json`.

### members/{uid}

`userId: UID`, `role: OWNER | ADMIN | MEMBER`, `status: ACTIVE | LEFT | REMOVED`, `joinedAt: Timestamp`, `leftAt: Timestamp?`, `invitedBy: UID?`, `removedBy: UID?`.
Member ID = UID; không dùng collection root `connection_members` (tên bảng Room vẫn hợp lệ).

## connections/{connectionId}/posts/{postId}

`connectionId`, `authorId`, `postType: PHOTO | VIDEO`, `layoutType: SINGLE | GRID | COLLAGE | CAROUSEL`, `caption: String?`, `mediaItems: List<Map>`, `clientCreatedAt: Number`, `createdAt: Timestamp`, `updatedAt: Timestamp`, `status: ACTIVE | DELETED`, `deletedAt: Timestamp?`, `deletedBy: UID?`, `schemaVersion: 1`.

Media map: `mediaId`, `mediaType: IMAGE | VIDEO`, `storagePath`, `thumbnailPath: String?`, `mimeType`, `width`, `height`, `durationMs: Number?`, `sizeBytes`, `position` (0-based). Media metadata phản ánh file sau nén; video cần thumbnail. LocalMediaItem chỉ là picker model, không serialize vào Firestore.

Pagination đã triển khai theo từng connection: status ACTIVE, createdAt DESC, limit 20, startAfter(lastDocument). Mỗi connection giữ cursor và trạng thái còn trang riêng; listener realtime chỉ giữ 20 bài mới nhất nhưng các trang cũ đã upsert vào Room vẫn được giữ để tạo feed chung. Index queryScope COLLECTION dùng cho query posts dưới một connection; không nhầm với COLLECTION_GROUP query toàn bộ posts. Trang 20 không giới hạn tổng lịch sử.

Tạo postId/mediaId trước upload; giữ nguyên khi retry. Luồng đã triển khai: Room PENDING → xử lý JPEG → Storage → Callable `finalizePhotoPost` → Firestore transaction set Post + update connection.lastPostAt/updatedAt bằng cùng server timestamp → Room SYNCED. Backend đọc object thật, kiểm tra `contentType`, byte size và custom metadata `authorId`; post đã tồn tại chỉ được coi là retry thành công khi dữ liệu bất biến khớp, nên không reset createdAt. Firestore không atomic với Storage: retry dùng lại object, còn orphan cleanup thuộc bước 7. Soft delete metadata trước cleanup Storage.

`softDeletePost(connectionId, postId)` chỉ cho member ACTIVE là tác giả gọi. Transaction đổi `status = DELETED`, ghi `deletedAt`, `deletedBy` và `updatedAt`; gọi lại cùng tác giả là idempotent. Client nghe thêm query `status == DELETED, updatedAt DESC, limit 20` để bài cũ đã tải cũng bị gỡ khỏi Room feed/cache dù không còn nằm trong trang 20 bài mới nhất. Composite index tương ứng có trong `firestore.indexes.json`.

`disconnectDirect(connectionId)` chỉ áp dụng cho DIRECT và một trong hai member ACTIVE. Cùng transaction đổi connection sang `CLOSED`, xóa `memberIds`, chuyển cả hai member sang `LEFT` với `leftAt`, và đóng `directConnectionLocks/{directKey}`. Rules vì vậy thu hồi ngay quyền đọc connection/post/media. App cũng đổi membership local và xóa thư mục cache của connection; document lịch sử và Storage object vẫn được giữ cho chính sách cleanup bước 7. Kết nối lại tạo connection ID mới nên lịch sử cũ không tái xuất hiện.

## Room

PostDao join posts/connections/membership theo current UID; filter All/connection/My/Received/type/time. Sắp xếp COALESCE(createdAt, clientCreatedAt) DESC. Lọc khoảng thời gian chỉ xét createdAt, như schema thảo luận. Composite PK (connectionId, postId) tránh giả định postId unique toàn cục. Các index posts(connectionId), posts(authorId), posts(createdAt), posts(status,createdAt), posts(connectionId,status,createdAt), connections(status), media_items(postId) có trong entities.

## Chuyển dữ liệu remote cũ

Bước 1 ngày 13/09/2026 đã migrate 3 profile legacy trên `memento-fre`, giữ UID/username/createdAt và có backup local. Không có connection/request để migrate tại thời điểm kiểm kê; không xóa tài khoản Auth hoặc collection.

Với dữ liệu legacy còn gặp ở môi trường khác, export/backup trước khi dùng Admin migration: chuyển users sang 9 field chuẩn (bỏ email); ánh xạ connection ID cũ sang Auto ID mới; chuyển root connection_members vào subcollection; tạo memberIds từ ACTIVE members; bổ sung maxMembers/ownerId/directKey/lastPostAt/schemaVersion; đổi ARCHIVED thành CLOSED; đổi milliseconds sang Timestamp. Chỉ xóa connection_requests và collection cũ sau khi đối soát xong. User legacy tự được sửa khi chính user đăng nhập, nhưng không thay thế migration toàn bộ dữ liệu trước deploy.

## Rules và triển khai

`firestore.rules` cho phép profile owner writes và connection/member/Post reads đúng membership ACTIVE. Mã kết nối, lookup, lock, connection/member mutation và Post write vẫn khóa client; Admin SDK trong Functions thực hiện redeem, finalize post, soft delete và disconnect. `storage.rules` cho member ACTIVE đọc, giới hạn upload JPEG đúng path ≤ 5 MiB và chỉ uploader được retry object của mình; connection CLOSED hoặc member LEFT không còn quyền đọc/ghi. `firebase.json` quản lý Rules/index/Functions và Emulator, gồm Storage Emulator. Ba indexes đã deploy. Xem [tiến độ](mvp-progress.md) và [quy tắc MVP](mvp-baseline.md).

Rules không lọc dữ liệu sau query; điều kiện query phải phù hợp quyền đọc. Tham khảo [Firebase query rules](https://firebase.google.com/docs/firestore/security/rules-query) và [transaction](https://firebase.google.com/docs/firestore/manage-data/transactions).
