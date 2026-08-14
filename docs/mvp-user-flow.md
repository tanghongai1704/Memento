# Memento — MVP User Flow

## 1. Overview

Memento is a private social sharing application focused on sharing
personal moments through photos and videos.

The core principle of Memento is:

> Every Moment has a clearly defined audience.

For the MVP, a Moment can only be shared with **one connected user**.

A user cannot send the same Post to multiple individual users at once.

Future group sharing may be introduced through explicit Groups, where all
members know who belongs to the Group. Group functionality is outside the
scope of the MVP.

---

# 2. MVP Product Principles

## 2.1 Private by Default

Every Post has a clearly defined audience.

For the MVP:

```text
1 Post
   ↓
1 Connected User
````

The system does not support:

```text
1 Post
   ↓
User A
User B
User C
```

If multiple people need to receive a Moment in the future, they must belong
to an explicitly created Group.

---

## 2.2 Connected Users Only

A user can only send a Post to a person they are connected with.

Therefore:

```text
Connection
    ↓
Connected User
    ↓
Can receive Moment
```

A user cannot directly send a Moment to an arbitrary user.

---

## 2.3 Transparent Audience

The audience of a Post should always be understandable.

From the sender's perspective:

```text
You → Alice
```

From the receiver's perspective:

```text
Alice → You
```

This reinforces the private nature of Memento.

---

# 3. Main MVP User Flow

```text
                    MEMENTO
                       │
                       ▼
                    Splash
                       │
                       ▼
                     Login
                  /    │     \
                 /     │      \
                ▼      ▼       ▼
            Signup   Forgot   Login
                       Password   │
                                  │
                                  ▼
                                Home
                                  │
             ┌────────────────────┼────────────────────┐
             │                    │                    │
             ▼                    ▼                    ▼
           Filter             Connection           History
             │
             │
             ▼
       All / Specific User
             │
             ▼
          Create Post
             │
             ▼
      Select ONE User
             │
             ▼
        Media Picker
             │
             ▼
        Media Preview
             │
             ▼
             Post
             │
             ▼
            Home
             │
             ▼
        Feed updated
```

---

# 4. Authentication Flow

## 4.1 Splash

The application starts with:

```text
App Launch
    ↓
Splash
```

The Splash Screen is responsible only for the initial application entry
experience.

After Splash:

```text
Splash
   ↓
Login
```

Authentication persistence can be implemented later with Firebase
Authentication.

---

# 5. Login Flow

## 5.1 Login Screen

The Login Screen contains:

* Login title
* Account field
* Password field
* Login button
* Forgot Password
* Sign Up navigation

Conceptually:

```text
┌──────────────────────────────┐
│                              │
│           Memento            │
│                              │
│            Login             │
│                              │
│  Account                     │
│  [ email / phone          ]  │
│                              │
│  Password                    │
│  [ ***********            ]  │
│                              │
│         [ Login ]            │
│                              │
│       Forgot password?       │
│                              │
│   Don't have an account?     │
│          Sign up             │
│                              │
└──────────────────────────────┘
```

### Success

```text
Login
  ↓
Home
```

### Error

Examples:

```text
Invalid account
Invalid password
Network error
```

For the initial MVP UI implementation, authentication can use mock data.

Real Firebase Authentication will be implemented in a later task.

---

# 6. Signup Flow

## 6.1 Signup Screen

The Signup Screen contains:

* Username
* Email / Phone
* Password
* Confirm Password
* Sign Up button
* Back/Login navigation

Conceptually:

```text
┌──────────────────────────────┐
│           Sign Up            │
│                              │
│  Username                    │
│  [                      ]    │
│                              │
│  Email / Phone               │
│  [                      ]    │
│                              │
│  Password                    │
│  [                      ]    │
│                              │
│  Confirm Password            │
│  [                      ]    │
│                              │
│         [ Sign Up ]          │
│                              │
│       Already have account?  │
│             Login            │
└──────────────────────────────┘
```

Basic validation:

* Required fields must not be empty.
* Password and Confirm Password must match.

### Success

```text
Signup
   ↓
Home
```

Real account creation will be implemented with Firebase Authentication
later.

---

# 7. Forgot Password Flow

Forgot Password is part of the MVP navigation but does not require
functional password recovery yet.

Flow:

```text
Login
  ↓
Forgot Password
```

Initial screen:

```text
Forgot Password

Coming soon...
```

Actual password reset functionality will be implemented later.

---

# 8. Home Flow

Home is the central screen of Memento.

The Home Screen contains:

1. Feed filter
2. Chronological feed
3. Create Post button

---

## 8.1 Feed

Posts are displayed from:

```text
Newest
   ↓
Older
```

The feed should use a scrollable list.

Example:

```text
┌──────────────────────────────┐
│ Memento                      │
│                              │
│ [ All ] [ Alice ] [ Bob ]    │
├──────────────────────────────┤
│                              │
│ You → Alice                  │
│ 10:32 AM                     │
│                              │
│       [ Image ]              │
│                              │
├──────────────────────────────┤
│ Alice → You                  │
│ 09:41 AM                     │
│                              │
│       [ Video ]              │
│                              │
├──────────────────────────────┤
│ You → Bob                    │
│ 08:20 AM                     │
│                              │
│       [ Image ]              │
│                              │
└──────────────────────────────┘

                         [ + ]
```

Unlike Locket, a single Post does not occupy the entire screen.

Multiple Posts can be visible in the feed.

---

# 9. Feed Filter

The Home Screen provides a filter for viewing different private
timelines.

Default:

```text
[ All ]
```

Available connected users:

```text
[ All ] [ Alice ] [ Bob ] [ Charlie ]
```

---

## 9.1 All Filter

When:

```text
Filter = All
```

the feed displays all Moments that the current user is allowed to see.

Example:

```text
You → Alice
Alice → You

You → Bob
Bob → You

You → Charlie
Charlie → You
```

Posts remain sorted:

```text
Newest → Oldest
```

---

## 9.2 User Filter

When:

```text
Filter = Alice
```

the feed represents the private 1-to-1 timeline between the current user
and Alice.

It should contain:

```text
You → Alice
Alice → You
```

It should NOT mean:

```text
Only posts authored by Alice
```

The filter represents a private relationship/timeline.

---

# 10. Create Post Flow

The user can create a Post using a persistent Create Post button,
represented by a Floating Action Button:

```text
[ + ]
```

The button is available from the Home feed.

Flow:

```text
Home
  ↓
Create Post
  ↓
Select Recipient
  ↓
Media Picker
  ↓
Media Preview
  ↓
Post
  ↓
Home
```

---

# 11. Selecting the Recipient

The recipient selection behavior depends on the current Home filter.

---

## 11.1 When Filter = All

No recipient is selected initially.

The user must select exactly one connected user.

Example:

```text
Create Moment

Share with:

○ Alice
○ Bob
○ Charlie
```

Only one user can be selected.

The UI must NOT allow:

```text
☑ Alice
☑ Bob
☑ Charlie
```

---

## 11.2 When Filter = Specific User

For example:

```text
Filter = Alice
```

When the user presses Create Post:

```text
Create Moment

Share with:
Alice

[ Change recipient ]
```

Alice is selected by default.

The user can still change the recipient.

---

# 12. Media Picker Flow

After selecting the recipient:

```text
Select Recipient
       ↓
Media Picker
```

The MVP supports the concept of:

* Photo
* Video
* Multiple photos

The actual media processing pipeline is outside the initial UI MVP.

Future processing includes:

```text
Media
  ↓
Resize
  ↓
Compress
  ↓
Thumbnail
  ↓
Upload
```

---

# 13. Media Preview Flow

After selecting media:

```text
Media Picker
     ↓
Media Preview
```

The Preview Screen displays:

* Selected media
* Selected recipient
* Back/Edit action
* Post action

Example:

```text
┌──────────────────────────────┐
│         Preview              │
│                              │
│       [ Media ]              │
│                              │
│  Share with: Alice           │
│                              │
│   [ Edit ]      [ Post ]     │
└──────────────────────────────┘
```

The user must be able to review the Moment before posting.

---

# 14. Posting Flow

When the user presses Post:

```text
Media Preview
      ↓
     Post
      ↓
   Uploading
      ↓
    Success
      ↓
     Home
```

For the initial UI MVP, the upload can be simulated.

Later:

```text
Post
 ↓
Resize / Compress
 ↓
Firebase Storage
 ↓
Save metadata to Firestore
 ↓
Realtime listener
 ↓
Feed updated
```

---

# 15. Post Model Concept

For MVP, every Post has one audience.

Conceptually:

```text
Post
├── id
├── authorId
├── audienceType
├── audienceId
├── media
└── createdAt
```

For MVP:

```text
audienceType = USER
```

and:

```text
audienceId = connectedUserId
```

Example:

```text
Post
authorId = userA
audienceType = USER
audienceId = userB
```

This means:

```text
User A → User B
```

---

# 16. Future Group Extension

Groups are NOT part of the MVP.

Future flow:

```text
Create Group
      ↓
Select Members
      ↓
Group Created
      ↓
All members know the members
      ↓
Create Post
      ↓
Select Group
      ↓
Post → Group
```

The future model can support:

```text
audienceType

USER
GROUP
```

Therefore the MVP should avoid designing the Post model in a way that
only supports multiple recipients.

The MVP only implements:

```text
USER
```

---

# 17. Connection Flow

Connection is required before a user can send a Moment to another user.

Basic MVP flow:

```text
Home
  ↓
Connection
  ↓
Connected Users
```

Example:

```text
My Connections

Alice
Bob
Charlie
```

These users are available as recipients when creating a Post.

The initial UI MVP does not need to implement the complete connection
request/accept system.

Real connection management can be implemented later.

---

# 18. History Flow

The user can navigate to History from Home.

```text
Home
  ↓
History
```

History displays previously available Moments.

Sorting:

```text
Newest
   ↓
Older
```

Future versions may group history by:

```text
Today
Yesterday
This Week
Older
```

Offline history using Room is outside the initial UI MVP.

---

# 19. Basic UI States

Each screen should be designed with basic states where appropriate.

## Loading

```text
Loading
   ↓
Display progress indicator
```

## Content

```text
Content
   ↓
Display normal UI
```

## Empty

Example:

```text
No Moments yet.

Start sharing a moment with someone.
```

## Error

Example:

```text
Something went wrong.

[ Retry ]
```

The initial MVP can simulate these states.

Real error handling and retry mechanisms will be implemented later.

---

# 20. Home UI State

Conceptually:

```text
HomeUiState
├── posts
├── connections
├── selectedFilter
├── isLoading
└── errorMessage
```

Filter:

```text
FeedFilter
├── All
└── User(userId)
```

Future:

```text
FeedFilter
├── All
├── User(userId)
└── Group(groupId)
```

Group is not implemented in MVP.

---

# 21. Create Post UI State

Conceptually:

```text
CreatePostUiState
├── selectedRecipient
├── selectedMedia
├── isUploading
├── uploadProgress
└── errorMessage
```

Important rule:

```text
selectedRecipient
```

must represent exactly one User in MVP.

---

# 22. Authentication UI States

## Login

```text
LoginUiState
├── account
├── password
├── isLoading
└── errorMessage
```

## Signup

```text
SignupUiState
├── username
├── account
├── password
├── confirmPassword
├── isLoading
└── errorMessage
```

---

# 23. MVP Navigation Map

```text
Splash
  ↓
Login
  ├── Signup
  │     ↓
  │    Home
  │
  └── Forgot Password
        ↓
      Login

Login success
  ↓
Home
  ├── Connection
  │
  ├── History
  │
  └── Create Post
        │
        ▼
   Select User
        │
        ▼
   Media Picker
        │
        ▼
   Media Preview
        │
        ▼
       Post
        │
        ▼
       Home
```

---

# 24. MVP Scope

## Included

* Splash
* Login UI
* Signup UI
* Forgot Password placeholder
* Home feed
* Chronological feed
* All filter
* User filter
* Connection screen
* Create Post
* Select one connected user
* Media Picker UI
* Media Preview UI
* History UI
* Basic Loading state
* Basic Empty state
* Basic Error state
* Mock/fake data
* Basic navigation

---

# 25. Not Included in Initial UI MVP

The following are intentionally postponed:

* Firebase Authentication
* Firestore
* Firebase Storage
* Realtime synchronization
* Room
* Offline-first architecture
* Memory Cache
* Disk Cache
* Image compression
* Image resizing
* Video compression
* Thumbnail generation
* Advanced retry
* Network monitoring
* Media preload
* Performance profiling
* TensorFlow Lite
* Face detection
* Background removal
* Skin smoothing
* Automatic lighting adjustment
* Filters
* Groups
* Multiple individual recipients
* Push notifications

These features will be implemented in later stages.

---

# 26. MVP Success Criteria

The UI MVP is considered complete when a user can conceptually perform:

```text
Open App
   ↓
Login / Signup
   ↓
Home
   ↓
View Feed
   ↓
Filter All / User
   ↓
Create Post
   ↓
Select ONE Connected User
   ↓
Select Media
   ↓
Preview Media
   ↓
Post
   ↓
Return to Home
```

The application should also allow navigation to:

```text
Connection
History
Forgot Password
```

without crashes.

The implementation does not need real backend functionality yet.

The goal of this MVP stage is to validate:

1. Screen structure.
2. Navigation.
3. UI state management.
4. Core user flow.
5. Private 1-to-1 sharing concept.
6. Future extensibility toward Groups and real-time backend.
