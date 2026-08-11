# Memento

## Project Overview

Memento is a small 1-to-1 social media application
that allows connected users to share photos and videos
in real time.

The project focuses on:

- 1-to-1 media sharing
- Photo and video support
- Offline history
- Media caching
- Smooth media experience

## Tech Stack

- Kotlin
- Jetpack Compose
- Navigation Compose
- Firebase Authentication
- Cloud Firestore
- Firebase Storage
- Room [Planned]
- Kotlin Coroutines
- Flow [Planned]
- Coil [Planned]
- Media3 [Planned]

## Architecture

The application follows an MVVM-based architecture.

UI
↓
ViewModel
↓
Repository
↓
Remote / Local Data Source

## Project Structure

## How to Run

### Requirements

- Android Studio
- JDK
- Android Emulator or Android device

### Setup

1. Clone the repository.
2. Open the project in Android Studio.
3. Sync Gradle.
4. Configure Firebase.
5. Start an emulator or connect an Android device.
6. Run the app.

## Firebase 

The application uses:

- Firebase Authentication
- Cloud Firestore
- Firebase Storage

Firebase configuration instructions will be added
when Firebase integration is implemented.

## Git Workflow

### Branches

- `main`: stable version
- `develop`: integration branch
- `feature/*`: new features
- `fix/*`: bug fixes

### Example

```bash
git checkout develop
git checkout -b feature/login
```

## Development Status