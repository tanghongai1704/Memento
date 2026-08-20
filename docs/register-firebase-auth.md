# Register bằng Email/Password qua Firebase (module `:feature:auth`)

Tài liệu này mô tả đầy đủ phần triển khai tính năng **đăng ký tài khoản** theo kiến trúc Clean Architecture multi-module trong project Memento.

## 1. Mục tiêu nghiệp vụ

Flow người dùng:

1. Nhập `Email`, `Password`, `Confirm Password`.
2. Validate input ở client.
3. Gọi Firebase Authentication để tạo account.
4. Nếu thành công, tạo user profile trên Firestore.
5. Cache user vào Room (local).
6. Phát event thành công để `:app` điều hướng sang Home.

Nếu lỗi ở bất kỳ bước nào: hiển thị lỗi rõ ràng, không crash app.

---

## 2. Ranh giới kiến trúc

## `feature/auth/domain` (pure Kotlin)
- Không phụ thuộc `androidx`, `android.*`, Firebase SDK.
- Chứa:
  - `AuthRepository` (contract nghiệp vụ)
  - `ValidateEmailUseCase`
  - `ValidatePasswordUseCase`
  - `ValidateRegisterInputUseCase`
  - `SignUpUseCase`
  - `RegisterError`, `RegisterException`, `ValidationResult`
- Dùng lại `User` từ `core:domain`.

## `feature/auth/data`
- Là nơi duy nhất gọi trực tiếp Firebase Auth + Firestore.
- Chứa:
  - `FirebaseAuthDataSource`: wrap call FirebaseAuth + phân loại lỗi kỹ thuật.
  - `AuthRepositoryImpl`: điều phối Auth + Firestore + Room.
  - Mapper `FirebaseUser -> User` để không lộ Firebase model ra ngoài data.

## `feature/auth/presentation`
- Chỉ gọi use case của domain.
- Không import class từ data.
- Không tự điều hướng sang feature khác: chỉ phát `effect`.

## `:app`
- Đặt Hilt binding `AuthRepository -> AuthRepositoryImpl`.
- Nhận `RegisterEffect.NavigateToHome` và xử lý navigation.

---

## 3. Thiết kế nghiệp vụ register

### 3.1 Vì sao gộp tạo profile vào `signUp()`

`AuthRepository.signUp(email, password)` được thiết kế để bao trọn nghiệp vụ “đăng ký hoàn tất”:

- Tạo account trên FirebaseAuth
- Tạo profile trên Firestore
- Cache user vào Room

Lý do: tránh trạng thái nửa vời (đã tạo auth account nhưng thiếu profile/cache).

### 3.2 Contract domain

`AuthRepository`:

- `suspend fun signUp(email: String, password: String): Result<User>`
- `suspend fun login(account: String, password: String): Result<Unit>` (luồng cũ còn giữ lại)

---

## 4. Validate input

`ValidateRegisterInputUseCase` trả về `RegisterValidationResult` với lỗi theo từng field:

- `email`: bắt buộc + đúng format email.
- `password`: tối thiểu 8 ký tự, có chữ và số.
- `confirmPassword`: bắt buộc + khớp password.

`ValidationResult`:
- `Valid`
- `Invalid(reason)`

Nhờ vậy UI có thể hiển thị message cụ thể cho từng ô nhập.

---

## 5. Mapping lỗi Firebase → lỗi domain

Trong data source/repository:

| Lỗi kỹ thuật | Lỗi domain/hiển thị |
|---|---|
| `FirebaseAuthUserCollisionException` | Email đã tồn tại |
| `FirebaseAuthWeakPasswordException` | Mật khẩu yếu |
| `FirebaseAuthInvalidCredentialsException` | Email không hợp lệ / thông tin không hợp lệ |
| `FirebaseNetworkException` | Lỗi mạng |
| Khác | Lỗi không xác định |

Kết quả cuối cùng trả về `RegisterException(RegisterError)` để UI hiển thị message dễ hiểu.

---

## 6. Luồng data chi tiết

1. `RegisterViewModel.onRegisterClick()`
2. Validate input bằng `ValidateRegisterInputUseCase`
3. Nếu hợp lệ: gọi `SignUpUseCase(email, password)`
4. `SignUpUseCase` gọi `AuthRepository.signUp()`
5. `AuthRepositoryImpl.signUp()`:
   - gọi `FirebaseAuthDataSource.signUp()`
   - map `FirebaseUser -> User`
   - ghi Firestore: `users/{uid}` với `uid`, `email`, `username`, `createdAt`
   - cache Room qua `UserDao.upsertUser()`
6. Thành công:
   - `uiState.isRegisterSuccess = true`
   - emit `RegisterEffect.NavigateToHome`
7. Thất bại:
   - set `generalError` để UI hiển thị Snackbar

---

## 7. Presentation state & effect

`RegisterUiState`:

- `email`, `password`, `confirmPassword`
- `isLoading`
- `emailError`, `passwordError`, `confirmPasswordError`
- `generalError`
- `isRegisterSuccess`

`RegisterEffect`:

- `NavigateToHome`

Màn `RegisterScreen`:
- 3 `OutlinedTextField` (Password dùng `PasswordVisualTransformation`)
- Disable tương tác khi loading
- Nút Register hiển thị loading
- Snackbar khi có `generalError`
- Không tự gọi navigation route Home; route do `NavGraph` bên app xử lý.

---

## 8. Hạ tầng core đã thêm

## `core:network`
- `FirebaseModule` (Hilt `@Provides`):
  - `FirebaseAuth`
  - `FirebaseFirestore`

## `core:database`
- `UserEntity`
- `UserDao`
- `MementoDatabase`
- `DatabaseModule` (Hilt provide DB + DAO)

> Lưu ý: Room hiện tạo mới DB `memento.db` với entity `users`.

---

## 9. Navigation tại app

Đổi route đăng ký sang:

- `MementoRoute.Register("register")`

Nav flow:

- Login -> Register
- Register success -> Home (qua effect do app bắt và điều hướng)

---

## 10. Danh sách file chính đã triển khai

### Domain
- `feature/auth/domain/.../AuthRepository.kt`
- `feature/auth/domain/.../model/RegisterError.kt`
- `feature/auth/domain/.../model/ValidationResult.kt`
- `feature/auth/domain/.../usecase/ValidateEmailUseCase.kt`
- `feature/auth/domain/.../usecase/ValidatePasswordUseCase.kt`
- `feature/auth/domain/.../usecase/ValidateRegisterInputUseCase.kt`
- `feature/auth/domain/.../usecase/SignUpUseCase.kt`

### Data
- `feature/auth/data/.../source/FirebaseAuthDataSource.kt`
- `feature/auth/data/.../source/util/AwaitTask.kt`
- `feature/auth/data/.../model/AuthDataError.kt`
- `feature/auth/data/.../mapper/FirebaseUserMapper.kt`
- `feature/auth/data/.../AuthRepositoryImpl.kt`

### Core
- `core/network/.../di/FirebaseModule.kt`
- `core/database/.../MementoDatabase.kt`
- `core/database/.../dao/UserDao.kt`
- `core/database/.../model/UserEntity.kt`
- `core/database/.../di/DatabaseModule.kt`

### Presentation
- `feature/auth/presentation/.../viewmodel/RegisterUiState.kt`
- `feature/auth/presentation/.../viewmodel/RegisterViewModel.kt`
- `feature/auth/presentation/.../ui/RegisterScreen.kt`
- `feature/auth/presentation/.../ui/LoginScreen.kt` (điều hướng sang Register)

### App wiring
- `app/.../di/AppModule.kt`
- `app/.../navigation/MementoRoute.kt`
- `app/.../navigation/MementoNavGraph.kt`

---

## 11. Cách kiểm tra nhanh

1. Vào Login -> bấm “Register”.
2. Thử input sai để kiểm tra lỗi từng field.
3. Đăng ký email mới hợp lệ.
4. Xác nhận app điều hướng Home khi thành công.
5. Kiểm tra Firestore có document `users/{uid}`.
6. Kiểm tra local DB có bản ghi user trong bảng `users`.
