# Tài liệu project

Tài liệu trong thư mục này mô tả trạng thái của code trên branch hiện tại. Khi code thay đổi, cập nhật tài liệu liên quan trong cùng Pull Request để tránh sai lệch giữa implementation và mô tả.

| Tài liệu | Nội dung |
|---|---|
| [project-status.md](project-status.md) | Phạm vi đã triển khai, giới hạn và invariant chính |
| [architecture.md](architecture.md) | Kiến trúc module, offline-first và luồng dữ liệu |
| [data-schema.md](data-schema.md) | Firestore collections, Storage path và Room schema |
| [register-firebase-auth.md](register-firebase-auth.md) | Authentication, profile và offline fallback |
| [photo-post-lifecycle.md](photo-post-lifecycle.md) | Xử lý ảnh, upload, retry, đồng bộ và cleanup |
| [code-review-guide.md](code-review-guide.md) | Bản đồ code và thứ tự đọc từng feature |
| [firebase-environment.md](firebase-environment.md) | Cấu hình local, emulator, App Check và deploy |
| [differentiating-feature.md](differentiating-feature.md) | Đề xuất Moment Recap, chưa triển khai |

## Nguồn sự thật

- Code và automated tests là nguồn xác nhận hành vi đã triển khai.
- `firestore.rules`, `storage.rules` và `firestore.indexes.json` là nguồn xác nhận quyền và index được version control.
- Firebase Console là nguồn xác nhận trạng thái đã deploy; commit trong repository không chứng minh production đã được cập nhật.
- `submission/` chỉ chứa artifact trình bày và link demo, không phải tài liệu kỹ thuật nguồn.

