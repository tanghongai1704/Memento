# Luồng sản phẩm

MVP đầu ưu tiên DIRECT + một ảnh. Phạm vi, giới hạn và quyền lịch sử đã chốt ở [mvp-baseline](mvp-baseline.md); theo dõi triển khai ở [mvp-progress](mvp-progress.md).

Đăng ký/đăng nhập → profile theo UID → Home. Người dùng chọn All hoặc một connection ACTIVE; bài viết thuộc connection, không gửi trực tiếp tới userId.

DIRECT có hai người và không chuyển thành GROUP. GROUP có owner và giới hạn thành viên. Tạo người thứ ba trong cuộc trò chuyện 1-1 phải tạo group mới để không lộ lịch sử direct.

Ghép đôi direct dùng mã 8 ký tự có hiệu lực 10 phút và dùng một lần. A tạo/chia sẻ code; B nhập code; Callable transaction tạo connection và hai membership. Tạo code mới tự revoke code cũ; A có thể revoke thủ công. Backend chặn tự redeem, cặp direct trùng và quá 5 lượt thử trong 10 phút. GROUP invite chưa triển khai.

Create Post hiển thị người còn lại trong DIRECT dưới dạng `displayName (@username)` nhưng lưu lựa chọn bằng connectionId. Người dùng chọn một connection ACTIVE, chọn đúng một ảnh và caption tùy chọn. App lưu PENDING trong Room, sửa hướng/resize/nén JPEG, upload Storage, gọi backend transaction tạo Post + cập nhật lastPostAt rồi chuyển local sang SYNCED. Nếu lỗi, bài chuyển FAILED và được khôi phục để retry cùng ID sau khi mở lại app. Home người gửi đọc bài và file local từ Room/cache. Post không hết hạn theo invite.

Người nhận chưa tự đồng bộ hoặc tải ảnh remote trong bước 4. Bước 5 bổ sung listener, pagination 20 bài/trang/connection và cache Storage để U2 nhìn thấy bài U1 đã đăng.

History hỗ trợ thiết kế My Posts/Received, PHOTO/VIDEO và khoảng thời gian; hiện DAO có bộ lọc, UI chưa đầy đủ. Xem [schema](data-schema.md) để triển khai tiếp.
