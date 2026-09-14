# Luồng sản phẩm

MVP đầu ưu tiên DIRECT + một ảnh. Phạm vi, giới hạn và quyền lịch sử đã chốt ở [mvp-baseline](mvp-baseline.md); theo dõi triển khai ở [mvp-progress](mvp-progress.md).

Đăng ký/đăng nhập → profile theo UID → Home. Người dùng chọn All hoặc một connection ACTIVE; bài viết thuộc connection, không gửi trực tiếp tới userId.

DIRECT có hai người và không chuyển thành GROUP. GROUP có owner và giới hạn thành viên. Tạo người thứ ba trong cuộc trò chuyện 1-1 phải tạo group mới để không lộ lịch sử direct.

Ghép đôi direct dùng mã 8 ký tự có hiệu lực 10 phút và dùng một lần. A tạo/chia sẻ code; B nhập code; Callable transaction tạo connection và hai membership. Tạo code mới tự revoke code cũ; A có thể revoke thủ công. Backend chặn tự redeem, cặp direct trùng và quá 5 lượt thử trong 10 phút. GROUP invite chưa triển khai.

Create Post chọn connection, chọn/nén media; đăng bài thật chưa khả dụng. Pipeline dự kiến: lưu PENDING trong Room → upload Storage → batch post + lastPostAt → SYNCED. Home/History đọc Room. Post không hết hạn theo invite.

History hỗ trợ thiết kế My Posts/Received, PHOTO/VIDEO và khoảng thời gian; hiện DAO có bộ lọc, UI chưa đầy đủ. Xem [schema](data-schema.md) để triển khai tiếp.
