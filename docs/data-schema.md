# Schema v1 — nguồn tham chiếu

Firestore dùng Timestamp cho thời gian server; domain/Room dùng epoch milliseconds (`Long`, nullable khi pending). schemaVersion remote bằng 1, độc lập Room version 3.

## users/{uid}

`displayName: String`, `username: String`, `usernameNormalized: String`, `avatarPath: String?`, `bio: String?`, `createdAt: Timestamp`, `updatedAt: Timestamp`, `schemaVersion: Number`.
UID = Firebase Auth UID. Không lưu email, uid field, friendList hoặc connection snapshots. Chuẩn hóa username bỏ toàn bộ whitespace + lowercase Locale.ROOT (chặt hơn ví dụ trim trong thảo luận). Search exact `whereEqualTo(usernameNormalized, normalized).limit(20)`; lookup UID dùng document get. Không cam kết username unique. Bio tối đa 500, tên/username tối đa 100 ký tự.

## invites/{codeHash}

`purpose: DIRECT_PAIR | GROUP_JOIN`, `createdBy: UID`, `targetConnectionId: String?`, `maxUses: Number`, `usedCount: Number`, `status: ACTIVE | USED | EXPIRED | REVOKED`, `createdAt: Timestamp`, `expiresAt: Timestamp`, `revokedAt: Timestamp?`, `schemaVersion: 1`.

Normalize code: trim, bỏ dấu '-', uppercase Locale.ROOT; SHA-256 làm document ID. Không lưu raw code và không list invites. Model có sẵn; chưa có generator/redeemer/UI.

Redeem tương lai phải đọc tất cả document cần thiết trước khi write trong cùng transaction: invite; connection đích; membership; khóa unique direct nếu áp dụng. Kiểm tra ACTIVE, thời gian server trước expiresAt, usedCount < maxUses, không tự redeem, chưa active, connection GROUP đúng loại/ACTIVE/chưa đầy. Direct maxUses = 1. Group có maxUses/expiresAt/maxMembers bắt buộc.

Tạo direct dùng Auto ID được sinh ngoài callback retry, lưu hai member document và memberIds cùng transaction; directKey là định danh cặp được tạo ổn định từ hai UID đã sort (nên hash encoding có phân cách rõ). Query directKey rồi tạo Auto ID không tự bảo đảm uniqueness khi concurrent: cần backend/lock document riêng được thiết kế ở bước invite. Không dùng local duplicate check làm ràng buộc.

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

Tạo postId/mediaId trước upload; giữ nguyên khi retry. Room PENDING → nén → Storage → batch set post + update connection.lastPostAt/updatedAt bằng server timestamp → Room SYNCED. Retry đã commit không được reset createdAt; cần kiểm tra postId/idempotency. Firestore không atomic với Storage: cần retry và orphan cleanup, không publish metadata trước file. Soft delete metadata trước cleanup Storage. Chưa triển khai pipeline này trong code.

## Room

PostDao join posts/connections/membership theo current UID; filter All/connection/My/Received/type/time. Sắp xếp COALESCE(createdAt, clientCreatedAt) DESC. Lọc khoảng thời gian chỉ xét createdAt, như schema thảo luận. Composite PK (connectionId, postId) tránh giả định postId unique toàn cục. Các index posts(connectionId), posts(authorId), posts(createdAt), posts(status,createdAt), posts(connectionId,status,createdAt), connections(status), media_items(postId) có trong entities.

## Chuyển dữ liệu remote cũ

Bước 1 ngày 13/09/2026 đã migrate 3 profile legacy trên `memento-fre`, giữ UID/username/createdAt và có backup local. Không có connection/request để migrate tại thời điểm kiểm kê; không xóa tài khoản Auth hoặc collection.

Với dữ liệu legacy còn gặp ở môi trường khác, export/backup trước khi dùng Admin migration: chuyển users sang 9 field chuẩn (bỏ email); ánh xạ connection ID cũ sang Auto ID mới; chuyển root connection_members vào subcollection; tạo memberIds từ ACTIVE members; bổ sung maxMembers/ownerId/directKey/lastPostAt/schemaVersion; đổi ARCHIVED thành CLOSED; đổi milliseconds sang Timestamp. Chỉ xóa connection_requests và collection cũ sau khi đối soát xong. User legacy tự được sửa khi chính user đăng nhập, nhưng không thay thế migration toàn bộ dữ liệu trước deploy.

## Rules và triển khai

`firestore.rules` cho phép profile owner writes và connection/member reads đúng quyền. Invite/post/connection mutations bị khóa đến bước triển khai transaction có kiểm chứng; không nới quyền toàn collection để làm demo. `firebase.json` trỏ rules/index; đã deploy lên `memento-fre` ngày 13/09/2026 và xác nhận 2 indexes READY. Đã kiểm tra 16 trường hợp Rules, 12 kiểm tra Auth/Profile Emulator ở bước 1; build debug, unit test username và 5 kiểm tra SQLite migration/query đạt ở đợt nền trước. Khi triển khai invite phải bổ sung test concurrent redeem và cập nhật quyền phù hợp. Xem [tiến độ](mvp-progress.md) và [quy tắc MVP](mvp-baseline.md).

Rules không lọc dữ liệu sau query; điều kiện query phải phù hợp quyền đọc. Tham khảo [Firebase query rules](https://firebase.google.com/docs/firestore/security/rules-query) và [transaction](https://firebase.google.com/docs/firestore/manage-data/transactions).
