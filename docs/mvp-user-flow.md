# Memento MVP User Flow

## 1. Overview

This document defines the user flow and screen scope for the Memento MVP.

The purpose of this document is to establish the core user experience before implementing the UI with Jetpack Compose.

The MVP focuses on one core scenario:

> Two connected users can authenticate, share photos/videos with each other in real time, and view previously shared moments.

---

# 2. MVP Scope

The MVP includes the following core capabilities:

* User registration
* User login
* User authentication state
* Connect two users
* Select photo/video
* Preview selected media
* Upload media
* Receive media in realtime
* View shared moments
* View history
* Basic loading state
* Basic empty state
* Basic error state

The MVP prioritizes a complete working flow over advanced optimization.

---

# 3. Main User Flow

```text
                         ┌─────────────┐
                         │     App     │
                         └──────┬──────┘
                                │
                                ▼
                         ┌─────────────┐
                         │   Splash    │
                         └──────┬──────┘
                                │
                                ▼
                       ┌──────────────────┐
                       │ Authenticated ?  │
                       └──────┬─────┬─────┘
                              │     │
                            Yes      No
                              │     │
                              │     ▼
                              │  ┌─────────┐
                              │  │  Login  │
                              │  └────┬────┘
                              │       │
                              │       ├─────────────┐
                              │       │             │
                              │       ▼             ▼
                              │  Login Success   Register
                              │       │             │
                              └───────┴─────────────┘
                                      │
                                      ▼
                              ┌─────────────────┐
                              │  1-to-1 Preview │
                              └───────┬─────────┘
                                      │
                    ┌─────────────────┼─────────────────┐
                    │                 │                 │
                    ▼                 ▼                 ▼
             Connect User        Send Media       View History
                    │                 │                 │
                    ▼                 ▼                 ▼
             User Connection    Media Picker        History
                                      │
                                      ▼
                                Media Preview
                                      │
                                      ▼
                                    Send
                                      │
                                      ▼
                               Upload Media
                                      │
                                      ▼
                           Realtime Conversation
                                      │
                                      ▼
                              Receiver sees media
```

---

# 4. Authentication Flow

## 4.1 Splash

The application starts from the Splash screen.

The application checks whether the user already has a valid authentication session.

```text
Splash
  │
  ├── Authenticated
  │       ↓
  │   1-to-1 Preview
  │
  └── Not Authenticated
          ↓
        Login
```

### Responsibilities

* Check authentication state
* Prevent displaying the wrong initial screen
* Redirect the user to the appropriate destination

### States

* Loading
* Authenticated
* Unauthenticated

---

# 5. Login Flow

```text
Login
 │
 ├── Login Success
 │       ↓
 │   1-to-1 Preview
 │
 ├── Invalid Credentials
 │       ↓
 │   Error Message
 │
 └── Network Error
         ↓
       Error State
```

### User Actions

* Enter email
* Enter password
* Press Login
* Navigate to Register

### Success

After successful authentication:

```text
Login
  ↓
1-to-1 Preview
```

### Error

For an invalid login:

```text
Login
  ↓
Error
  ↓
User corrects input
```

---

# 6. Register Flow

```text
Register
   │
   ├── Registration Success
   │          ↓
   │      1-to-1 Preview
   │
   ├── Invalid Input
   │          ↓
   │      Validation Error
   │
   └── Registration Failed
              ↓
          Error State
```

### User Actions

* Enter registration information
* Submit registration
* Return to Login

### Success

A successfully registered user is authenticated and redirected to:

```text
Register
   ↓
1-to-1 Preview
```

---

# 7. 1-to-1 Preview

The 1-to-1 Preview is the main screen of the MVP.

It represents the user's current sharing relationship with another user.

### Main Actions

```text
1-to-1 Preview
 │
 ├── Connect User
 │
 ├── Send Media
 │
 └── View History
```

### Responsibilities

* Display connected user
* Display recent shared moment/media
* Allow the user to connect another user
* Start media sharing
* Navigate to history

---

# 8. User Connection Flow

```text
1-to-1 Preview
       ↓
Connect User
       ↓
User Connection
       ↓
Search User
       ↓
User Found?
   ┌───┴────┐
   │        │
  No       Yes
   │        │
   ▼        ▼
 Empty    User
 State    Found
            │
            ▼
         Connect
            │
            ▼
        Connected
            │
            ▼
      1-to-1 Preview
```

## Empty State

If no user matches the search:

```text
User Connection
       ↓
No User Found
       ↓
"No users found"
```

## Error State

If the request fails:

```text
User Connection
       ↓
Error
       ↓
"Unable to connect"
       ↓
Retry
```

---

# 9. Media Sharing Flow

The media sharing flow is one of the most important MVP flows.

```text
1-to-1 Preview
       ↓
   Send Media
       ↓
Android Photo Picker
       ↓
Select Photo / Video
       ↓
Media Preview
       ↓
      Send
       ↓
  Upload Media
       ↓
Realtime Conversation
```

---

# 10. Media Picker

The MVP uses the Android system Photo Picker.

Memento does not implement a custom gallery/media browser in the initial MVP.

```text
1-to-1 Preview
       ↓
   Send Media
       ↓
Android Photo Picker
       ↓
Photo / Video Selected
```

The system picker allows the user to select supported media and returns the selected media URI to the application.

---

# 11. Media Preview

After selecting media:

```text
Media Picker
     ↓
Media Preview
```

The user can:

```text
Media Preview
 │
 ├── Cancel
 │      ↓
 │   1-to-1 Preview
 │
 └── Send
       ↓
   Upload Media
```

### Responsibilities

* Display selected photo/video
* Allow the user to confirm the media
* Allow the user to cancel
* Prepare media for upload

---

# 12. Media Upload Flow

```text
Media Preview
      ↓
     Send
      ↓
   Uploading
      │
      ├── Success
      │      ↓
      │ Realtime Conversation
      │
      └── Failure
             ↓
          Error State
             │
             ├── Retry
             │
             └── Cancel
```

## Uploading State

The UI should communicate that the media is currently being uploaded.

Example:

```text
Uploading...
```

The MVP does not require advanced upload optimization or automatic retry.

---

# 13. Realtime Conversation

The Realtime Conversation represents the shared media stream between the two connected users.

```text
User A
  │
  │ Send Media
  ▼
Firebase
  │
  │ Realtime Update
  ▼
User B
  │
  ▼
Conversation
```

### Responsibilities

* Display sent media
* Display received media
* Receive new media updates
* Display upload/loading states
* Display media loading errors

### Basic States

```text
Loading
Content
Empty
Error
```

---

# 14. Receiving Media

When another user sends media:

```text
Firebase
   ↓
Realtime Update
   ↓
Conversation
   ↓
New Media Appears
```

The receiver should not need to manually refresh the screen.

The MVP relies on Firebase realtime updates to update the UI.

---

# 15. History Flow

```text
1-to-1 Preview
      ↓
  View History
      ↓
    History
      │
      ├── Has Media
      │      ↓
      │   Media List
      │
      └── No Media
             ↓
         Empty State
```

### Responsibilities

* Display previously shared moments
* Display photos/videos
* Allow the user to review previous media

---

# 16. History States

## Loading

```text
History
   ↓
Loading
```

The application is retrieving history data.

## Empty

```text
History
   ↓
No moments yet
```

Example message:

```text
No moments yet.
Start sharing something with your connection.
```

## Content

```text
History
   ↓
Media List
```

## Error

```text
History
   ↓
Unable to load history
   ↓
Retry
```

---

# 17. Screen List

The MVP contains the following application screens:

| Screen                | MVP | Purpose                             |
| --------------------- | --: | ----------------------------------- |
| Splash                | Yes | Check authentication state          |
| Login                 | Yes | Authenticate existing user          |
| Register              | Yes | Create a new user                   |
| 1-to-1 Preview        | Yes | Main screen and sharing entry point |
| User Connection       | Yes | Search and connect users            |
| Media Preview         | Yes | Preview selected media              |
| Realtime Conversation | Yes | Display sent/received media         |
| History               | Yes | View previously shared moments      |
| Profile / Settings    |  No | Post-MVP                            |

### System UI

The Media Picker is not implemented as a Memento screen.

The MVP uses:

```text
Android System Photo Picker
```

for selecting photos and videos.

---

# 18. Navigation Map

```text
Splash
 │
 ├── Login
 │    └── Register
 │          │
 │          └──────────────┐
 │                         │
 └─────────────────────────┤
                           ▼
                    1-to-1 Preview
                      │    │    │
                      │    │    └──────→ History
                      │    │
                      │    └───────────→ Media Picker
                      │                      │
                      │                      ▼
                      │                Media Preview
                      │                      │
                      │                      ▼
                      │               Conversation
                      │
                      └──────────────→ User Connection
                                             │
                                             ▼
                                      1-to-1 Preview
```

---

# 19. UI State Model

The MVP follows a basic state-driven UI model.

Major data-driven screens should consider:

```text
┌─────────┐
│ Loading │
└────┬────┘
     │
     ├───────────────┐
     ▼               ▼
┌─────────┐     ┌─────────┐
│ Content │     │  Empty  │
└─────────┘     └─────────┘
     │
     │ Error
     ▼
┌─────────┐
│  Error  │
└─────────┘
```

The exact states will be implemented through Compose UI state and ViewModel state.

---

# 20. Basic Error Scenarios

The MVP should handle the following basic scenarios.

| Scenario              | Expected UI                |
| --------------------- | -------------------------- |
| Invalid login         | Error message              |
| Invalid registration  | Validation error           |
| User not found        | Empty state                |
| Connection failure    | Error + retry              |
| Media upload failure  | Error + retry              |
| Media loading failure | Error placeholder          |
| Empty history         | Empty state                |
| Network unavailable   | Error / offline indication |

Advanced automatic retry and offline synchronization are outside the initial MVP scope.

---

# 21. MVP Out of Scope

The following features are intentionally excluded from the initial MVP implementation.

### Advanced Performance

* FPS profiling
* Memory profiling
* Advanced image decoding optimization
* Advanced preload strategy
* Scroll-based request cancellation

### Advanced Caching

* Two-level memory + disk cache
* Custom LRU cache management
* Advanced cache eviction strategy

### Advanced Error Handling

* Automatic retry
* Network-aware retry queue
* Complex upload recovery

### Advanced Media Processing

* Color filters
* TensorFlow Lite
* Selfie segmentation
* Face Landmarker
* Background removal
* Skin smoothing
* Automatic lighting correction

### Other

* Profile
* Settings
* Notifications
* Group sharing
* Advanced social features

These features may be added after the MVP is stable.

---

# 22. MVP Success Criteria

The MVP is considered functionally complete when the following end-to-end flow works:

```text
User A
  ↓
Register
  ↓
Login
  ↓
Connect User B
  ↓
Open 1-to-1 Preview
  ↓
Select Photo / Video
  ↓
Preview Media
  ↓
Send
  ↓
Upload
  ↓
Firebase
  ↓
Realtime Update
  ↓
User B receives media
  ↓
User B sees media
  ↓
Media appears in History
```

The MVP should also support:

```text
Login Error
Upload Error
Empty History
User Not Found
Basic Network Error
```

without crashing the application.

---

# 23. Implementation Principle

The MVP prioritizes:

1. Correct functionality
2. Complete end-to-end flow
3. Clear architecture
4. Basic error handling
5. Maintainable code

Advanced optimization will be implemented after the core flow is stable.

The goal is to first answer:

> "Can two users reliably share and view moments?"

Once this works, the project will move toward:

> "Can the experience be made faster, smoother, more reliable, and more intelligent?"
