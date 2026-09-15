# Schema v1 — nguồn tham chiếu

Firestore dùng Timestamp cho thời gian server; domain/Room dùng epoch milliseconds (`Long`, nullable khi pending). schemaVersion remote bằng 1, độc lập Room version 3.

## users/{uid}

`displayName: String`, `username: String`, `usernameNormalized: String`, `avatarPath: String?`, `bio: String?`, `createdAt: Timestamp`, `updatedAt: Timestamp`, `schemaVersion: Number`.
UID = Firebase Auth UID. Không lưu email, uid field, friendList hoặc connection snapshots. Username dài 2–30 ký tự, bắt đầu bằng chữ/số, chỉ gồm chữ Latin/số/`.`/`_`; `usernameNormalized` bằng lowercase Locale.ROOT và được Rules kiểm tra. Search exact `whereEqualTo(usernameNormalized, normalized).limit(20)`; lookup UID dùng document get. Không cam kết username unique. Bio tối đa 500, displayName tối đa 100 ký tự.

## invites/{codeHash}

`purpose: DIRECT_PAIR | GROUP_JOIN`, `createdBy: UID`, `targetConnectionId: String?`, `maxUses: Number`, `usedCount: Number`, `status: ACTIVE | USED | EXPIRED | REVOKED`, `createdAt: Timestamp`, `expiresAt: Timestamp`, `revokedAt: Timestamp?`, `schemaVersion: 1`.

Normalize code: trim, bỏ dấu '-', uppercase; SHA-256 làm document ID. Không lưu raw code và client không được đọc/list invites. Bước 3 đã triển khai generator/redeemer/revoke trong Callable Functions và UI tạo, nhập, chia sẻ mã.

Redeem direct đọc dữ liệu cần thiết trước khi write trong cùng transaction: rate limit, invite, hai profile và khóa unique. Backend kiểm tra ACTIVE, thời gian server trước expiresAt, usedCount < maxUses, không tự redeem và chưa có direct ACTIVE. Direct maxUses = 1. Group vẫn chưa triển khai; khi làm phải kiểm tra connection đúng loại/ACTIVE/chưa đầy.

Tạo direct dùng Auto ID được sinh ngoài callback retry, lưu connection, hai member document, invite và lock trong cùng transaction. `directKey` là SHA-256 của JSON array hai UID đã sort; document cùng ID trong `directConnectionLocks` bảo đảm uniqueness khi concurrent. `directInviteOwners/{uid}` giữ con trỏ invite hiện tại để tạo mã mới revoke mã cũ. `inviteRedeemRateLimits/{uid}` giới hạn 5 lượt thử trong 10 phút. Ba collection này là dữ liệu backend và Rules cấm toàn bộ client access.

Group join cập nhật memberIds, member document, usedCount; USED khi đạt maxUses. Leave/remove giữ member document, cập nhật LEFT/REMOVED và audit đồng thời với memberIds. Owner phải chuyển quyền hoặc đóng group trước khi rời. Rejoin và quyền xem bài trước joinedAt cần chốt trước triển khai; hiện chưa expose posts remote.

## connections/{connectionId}

Auto ID; fields: `type: DIRECT | GROUP`, `name: String?`, `memberIds: List<UID>` (chỉ ACTIVE), `createdBy: UID`, `ownerId: UID?`, `maxMembers: Number`, `status: PENDING | ACTIVE | CLOSED`, `directKey: String?`, `lastPostAt: Timestamp?`, `createdAt: Timestamp`, `updatedAt: Timestamp`, `schemaVersion: 1`.

Phải lưu `lastPostAt: null` ngay cả khi chưa có post để document không thiếu field orderBy. Domain/Room dùng members làm dữ liệu membership; memberIds là chỉ mục remote, không cần sao chép JSON array vào Room.

Query: memberIds ARRAY_CONTAINS uid, status == ACTIVE, lastPostAt DESC. Composite index trong `firestore.indexes.json`.

### members/{uid}

`userId: UID`, `role: OWNER | ADMIN | MEMBER`, `status: ACTIVE | LEFT | REMOVED`, `joinedAt: Timestamp`, `leftAt: Timestamp?`, `invitedBy: UID?`, `removedBy: UID?`.
Member ID = UID; không dùng collection root `connection_members` (tên bảng Room vẫn hợp lệ).

## connections/{connectionId}/posts/{postId}

`connectionId`, `authorId`, `postType: PHOTO | VIDEO`, `layoutType: SINGLE | GRID | COLLAGE | CAROUSEL`, `caption: String?`, `mediaItems: List<Map>`, `clientCreatedAt: Number`, `createdAt: Timestamp`, `updatedAt: Timestamp`, `status: ACTIVE | DELETED`, `deletedAt: Timestamp?`, `deletedBy: UID?`, `schemaVersion: 1`.

Media map: `mediaId`, `mediaType: IMAGE | VIDEO`, `storagePath`, `thumbnailPath: String?`, `mimeType`, `width`, `height`, `durationMs: Number?`, `sizeBytes`, `position` (0-based). Media metadata phản ánh file sau nén; video cần thumbnail. LocalMediaItem chỉ là picker model, không serialize vào Firestore.

Pagination dự kiến theo từng connection: status ACTIVE, createdAt DESC, limit 20, startAfter(lastDocument). Index queryScope COLLECTION cho query posts dưới một connection; không nhầm với COLLECTION_GROUP query toàn bộ posts. Trang 20 không giới hạn tổng lịch sử.

Tạo postId/mediaId trước upload; giữ nguyên khi retry. Luồng đã triển khai: Room PENDING → xử lý JPEG → Storage → Callable `finalizePhotoPost` → Firestore transaction set Post + update connection.lastPostAt/updatedAt bằng cùng server timestamp → Room SYNCED. Backend đọc object thật, kiểm tra `contentType`, byte size và custom metadata `authorId`; post đã tồn tại chỉ được coi là retry thành công khi dữ liệu bất biến khớp, nên không reset createdAt. Firestore không atomic với Storage: retry dùng lại object, còn orphan cleanup thuộc bước 7. Soft delete metadata trước cleanup Storage.

## Room

PostDao join posts/connections/membership theo current UID; filter All/connection/My/Received/type/time. Sắp xếp COALESCE(createdAt, clientCreatedAt) DESC. Lọc khoảng thời gian chỉ xét createdAt, như schema thảo luận. Composite PK (connectionId, postId) tránh giả định postId unique toàn cục. Các index posts(connectionId), posts(authorId), posts(createdAt), posts(status,createdAt), posts(connectionId,status,createdAt), connections(status), media_items(postId) có trong entities.

## Chuyển dữ liệu remote cũ

Bước 1 ngày 13/09/2026 đã migrate 3 profile legacy trên `memento-fre`, giữ UID/username/createdAt và có backup local. Không có connection/request để migrate tại thời điểm kiểm kê; không xóa tài khoản Auth hoặc collection.

Với dữ liệu legacy còn gặp ở môi trường khác, export/backup trước khi dùng Admin migration: chuyển users sang 9 field chuẩn (bỏ email); ánh xạ connection ID cũ sang Auto ID mới; chuyển root connection_members vào subcollection; tạo memberIds từ ACTIVE members; bổ sung maxMembers/ownerId/directKey/lastPostAt/schemaVersion; đổi ARCHIVED thành CLOSED; đổi milliseconds sang Timestamp. Chỉ xóa connection_requests và collection cũ sau khi đối soát xong. User legacy tự được sửa khi chính user đăng nhập, nhưng không thay thế migration toàn bộ dữ liệu trước deploy.

## Rules và triển khai

`firestore.rules` cho phép profile owner writes và connection/member reads đúng quyền. Invite, lock, rate-limit, connection/member mutation và Post vẫn khóa client write; Admin SDK trong Functions thực hiện direct invite và finalize post. `storage.rules` cho member ACTIVE đọc, giới hạn upload JPEG đúng path ≤ 5 MiB và chỉ uploader được retry object của mình. `firebase.json` quản lý Rules/index/Functions và Emulator, gồm Storage Emulator. Ba Callable invite đã deploy ngày 13/09/2026; `finalizePhotoPost` và Storage Rules đã deploy ngày 14/09/2026; hai indexes READY. Xem [tiến độ](mvp-progress.md) và [quy tắc MVP](mvp-baseline.md).

Rules không lọc dữ liệu sau query; điều kiện query phải phù hợp quyền đọc. Tham khảo [Firebase query rules](https://firebase.google.com/docs/firestore/security/rules-query) và [transaction](https://firebase.google.com/docs/firestore/manage-data/transactions).
