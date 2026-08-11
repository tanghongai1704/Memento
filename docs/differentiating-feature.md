# Memento MVP Differentiating Feature

## 1. Feature Overview

### Selected Feature

**Daily Shared Moment**

### Short Description

Daily Shared Moment is a private daily sharing experience between two connected users.

Each connected pair has a shared space where they can see the moments they have exchanged during the day.

The feature is designed around the idea:

> **One private place for two people to share and remember their moments.**

The feature does not attempt to create another public social network. Instead, it strengthens Memento's core identity as a private 1-to-1 memory-sharing application.

---

# 2. Problem

Existing social media applications are generally designed around large social networks.

Users are often exposed to:

* Public feeds
* Followers
* Likes
* Comments
* Recommendations
* Algorithmic content
* Large amounts of unrelated content

For a private relationship, this can create unnecessary complexity.

A user may simply want to:

> "Share a small moment with one specific person and be able to look back at those moments later."

The basic media-sharing flow solves the technical problem of sending media, but it does not yet provide a strong product identity.

Memento therefore needs a simple feature that turns individual media messages into a shared experience.

---

# 3. Why Users Need It

The core value of Memento is not simply uploading photos or videos.

The deeper value is:

> **Keeping small moments shared between two people.**

A user should be able to open Memento and immediately understand:

* What did we share today?
* What moments did the other person send?
* What did we share together?
* Can I look back at previous moments?

This creates a stronger emotional and functional reason to return to the application.

---

# 4. Existing Solutions

Several existing applications provide related concepts.

### Locket

Locket emphasizes sharing personal moments with a small group of connected people.

Relevant insight:

> Sharing can feel more personal when the audience is intentionally limited.

### BeReal

BeReal emphasizes sharing a moment within a specific time/context.

Relevant insight:

> A shared moment can become more meaningful when it is associated with a particular time.

### Instagram

Instagram provides strong media presentation and memory/history capabilities.

Relevant insight:

> Users are familiar with browsing collections of photos and videos chronologically or through different layouts.

---

# 5. Gap / Opportunity

Memento combines these concepts but focuses specifically on **two people**.

Instead of:

```text
Public Social Network
        ↓
Many Users
        ↓
Many Interactions
```

Memento focuses on:

```text
User A
   ↕
User B
   ↓
Shared Moments
```

The opportunity is to make the relationship itself the center of the application.

---

# 6. Our Solution

## Daily Shared Moment

Each connected pair has a dedicated shared space.

The main screen presents the current shared moments between the two users.

Example:

```text
┌─────────────────────────────┐
│        Our Moments          │
│                             │
│       Today · Aug 11        │
├─────────────────────────────┤
│                             │
│          Photo              │
│                             │
├─────────────────────────────┤
│                             │
│          Video              │
│                             │
├─────────────────────────────┤
│                             │
│          Photo              │
│                             │
└─────────────────────────────┘

        + Share Moment
```

The user can:

```text
View today's moments
        ↓
Share a new moment
        ↓
Partner receives it
        ↓
Moment appears in shared space
```

---

# 7. Core User Flow

The differentiating feature is built directly on top of the existing MVP sharing flow.

```text
Login
  ↓
Connect User
  ↓
1-to-1 Preview
  ↓
Daily Shared Moment
  │
  ├── View Today's Moments
  │
  └── Share Moment
          ↓
      Photo Picker
          ↓
      Media Preview
          ↓
          Send
          ↓
        Upload
          ↓
      Firebase
          ↓
    Realtime Update
          ↓
  Partner sees the moment
```

---

# 8. What Makes It Different

The feature is not simply:

> "Upload a photo."

The differentiation comes from the context in which the media is presented.

Instead of:

```text
Photo
```

Memento presents:

```text
Today
  ↓
Our shared moments
  ↓
Moments from both users
```

This makes the media feel like part of a shared memory rather than an isolated message.

---

# 9. MVP Scope

The feature must remain intentionally small.

## Included

### 1. Shared daily space

Each connected pair has one shared moment stream.

### 2. Today's moments

Users can see media shared during the current day.

### 3. Both users contribute

User A and User B can both send media.

```text
User A ──────┐
             │
             ▼
      Daily Shared Moment
             ▲
             │
User B ──────┘
```

### 4. Realtime update

When one user sends media, the other user sees the new moment without manually refreshing.

### 5. Existing media formats

The feature supports:

* Single Image
* Grid / Collage
* Carousel
* Video with thumbnail

These are already part of the Memento MVP media requirements.

---

# 10. Explicitly Out of Scope

To prevent the feature from expanding the MVP too much, the following are excluded.

### Not included

* Daily notifications
* Streak system
* Gamification
* Public sharing
* Likes
* Comments
* Reactions
* Multiple connections
* Group moments
* AI-generated memories
* Automatic daily recap
* Advanced calendar
* Search
* Advanced filters
* Automatic reminder
* Social ranking

These features may be considered in future versions.

---

# 11. Why Not "Daily Challenge"?

A daily challenge could require:

```text
Notification
      ↓
Daily deadline
      ↓
Camera
      ↓
Upload
      ↓
Streak
      ↓
Reward
```

This would significantly increase the MVP scope.

It would also move Memento closer to copying the behavioral model of existing social applications.

Daily Shared Moment instead focuses on the core value:

```text
Share
  ↓
See
  ↓
Remember
```

Therefore, it is more appropriate for the MVP.

---

# 12. Expected User Value

The feature provides several benefits.

## 12.1 Stronger Product Identity

Without the feature:

> Memento is an application for sending photos/videos.

With the feature:

> Memento is a private shared space for two people to keep their moments.

This gives the product a clearer identity.

---

## 12.2 Stronger Reason to Return

The user can return to the application to see:

```text
What did we share today?
```

rather than only opening the application when they need to send something.

---

## 12.3 Emotional Value

The product becomes centered around shared memories instead of generic social interactions.

The user relationship becomes the primary object.

---

## 12.4 Natural Extension of Existing MVP

The feature does not require an entirely new backend architecture.

It can reuse:

* User authentication
* User connection
* Media upload
* Firebase Storage
* Firestore
* Realtime updates
* History

Therefore, the feature has relatively high value compared with its implementation cost.

---

# 13. Technical Fit

The feature can be implemented using the existing Memento architecture.

High-level data relationship:

```text
User
  │
  └── Connection
          │
          └── Shared Moments
                  │
                  ├── Media
                  ├── Sender
                  ├── Timestamp
                  └── Media Type
```

Example conceptual Firestore structure:

```text
users/
    {userId}

connections/
    {connectionId}

sharedMoments/
    {momentId}
```

A shared moment could conceptually contain:

```text
momentId
connectionId
senderId
mediaType
mediaUrl
thumbnailUrl
createdAt
```

The exact schema will be defined during the technical implementation task.

---

# 14. Realtime Behavior

The feature should use Firestore realtime listeners.

Example:

```text
User A
   │
   │ Send photo
   ▼
Firebase Storage
   │
   │
   ▼
Firestore
   │
   │ Realtime update
   ▼
User B
   │
   ▼
Daily Shared Moment
```

User B should not need to manually refresh the screen.

---

# 15. UI Concept

The main screen should communicate the relationship clearly.

Possible structure:

```text
┌──────────────────────────────┐
│ ←        Our Moments         │
│                              │
│        User A + User B       │
│                              │
├──────────────────────────────┤
│                              │
│         TODAY                │
│                              │
│   ┌──────────────────────┐   │
│   │                      │   │
│   │       PHOTO          │   │
│   │                      │   │
│   └──────────────────────┘   │
│        User A · 09:32        │
│                              │
│   ┌──────────────────────┐   │
│   │       VIDEO ▶        │   │
│   └──────────────────────┘   │
│        User B · 12:45        │
│                              │
│                              │
│       + Share Moment         │
│                              │
└──────────────────────────────┘
```

The final UI will be defined during the UI design and implementation phase.

---

# 16. State Handling

The feature should support basic UI states.

## Loading

```text
Loading today's moments
```

## Empty

```text
No moments yet.

Share something with your connection.
```

## Content

```text
Today's shared moments
```

## Error

```text
Couldn't load today's moments.

[ Retry ]
```

These states are part of the existing MVP requirements.

---

# 17. Relationship With History

Daily Shared Moment and History have different purposes.

### Daily Shared Moment

Focus:

> What are we sharing today?

```text
Today
 ↓
Current moments
```

### History

Focus:

> What have we shared in the past?

```text
Today
Yesterday
Aug 10
Aug 9
...
```

Therefore:

```text
1-to-1 Preview
      │
      ├── Daily Shared Moment
      │
      └── History
```

The Daily Shared Moment should not replace History.

---

# 18. Feature Evaluation

Potential differentiating features were evaluated using four criteria:

* User value
* Concept fit
* Implementation complexity
* MVP feasibility

| Feature             | User Value | Concept Fit | Complexity |   MVP Fit |
| ------------------- | ---------: | ----------: | ---------: | --------: |
| Daily Shared Moment |       High |   Very High |     Medium | Very High |
| Memory Timeline     |       High |        High |     Medium |      High |
| Quick Reaction      |     Medium |      Medium |        Low |      High |
| Daily Streak        |     Medium |      Medium |     Medium |    Medium |
| AI Memory Summary   |       High |        High |  Very High |       Low |
| Advanced Filters    |     Medium |      Medium |     Medium |    Medium |

---

# 19. Why Daily Shared Moment Was Selected

Daily Shared Moment was selected because it provides the best balance between product value and implementation complexity.

### User Value

High.

It creates a reason for users to return and view shared moments.

### Concept Fit

Very high.

It directly supports Memento's core concept of private 1-to-1 memory sharing.

### Implementation Complexity

Medium.

Most of the required infrastructure already exists in the MVP:

* Authentication
* Connection
* Firebase
* Media upload
* Realtime updates
* History

### MVP Feasibility

High.

The feature can be implemented without introducing:

* New complex backend systems
* Machine learning
* Large-scale social features
* Complicated recommendation systems

---

# 20. Expected Value

The expected outcome is that Memento becomes more than a simple media-transfer application.

### Before

```text
User A
   ↓
Send Photo
   ↓
User B
```

### After

```text
User A
   ↓
Share Moment
   ↓
Shared Space
   ↓
User B
   ↓
Both users see
their moments together
```

The product therefore shifts from:

> **Media sharing**

to:

> **Private shared memories**

---

# 21. MVP Success Criteria

The differentiating feature is considered successfully implemented when:

### Scenario 1 — User A sends a moment

```text
User A
 ↓
Open 1-to-1 Preview
 ↓
Share Moment
 ↓
Select Photo
 ↓
Preview
 ↓
Send
 ↓
Moment appears in Daily Shared Moment
```

### Scenario 2 — User B receives the moment

```text
User B
 ↓
Open Memento
 ↓
Daily Shared Moment
 ↓
New moment appears
```

### Scenario 3 — Both users contribute

```text
User A → Photo
User B → Video
User A → Photo
```

The shared space displays:

```text
Today's Moments

Photo      User A
Video      User B
Photo      User A
```

### Scenario 4 — No moments

```text
Daily Shared Moment

No moments yet.

Share your first moment.
```

---

# 22. MVP Feature Boundary

The feature should remain within the following boundary:

```text
┌───────────────────────────────────────┐
│          DAILY SHARED MOMENT          │
│                                       │
│  View today's moments                 │
│  Send photo/video                     │
│  Receive photo/video                  │
│  Realtime update                      │
│  Basic loading / empty / error state  │
│                                       │
└───────────────────────────────────────┘
```

Anything beyond this boundary should be considered post-MVP.

---

# 23. Final Decision

## Selected Differentiating Feature

**Daily Shared Moment**

### Product Statement

> Memento provides a private shared space where two connected people can quickly exchange and revisit the moments they share during the day.

### Core Value

```text
Private
   +
Simple
   +
Realtime
   +
Memorable
```

### MVP Flow

```text
Connect
   ↓
Share
   ↓
Receive
   ↓
See Together
   ↓
Remember
```

This feature strengthens Memento's identity without significantly expanding the core MVP architecture or user flow.

---

# 24. Future Expansion

After the MVP is stable, Daily Shared Moment can be extended with:

* Daily recap
* Memory timeline
* Calendar view
* Favorite moments
* Reactions
* Moment comments
* Shared albums
* Smart memory summaries
* AI-generated highlights
* Notifications
* Streaks

These features are intentionally postponed until the core experience is validated.