# Memento Architecture

## 1. Architecture Goal

The goal of this document is to define a clear and maintainable architecture for the Memento MVP before implementing major features and integrating Firebase.

The architecture should:

* Keep UI code separate from business logic.
* Keep data access separate from UI.
* Make Firebase and Room replaceable at the repository level.
* Make UI state predictable.
* Support realtime data updates.
* Support local caching and offline access.
* Remain simple enough for an MVP.
* Avoid unnecessary abstraction and over-engineering.

---

# 2. Technology Stack

Memento is an Android application built with:

* Kotlin
* Jetpack Compose
* Android Jetpack
* MVVM
* Repository Pattern
* Firebase Authentication
* Cloud Firestore
* Firebase Storage
* Room
* Kotlin Coroutines
* Kotlin Flow

---

# 3. Architecture Pattern

Memento uses:

> **MVVM + Repository Pattern**

The high-level architecture is:

```text
┌─────────────────────────────────────┐
│             Compose UI              │
│                                     │
│ Screen / Component / Navigation    │
└──────────────────┬──────────────────┘
                   │
                   │ State / Event
                   ▼
┌─────────────────────────────────────┐
│             ViewModel               │
│                                     │
│ UI State                            │
│ User Actions                        │
│ Presentation Logic                  │
└──────────────────┬──────────────────┘
                   │
                   │ Request / Observe
                   ▼
┌─────────────────────────────────────┐
│            Repository               │
│                                     │
│ Coordinates data sources            │
│ Applies data/business rules         │
└───────────────┬─────────────┬───────┘
                │             │
                ▼             ▼
       ┌──────────────┐ ┌──────────────┐
       │ Remote       │ │ Local        │
       │ Data Source  │ │ Data Source  │
       │              │ │              │
       │ Firebase     │ │ Room         │
       │ Auth         │ │ Database     │
       │ Firestore    │ │              │
       │ Storage      │ │              │
       └──────────────┘ └──────────────┘
```

---

# 4. Layer Responsibilities

## 4.1 Compose UI

The UI layer is responsible for displaying the current state and sending user actions.

Examples:

* LoginScreen
* RegisterScreen
* HomeScreen
* ConnectionScreen
* MediaPreviewScreen
* HistoryScreen
* Reusable Compose components

The UI should:

* Display UI state.
* Handle user interaction.
* Send events to the ViewModel.
* Navigate between screens.
* Display loading, content, empty, and error states.

The UI should **not**:

* Call Firebase directly.
* Query Room directly.
* Perform repository operations.
* Contain complex business logic.

Example:

```text
User taps "Send"
        ↓
UI sends event
        ↓
ViewModel.sendMedia()
```

---

# 5. ViewModel

The ViewModel acts as the bridge between Compose UI and the Repository.

Responsibilities:

* Receive user actions.
* Call Repository methods.
* Maintain UI state.
* Expose state to Compose.
* Handle presentation-level logic.
* Survive configuration changes.
* Launch coroutines for asynchronous operations.

Example:

```text
Compose UI
    │
    │ onSend()
    ▼
ViewModel
    │
    │ repository.sendMedia()
    ▼
Repository
```

The ViewModel should not directly access Firebase or Room.

---

# 6. UI State

Each feature should expose a predictable UI state.

A typical state model is:

```text
Loading
Content
Empty
Error
```

For example:

```kotlin
sealed interface HistoryUiState {

    data object Loading : HistoryUiState

    data class Content(
        val moments: List<Moment>
    ) : HistoryUiState

    data object Empty : HistoryUiState

    data class Error(
        val message: String
    ) : HistoryUiState
}
```

The Compose UI observes this state and renders the appropriate UI.

```text
ViewModel
    ↓
StateFlow<HistoryUiState>
    ↓
Compose
    ↓
when(state)
```

---

# 7. Repository

The Repository is responsible for coordinating data access.

The Repository provides a clean API to the ViewModel.

For example:

```kotlin
interface MomentRepository {

    fun observeMoments(): Flow<List<Moment>>

    suspend fun sendMoment(moment: Moment)

    suspend fun retryMoment(momentId: String)
}
```

The ViewModel does not need to know whether the data comes from:

* Firebase
* Room
* Cache
* Another source

The Repository handles this decision.

---

# 8. Why Use Repository Pattern?

Without a Repository:

```text
ViewModel
   ├── Firebase
   ├── Firestore
   ├── Storage
   └── Room
```

This makes the ViewModel tightly coupled to data sources.

With Repository:

```text
ViewModel
    ↓
Repository
    ↓
Firebase / Room
```

The ViewModel only cares about the data it needs.

This makes the application:

* Easier to test
* Easier to maintain
* Easier to change
* Easier to support offline behavior

---

# 9. Remote Data Source

The Remote Data Source handles communication with Firebase.

For Memento, remote sources include:

### Firebase Authentication

Used for:

* Register
* Login
* Logout
* Current user

### Cloud Firestore

Used for:

* User information
* User connections
* Shared moments metadata
* Realtime updates

### Firebase Storage

Used for:

* Photos
* Videos
* Thumbnails

Conceptually:

```text
RemoteDataSource
      │
      ├── Firebase Auth
      ├── Firestore
      └── Firebase Storage
```

The Remote Data Source should hide Firebase-specific implementation details from the Repository.

---

# 10. Local Data Source

The Local Data Source handles persistent data stored on the device.

Memento will use:

> Room

Room will eventually be used for:

* Cached moments
* Media metadata
* Offline history
* Synchronization state

Conceptually:

```text
LocalDataSource
      │
      └── Room
             │
             ├── Entity
             ├── DAO
             └── Database
```

The Local Data Source should not be accessed directly by the UI.

---

# 11. Repository + Remote + Local

The intended relationship is:

```text
                    ViewModel
                        │
                        ▼
                   Repository
                  /           \
                 /             \
                ▼               ▼
       RemoteDataSource    LocalDataSource
                │               │
                ▼               ▼
             Firebase          Room
```

The Repository determines how these sources work together.

For example:

```text
Observe History
      ↓
Repository
      ↓
Room provides cached data
      ↓
UI displays cached data
      ↓
Firebase provides updated data
      ↓
Repository updates Room
      ↓
Room emits new data
      ↓
UI updates
```

This provides the foundation for an offline-first architecture.

---

# 12. Single Source of Truth

Memento should move toward a:

> **Single Source of Truth**

architecture.

For persistent application data, the preferred flow is:

```text
Firebase
    ↓
Repository
    ↓
Room
    ↓
ViewModel
    ↓
Compose UI
```

The UI should primarily observe local state rather than directly observing Firebase and Room independently.

This avoids situations where:

```text
Firebase says:
Photo A

Room says:
Photo B

UI displays:
Photo A + Photo B
```

Instead, Room becomes the local source observed by the UI.

---

# 13. Realtime Data Flow

Memento requires realtime updates.

For example, User A sends a photo.

```text
User A
   │
   │ Upload media
   ▼
Firebase Storage
   │
   ▼
Firestore
   │
   │ Realtime update
   ▼
Repository
   │
   ▼
Room
   │
   ▼
ViewModel
   │
   ▼
Compose UI
   │
   ▼
User B sees new media
```

This keeps the UI reactive.

---

# 14. One-Way Data Flow

Memento follows a unidirectional flow:

```text
User Action
     ↓
Compose UI
     ↓
ViewModel
     ↓
Repository
     ↓
Data Source
     ↓
Data
     ↓
ViewModel
     ↓
UI State
     ↓
Compose UI
```

For example:

```text
User taps Send
      ↓
onSendClicked()
      ↓
ViewModel
      ↓
Repository.sendMoment()
      ↓
Storage / Firestore
      ↓
Success
      ↓
UI State
      ↓
UI updates
```

This makes state changes easier to reason about and debug.

---

# 15. Package Structure

The initial package structure is:

```text
com.tangai.memento
│
├── core
│   ├── common
│   ├── media
│   └── ui
│
├── data
│   ├── local
│   ├── remote
│   ├── model
│   └── repository
│
├── domain
│   └── model
│
├── feature
│   ├── auth
│   ├── home
│   ├── connection
│   ├── media
│   └── history
│
└── MainActivity.kt
```

The package name follows the configured application package:

```text
com.tangai.memento
```

---

# 16. Core Package

The `core` package contains reusable components that are not tied to a specific feature.

```text
core/
├── common/
├── media/
└── ui/
```

## core/common

Contains common utilities and shared types.

Examples:

```text
Result
Constants
Extensions
Network utilities
Date/time utilities
```

Only truly shared functionality should be placed here.

Avoid putting feature-specific code into `core`.

---

## core/media

Contains media-related utilities.

Potential future responsibilities:

* Image resizing
* Image compression
* Video compression
* Thumbnail generation
* Media metadata
* Media validation

Example:

```text
core/media/
├── MediaCompressor
├── ImageResizer
├── ThumbnailGenerator
└── MediaValidator
```

These should only be created when needed.

---

## core/ui

Contains reusable Compose UI components.

Examples:

```text
LoadingIndicator
ErrorView
EmptyState
PrimaryButton
MediaPlaceholder
```

Feature-specific UI should remain inside its feature package.

---

# 17. Data Package

The `data` package contains implementation details related to data access.

```text
data/
├── local/
├── remote/
├── model/
└── repository/
```

---

## data/local

Contains Room-related implementations.

Potential structure:

```text
data/local/
├── dao/
├── entity/
└── database/
```

Example:

```text
MomentDao
MomentEntity
MementoDatabase
```

---

## data/remote

Contains Firebase-related implementations.

Potential structure:

```text
data/remote/
├── auth/
├── firestore/
└── storage/
```

Example:

```text
FirebaseAuthDataSource
FirebaseMomentDataSource
FirebaseStorageDataSource
```

---

## data/model

Contains data-layer models.

Examples:

```text
MomentDto
UserDto
ConnectionDto
```

These models represent the format used by Firebase or other external data sources.

---

## data/repository

Contains concrete Repository implementations.

Example:

```text
MomentRepositoryImpl
UserRepositoryImpl
AuthRepositoryImpl
```

The ViewModel interacts with the Repository interface rather than Firebase directly.

---

# 18. Domain Package

For the MVP, the domain layer should remain lightweight.

```text
domain/
└── model/
```

Domain models represent concepts used by the application itself.

Examples:

```text
User
Connection
Moment
Media
```

A domain model should not depend directly on Firebase or Room.

For example:

```kotlin
data class Moment(
    val id: String,
    val senderId: String,
    val mediaType: MediaType,
    val createdAt: Long
)
```

The exact models will be defined during feature implementation.

---

# 19. Feature Package

The `feature` package contains user-facing functionality.

```text
feature/
├── auth/
├── home/
├── connection/
├── media/
└── history/
```

Each feature owns its UI and ViewModel.

---

## feature/auth

Contains:

* Login
* Register
* Authentication UI
* AuthViewModel

Example:

```text
auth/
├── LoginScreen.kt
├── RegisterScreen.kt
└── AuthViewModel.kt
```

---

## feature/home

Contains:

* 1-to-1 Preview
* Daily Shared Moment
* Main MVP experience

Example:

```text
home/
├── HomeScreen.kt
└── HomeViewModel.kt
```

---

## feature/connection

Contains:

* Connect User
* Connection state
* Connected user information

Example:

```text
connection/
├── ConnectionScreen.kt
└── ConnectionViewModel.kt
```

---

## feature/media

Contains:

* Media Picker
* Media Preview
* Media display
* Media-related UI state

Example:

```text
media/
├── MediaPreviewScreen.kt
├── MediaPreviewViewModel.kt
└── components/
```

Media processing utilities that are reusable across features should remain in:

```text
core/media
```

---

## feature/history

Contains:

* History screen
* Chronological moments
* History state

Example:

```text
history/
├── HistoryScreen.kt
└── HistoryViewModel.kt
```

---

# 20. Feature Structure

Feature packages should generally follow:

```text
feature/
└── history/
    ├── HistoryScreen.kt
    ├── HistoryViewModel.kt
    ├── HistoryUiState.kt
    └── components/
```

This keeps feature-specific code together.

The exact structure can evolve as the feature becomes more complex.

---

# 21. Navigation

Navigation should remain separate from feature business logic.

The application will use Jetpack Navigation for Compose.

Conceptually:

```text
NavHost
   │
   ├── Login
   ├── Register
   ├── Home
   ├── Connection
   ├── MediaPreview
   └── History
```

Navigation events should be triggered by UI state or user actions rather than repositories directly navigating between screens.

---

# 22. Dependency Direction

Dependencies should generally point inward/downward:

```text
Compose UI
    ↓
ViewModel
    ↓
Repository
    ↓
Data Source
```

The following should be avoided:

```text
Compose UI → Firebase
Compose UI → Room
ViewModel → Firebase
ViewModel → Room
Repository → Compose
Firebase → ViewModel
```

Especially:

> **Data layer should never know about UI.**

---

# 23. What Should Not Be Created Yet

The architecture intentionally avoids creating every possible package immediately.

Do not create unused abstractions such as:

```text
UseCase/
Mapper/
Manager/
Service/
Helper/
Util/
Factory/
```

unless there is a real reason to introduce them.

For example, do not create:

```text
GetDailySharedMomentsUseCase
```

just because Clean Architecture tutorials use UseCases.

If the current feature can be clearly handled by:

```text
ViewModel
    ↓
Repository
```

keep it that way.

---

# 24. MVP Initial Structure

At the beginning of implementation, the project can be much smaller:

```text
com.tangai.memento
│
├── core
│   └── ui
│
├── data
│   └── repository
│
├── feature
│   ├── auth
│   ├── home
│   ├── connection
│   ├── media
│   └── history
│
└── MainActivity.kt
```

As Firebase and Room are introduced:

```text
data/
├── local/
├── remote/
├── model/
└── repository/
```

can be added incrementally.

This prevents over-engineering.

---

# 25. Example Complete Flow

Consider the user sending a photo.

## Step 1 — Compose UI

The user taps:

```text
Send
```

The UI sends an event to the ViewModel.

```kotlin
viewModel.sendMedia(uri)
```

---

## Step 2 — ViewModel

The ViewModel:

* Updates UI state to uploading.
* Calls Repository.

```text
ViewModel
    ↓
Uploading
    ↓
repository.sendMedia()
```

---

## Step 3 — Repository

The Repository:

1. Processes the media.
2. Uploads it to Firebase Storage.
3. Creates Firestore metadata.
4. Handles errors.

```text
Repository
    ↓
Media Processing
    ↓
Storage
    ↓
Firestore
```

---

## Step 4 — Data Source

Firebase-specific operations happen inside the Remote Data Source.

```text
Repository
    ↓
RemoteDataSource
    ↓
Firebase
```

---

## Step 5 — Result

The Repository returns/emits the result.

```text
Firebase
    ↓
Repository
    ↓
ViewModel
    ↓
UI State
    ↓
Compose
```

The UI displays:

```text
Sending...
```

then:

```text
Sent
```

or:

```text
Couldn't send

[ Retry ]
```

---

# 26. Error Handling

Errors should be handled at the appropriate layer.

### Data Source

Responsible for detecting technical errors.

Examples:

```text
FirebaseNetworkException
StorageException
IOException
```

### Repository

Responsible for converting technical failures into application-level results.

Example:

```text
Upload failed
```

### ViewModel

Responsible for exposing an appropriate UI state.

```text
Error(
    message = "Couldn't send this moment."
)
```

### Compose UI

Responsible for displaying the error.

```text
Couldn't send this moment.

[ Retry ]
```

---

# 27. Testing Considerations

The architecture should make components independently testable.

For example:

```text
HistoryViewModel
       ↓
FakeMomentRepository
```

instead of:

```text
HistoryViewModel
       ↓
Firebase
```

This allows ViewModel tests to run without requiring a real Firebase connection.

Repositories can also be tested separately.

---

# 28. Architecture Decision Summary

| Component          | Responsibility                         |
| ------------------ | -------------------------------------- |
| Compose UI         | Render UI and send user events         |
| ViewModel          | Manage UI state and presentation logic |
| Repository         | Coordinate data sources                |
| Remote Data Source | Communicate with Firebase              |
| Local Data Source  | Communicate with Room                  |
| Domain Model       | Represent application concepts         |
| Data Model         | Represent external/local data          |
| Core               | Shared reusable functionality          |
| Navigation         | Manage screen transitions              |

---

# 29. Final Architecture

The final MVP architecture is:

```text
                         MEMENTO
                            │
                            ▼
                    ┌───────────────┐
                    │  Compose UI   │
                    │               │
                    │ Screen        │
                    │ Components    │
                    └───────┬───────┘
                            │
                     User Events
                            │
                            ▼
                    ┌───────────────┐
                    │   ViewModel   │
                    │               │
                    │ UI State      │
                    │ Presentation  │
                    └───────┬───────┘
                            │
                            ▼
                    ┌───────────────┐
                    │  Repository   │
                    │               │
                    │ Data Logic    │
                    └───────┬───────┘
                            │
                 ┌──────────┴──────────┐
                 │                     │
                 ▼                     ▼
        ┌────────────────┐    ┌────────────────┐
        │ Remote Data    │    │ Local Data     │
        │ Source         │    │ Source         │
        │                │    │                │
        │ Firebase Auth  │    │ Room           │
        │ Firestore      │    │ Database       │
        │ Storage        │    │                │
        └────────────────┘    └────────────────┘
```

---

# 30. Architecture Principles

The following principles should guide future implementation.

### Principle 1

**UI does not access Firebase directly.**

### Principle 2

**ViewModel does not access Firebase or Room directly.**

### Principle 3

**Repository hides data-source implementation details.**

### Principle 4

**UI observes state instead of manually requesting data whenever it needs to redraw.**

### Principle 5

**Remote and local data sources are separated.**

### Principle 6

**Shared code belongs in `core`; feature-specific code belongs in `feature`.**

### Principle 7

**Do not create abstractions without a real use case.**

### Principle 8

**Architecture should evolve with the application instead of being fully built before implementation.**

---

# 31. Final Decision

Memento will use:

> **MVVM + Repository Pattern**

with the following primary data flow:

```text
Compose UI
    ↓
ViewModel
    ↓
Repository
    ↓
Remote / Local Data Source
    ↓
Firebase / Room
```

The project will initially implement only the packages required by the current MVP.

Additional layers, abstractions, and infrastructure will be introduced when they solve an actual problem.

This approach provides enough structure for a maintainable application while avoiding unnecessary complexity during MVP development.
