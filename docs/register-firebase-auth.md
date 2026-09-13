# Firebase Auth và user profile

UID Firebase Auth là document ID duy nhất: `users/{uid}`. Email/password thuộc Auth, không thuộc public profile.

Form hiện nhận email/password; username mặc định là phần trước @ (fallback UID), displayName mặc định bằng username. Đây không phải cơ chế bảo đảm username duy nhất; schema cho phép nhiều kết quả search. Màn hình đổi tên/profile chưa triển khai.

## Đăng ký và đăng nhập

1. Firebase Auth tạo tài khoản hoặc đăng nhập bằng email đã trim; giữ nguyên password.
2. Transaction đọc trực tiếp profile UID. Nếu chưa có, tạo đủ 9 field schema, timestamps dùng server timestamp.
3. Profile legacy của chính user được chuẩn hóa và bỏ field ngoài schema; giữ createdAt và tên/avatar/bio đang có. Profile đúng schema không bị ghi lại mỗi lần đăng nhập.
4. Đọc profile về Room. Login chỉ báo thành công khi bước này thành công. Splash cũng kiểm tra profile trước khi vào Home; hiện cần mạng cho bước này, chưa hỗ trợ khởi động offline có session.
5. Nếu đăng ký Auth thành công nhưng profile lỗi, sign out và báo user đăng nhập lại để thử bước profile. Không tạo tài khoản thứ hai, không xóa Auth account đã tạo.

`syncCurrentUserProfile` chỉ đọc chính user; không tải cả collection users. Username được bỏ toàn bộ whitespace và lowercase bằng Locale.ROOT; search dùng cùng hàm, không tìm theo email/UID hay contains.

Rules chỉ cho chủ UID tạo/cập nhật profile, kiểm tra kiểu và giới hạn displayName/username 100 ký tự, bio 500 ký tự. Bước 1 đã migrate cả 3 profile legacy hiện có trên `memento-fre`; email không còn trong các profile này. Với môi trường khác, email legacy cần được migrate bằng Admin trước khi bật đọc profile cho người khác. Rules không che được field nhạy cảm của một document.

## Kiểm tra trên Firebase/emulator trước phát hành

Đăng ký mới; đăng nhập tài khoản cũ; ngắt mạng sau Auth thành công rồi đăng nhập lại; profile đã có không bị reset; timestamps là Timestamp; tài khoản B không sửa profile A; search khác hoa/thường/khoảng trắng; logout/login tài khoản khác không thấy feed của tài khoản trước.
