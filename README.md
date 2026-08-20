# Memento

Memento là ứng dụng Android chia sẻ media **1-1** giữa hai người dùng, xây theo hướng multi-module và Clean Architecture.

## Hiện trạng dự án

- Có các flow chính: **Auth, Home, Connection, Create Post, History**
- UI dùng **Jetpack Compose**
- Kiến trúc theo layer: **presentation → domain → data**
- Data hiện tại đang dùng **Fake Repository** để hoàn thiện luồng MVP

## Công nghệ chính

- Kotlin, Coroutines
- Jetpack Compose, Navigation Compose, Material 3
- Hilt (DI)
- Room (module nền tảng)
- Firebase SDK (đã cấu hình dependency trong project)

## Cấu trúc module

```text
app
core/
  common, domain, ui, designsystem, database, network, media
feature/
  auth/{domain,data,presentation}
  home/{domain,data,presentation}
  connection/{domain,data,presentation}
  post/{domain,data,presentation}
  history/{domain,data,presentation}
```

## Chạy dự án

1. Mở project bằng Android Studio (JDK 11+).
2. Sync Gradle.
3. Chạy module `app` trên emulator hoặc thiết bị thật.

## Ghi chú

- `docs/architecture.md`: mô tả kiến trúc chi tiết.
- `docs/differentiating-feature.md`: mô tả differentiating feature **Moment Recap**.
- `docs/register-firebase-auth.md`: mô tả đầy đủ flow Register Email/Password qua Firebase Auth + Firestore + Room.