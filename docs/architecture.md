# Memento Architecture (Multi-module Clean Architecture)

Tài liệu này mô tả **kiến trúc đang áp dụng thực tế trong codebase hiện tại** của Memento sau khi tách module Gradle.

---

## 1. Mục tiêu kiến trúc

- Tách rõ 3 layer: **presentation → domain → data**
- Tách module theo feature để scale dần
- Giữ domain thuần Kotlin (dễ test, không phụ thuộc Android/Firebase)
- UI/ViewModel chỉ làm việc qua interface domain
- Dễ thay thế Fake repository bằng Firebase/Room sau này mà không đụng UI

---

## 2. Module map hiện tại

### 2.1 App shell

- `:app`
  - Chứa entrypoint app: `MementoApplication`, `MainActivity`
  - Chứa `navigation` (NavGraph + routes)
  - Chứa Hilt bindings (`AppModule`) để bind interface → implementation

### 2.2 Core modules

- `:core:domain` (**đang dùng**)
  - Shared domain models thuần Kotlin:
    - `User`
    - `Post`
    - `MediaItem`
    - `MediaType`
    - `AudienceType`
- `:core:ui` (**đang dùng**)
  - Shared UI state: `UiState<T>`
- `:core:common` (placeholder)
- `:core:database` (placeholder)
- `:core:network` (placeholder)
- `:core:media` (placeholder)
- `:core:designsystem` (**đang dùng**)
  - Chứa Design System/Theme dùng chung:
    - `MementoTheme`
    - Typography + Google Fonts provider
    - Material color tokens

### 2.3 Feature modules

Mỗi feature gồm 3 module: `domain`, `data`, `presentation`

- `:feature:auth:{domain,data,presentation}`
- `:feature:home:{domain,data,presentation}`
- `:feature:connection:{domain,data,presentation}`
- `:feature:post:{domain,data,presentation}`
- `:feature:history:{domain,data,presentation}`

---

## 3. Quy tắc layer (bắt buộc)

## 3.1 Domain

- Plugin: `kotlin("jvm")`
- Không import:
  - `android.*`
  - `androidx.*`
  - `com.google.firebase.*`
- Chỉ chứa:
  - Repository interfaces
  - Domain models thuần Kotlin
  - (Khi cần) UseCases thuần Kotlin

## 3.2 Data

- Implement repository interface từ domain
- Có thể dùng Android/Firebase/Room
- Không phụ thuộc presentation

## 3.3 Presentation

- Chứa Compose UI + ViewModel
- ViewModel chỉ phụ thuộc domain interface
- Không biết implementation cụ thể của data
- Không truyền `NavController` vào ViewModel

## 3.4 App (composition root)

- Chỉ `:app` được bind interface → implementation qua Hilt (`@Binds`)
- Điều này đang được thực hiện trong `app/src/main/java/com/tangai/memento/di/AppModule.kt`

---

## 4. Dependency graph chuẩn

```text
:app
 ├─> :feature:*:presentation
 ├─> :feature:*:data        (để Hilt bind impl tại app)
 ├─> :core:domain
 ├─> :core:designsystem
 └─> :core:ui

:feature:*:presentation
 ├─> :feature:*:domain
 ├─> :core:domain
 └─> :core:ui

:feature:*:data
 ├─> :feature:*:domain
 ├─> :core:domain
 ├─> :core:database (future)
 └─> :core:network  (future)

:feature:*:domain
 └─> :core:domain
```

---

## 5. Ownership của model

## 5.1 Shared business models (core)

Đặt ở `:core:domain`:

- `User`
- `Post`
- `MediaItem`
- `MediaType`
- `AudienceType`

Lý do: đây là language chung giữa nhiều feature.

## 5.2 Feature-specific model

- `FeedFilter` nằm ở `:feature:home:domain` vì đang là logic riêng của Home feed.

---

## 6. Dòng dữ liệu chuẩn

```text
Compose Screen
   -> ViewModel
      -> Domain Repository Interface
         -> Data Repository Impl (Fake hiện tại)
            -> trả dữ liệu về ViewModel
               -> StateFlow
                  -> Compose render
```

Hiện tại backend chưa bật nên các data module dùng `Fake*Repository`.

---

## 7. Navigation ownership

- NavGraph đặt tại `:app` (`MementoNavGraph`)
- Screen nhận callback navigation từ NavGraph
- Screen/ViewModel không sở hữu `NavController`

Routes hiện có:

- `splash`
- `login`
- `signup`
- `forgot_password`
- `home`
- `connection`
- `history`
- `create_post`
- `media_picker`
- `media_preview`

---

## 8. UI State strategy

- Màn đơn giản dùng `UiState<T>` từ `:core:ui` (Loading / Empty / Success / Error)
- Màn phức tạp có feature-specific state:
  - `LoginUiState`
  - `SignupUiState`
  - `HomeUiState`
  - `CreatePostUiState`

---

## 9. Trạng thái hiện tại vs roadmap

## 9.1 Đang có

- Multi-module Gradle theo feature + layer
- Domain/data/presentation tách riêng
- Hilt binding tập trung tại `:app`
- Fake repository cho toàn bộ flow MVP
- Theme đã tách khỏi `:app` và đặt đúng ở `:core:designsystem`

## 9.2 Chưa bật (để phase sau)

- Firebase thật
- Room thật
- Đồng bộ offline/real-time production
- Media processing thực

---

## 10. Checklist maintain kiến trúc

Khi thêm code mới, luôn check:

1. Domain có lỡ import Android/Firebase không?
2. Presentation có gọi thẳng data impl không?
3. Binding interface → impl có đặt sai chỗ ngoài `:app` không?
4. Model mới là shared hay feature-specific?
5. Navigation có bị đẩy vào ViewModel không?

Nếu vi phạm một trong các điểm trên, phải sửa ngay trước khi merge.
