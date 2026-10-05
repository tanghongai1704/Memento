# Firebase Auth và user profile

UID Firebase Auth là document ID duy nhất: `users/{uid}`. Email/password thuộc Firebase Auth và không nằm trong public profile.

## Đăng ký, đăng nhập và offline fallback

1. Firebase Auth tạo tài khoản hoặc đăng nhập bằng email đã trim; password được giữ nguyên.
2. `AuthRepositoryImpl.syncCurrentUserProfile()` đọc profile theo UID trong transaction. Nếu chưa có, repository tạo đủ schema; profile legacy của chính user được chuẩn hóa.
3. Profile được cache vào Room và backend bảo đảm mã kết nối cố định đã tồn tại.
4. Login chỉ báo thành công sau khi đồng bộ xong. Nếu Firebase lỗi mạng nhưng session vẫn thuộc đúng UID và Room đã có profile, app có thể dùng cache; lỗi quyền hoặc dữ liệu sai không được che bằng fallback.
5. Nếu đăng ký Auth thành công nhưng setup profile thất bại, app sign out để tránh session nửa hoàn chỉnh. Auth account không bị tự động xóa.

Splash kiểm tra Firebase session. Dữ liệu Home/Profile sau đó đi qua repository và Room, nên phiên hợp lệ có thể mở app với cache khi offline.

## Profile

`syncCurrentUserProfile` chỉ đọc document của chính user; không tải toàn bộ `users`. Profile screen đọc remote, cập nhật Room, cho sửa `displayName`, `username`, `bio` và chỉ báo thành công sau transaction Firestore.

- `displayName`: 1–100 ký tự sau trim.
- `username`: 2–30 ký tự, bắt đầu bằng chữ/số, chỉ gồm chữ Latin, số, dấu chấm hoặc gạch dưới.
- `usernameNormalized`: lowercase theo `Locale.ROOT`; Rules yêu cầu khớp `username.lower()`.
- `bio`: nullable, tối đa 500 ký tự.
- `avatarPath`: có trong schema nhưng app chưa có flow upload avatar.

Username không unique; direct connection dùng invite code. Rules cho user đã đăng nhập đọc profile để dựng nhãn thành viên, nhưng chỉ chủ UID được tạo/cập nhật document của mình.

## Quên mật khẩu

Forgot Password validate email rồi gọi Firebase Auth `sendPasswordResetEmail`. UI dùng thông báo chung sau khi gửi và không xác nhận email có tài khoản hay không.

## Kiểm tra trước phát hành

Tự động hóa hiện có kiểm tra profile create/read, login lại không reset profile, password reset request, quyền sửa profile và truy cập không đăng nhập. Trước phát hành vẫn cần kiểm tra email reset thật, UI trên thiết bị, session hết hạn, offline sau Auth và đổi tài khoản không lẫn cache.
