# Firebase Auth và user profile

UID Firebase Auth là document ID duy nhất: `users/{uid}`. Email/password thuộc Auth, không thuộc public profile.

Form đăng ký nhận email/password. Username mặc định được lọc từ phần trước `@`; nếu không còn ít nhất 2 ký tự hợp lệ thì dùng `user` + một phần UID. DisplayName mặc định bằng username. Schema cho phép username trùng; invite mới là cơ chế ghép đôi.

## Đăng ký và đăng nhập

1. Firebase Auth tạo tài khoản hoặc đăng nhập bằng email đã trim; giữ nguyên password.
2. Transaction đọc trực tiếp profile UID. Nếu chưa có, tạo đủ 9 field schema, timestamps dùng server timestamp.
3. Profile legacy của chính user được chuẩn hóa và bỏ field ngoài schema; giữ createdAt và tên/avatar/bio đang có. Profile đúng schema không bị ghi lại mỗi lần đăng nhập.
4. Đọc profile về Room. Login chỉ báo thành công khi bước này thành công. Splash cũng kiểm tra profile trước khi vào Home; hiện cần mạng cho bước này, chưa hỗ trợ khởi động offline có session.
5. Nếu đăng ký Auth thành công nhưng profile lỗi, sign out và báo user đăng nhập lại để thử bước profile. Không tạo tài khoản thứ hai, không xóa Auth account đã tạo.

`syncCurrentUserProfile` chỉ đọc chính user; không tải cả collection users. Profile screen đọc document hiện tại, cache Room, cho sửa displayName/username/bio và chỉ báo thành công sau khi Firestore transaction hoàn tất. Email chỉ đọc từ Firebase Auth.

Username dài 2–30 ký tự, bắt đầu bằng chữ/số và chỉ gồm chữ Latin, số, dấu chấm hoặc gạch dưới. `usernameNormalized` là lowercase Locale.ROOT và Rules bắt buộc nó bằng `username.lower()`. DisplayName dài 1–100 ký tự sau trim; bio tối đa 500 ký tự.

Rules chỉ cho chủ UID tạo/cập nhật profile. Bước 1 đã migrate cả 3 profile legacy hiện có trên `memento-fre`; email không còn trong các profile này. Với môi trường khác, email legacy cần được migrate bằng Admin trước khi bật đọc profile cho người khác. Rules không che được field nhạy cảm của một document.

## Quên mật khẩu

Forgot Password validate email rồi gọi Firebase Auth `sendPasswordResetEmail`. UI luôn dùng thông báo sau gửi mang tính chung, không khẳng định email có tài khoản. Bước 2 đã kiểm tra request reset trên Auth Emulator; không tự động gửi mail tới tài khoản Firebase thật.

## Kiểm tra trên Firebase/emulator trước phát hành

Tự động hiện có: build Android, unit test validation, hai tài khoản Auth Emulator, profile create/read, login lại không reset profile, password reset request, tài khoản B không sửa profile A và truy cập không đăng nhập bị chặn.

Còn test thủ công trước MVP: email reset thật đến inbox/spam, UI trên thiết bị, ngắt mạng sau Auth thành công, logout/login tài khoản khác không lẫn cache và hành vi khi session hết hạn. Offline Splash được xử lý ở bước 7.
