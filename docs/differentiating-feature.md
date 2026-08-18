# Moment Recap

## Tổng quan
- **Moment Recap** tự động tổng hợp ảnh/video giữa 2 người dùng theo **tuần** hoặc **tháng** thành một collage dạng **carousel** để xem lại.
- Trải nghiệm này đóng vai trò như một **“trang nhật ký” riêng** của từng cặp đôi trong app Android.

## Lý do lựa chọn (so sánh với Locket/BeReal/Instagram)
- Khảo sát cho thấy **Locket, BeReal, Instagram** đều tối ưu cho **khoảnh khắc đơn lẻ**:
  - Locket: gửi liên tục lên widget.
  - BeReal: chụp đồng thời 1 lần/ngày, ép tính xác thực.
  - Instagram: đăng công khai, tối ưu reach/thẩm mỹ.
- Không ứng dụng nào trong ba app trên tập trung **tổng hợp lịch sử theo riêng 1 cặp người dùng**.
- App hiện tại đi theo luồng chia sẻ **1-1** (khác với public/broadcast) và đã có kiến trúc **offline-first** với Room lưu trữ dài hạn.
- Vì vậy, Moment Recap biến hạ tầng offline-first bắt buộc thành **lợi thế sản phẩm thực tế**, không chỉ là yêu cầu kỹ thuật.

## Nguyên tắc thiết kế
- Mốc thời gian theo lịch chuẩn:
  - Tuần: **thứ 2 -> chủ nhật**
  - Tháng: **ngày 1 -> cuối tháng**
- Không tính từ ngày pairing hoặc bài đăng đầu tiên, để logic đơn giản và nhất quán cho mọi cặp.
- Khoảng thời gian không có media thì **không tạo recap**.
- Recap sinh hoàn toàn từ dữ liệu local trong **Room**, không cần gọi network, đúng tinh thần offline-first.
- Media sắp xếp theo thời gian; bản đầu **không dùng AI** chọn ảnh đẹp nhất để giữ phạm vi đơn giản.
- Video chỉ hiển thị **thumbnail có sẵn**, không ghép nhiều video thành một clip (tránh MediaMuxer/FFmpeg vì độ khó không tương xứng giá trị).

## Kiến trúc triển khai
- Thêm bảng Room mới **`RecapEntity`** trong `:core:database`, không đổi schema `Message`/`Media` hiện có.
- Domain layer bổ sung:
  - `RecapRepository` interface
  - `GenerateRecapUseCase`
  - `GetRecapsUseCase`
- Các thành phần domain giữ dạng **pure Kotlin**, tuân thủ Clean Architecture của project.
- Sinh recap định kỳ bằng **WorkManager**, tái sử dụng pattern worker đang dùng cho upload retry.
- UI tái dùng component **Carousel** đã có cho hiển thị ảnh bắt buộc, không viết layout mới.
- Tích hợp vào module `:feature:history` hiện có, không tách module riêng vì đây là một view mode bổ sung của màn History.
