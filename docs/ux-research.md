# Memento UX Research

## 1. Research Goal

The goal of this research is to study existing social media and private-sharing applications and identify UI/UX patterns that can be applied to Memento.

The research focuses on applications that provide strong experiences around:

* Sharing photos and videos
* Selecting and previewing media
* Viewing memories and previous content
* Realtime or near-realtime interactions
* Simple navigation
* Loading, empty, and error states
* Media-focused interactions

The applications selected for reference are:

* Locket
* BeReal
* Instagram

The goal is **not to copy the UI of these applications**, but to understand the problems they solve, identify reusable UX patterns, and determine which patterns are appropriate for Memento.

---

# 2. Product Context

Memento is a small 1-to-1 social media application focused on sharing personal moments between two connected users.

The core experience is:

```text
Authentication
      ↓
Connect User
      ↓
1-to-1 Preview
      ↓
Select Photo / Video
      ↓
Preview Media
      ↓
Send
      ↓
Realtime Update
      ↓
Receiver Sees Media
      ↓
History
```

Because Memento is designed around a private 1-to-1 relationship, the UX should prioritize:

1. Simplicity
2. Fast media sharing
3. Clear confirmation before sending
4. Easy access to shared memories
5. Minimal navigation complexity
6. Clear feedback during asynchronous operations

---

# 3. Research Method

The research is organized around the following UX areas:

| Area            | Questions                                         |
| --------------- | ------------------------------------------------- |
| Media Sharing   | How does the app let users share moments?         |
| Media Selection | How does the user choose photos/videos?           |
| Media Preview   | Can the user verify media before sharing?         |
| Multiple Photos | How are multiple images selected and presented?   |
| Video           | How are videos represented and played?            |
| History         | How can users access previous moments?            |
| Loading         | What happens while content is loading?            |
| Empty State     | What happens when there is no content?            |
| Error State     | How does the app recover from failures?           |
| Navigation      | How does the user move between core destinations? |
| Animation       | Which transitions improve the experience?         |

The research evaluates each pattern using:

* Observation
* Advantages
* Potential disadvantages
* Applicability to Memento
* Final decision

---

# 4. Reference Application: Locket

## 4.1 Why Locket is Relevant

Locket is particularly relevant because its product concept focuses on sharing personal moments with a small number of people rather than broadcasting content to a large social network.

This is conceptually close to Memento's 1-to-1 sharing model.

The most useful areas to study are:

* Personal/private sharing
* Camera and media interaction
* Minimal interaction flow
* Recent moments
* Relationship-oriented presentation

---

## 4.2 Sharing Experience

### Observation

Locket emphasizes quickly sharing moments with connected people.

The interaction is intentionally simple and focused on the content itself rather than additional social actions.

### Advantages

* Low interaction cost
* Easy to understand
* Keeps the focus on the moment
* Appropriate for private sharing

### Potential Disadvantages

* A very simplified flow may provide less control over the media before sending.
* Users may need confirmation when selecting existing media instead of taking a new photo.

### Memento Decision

**ADAPT**

Memento should maintain a short sharing flow but provide an explicit media preview before sending.

```text
Select Media
     ↓
Preview
     ↓
Send
```

This keeps the flow simple while reducing accidental sharing.

---

## 4.3 Private Relationship UX

### Observation

The experience is centered around sharing with a small group of connected people rather than an open public feed.

### Memento Decision

**ADOPT THE PRINCIPLE**

Memento should maintain a strong sense of private sharing.

The main screen should prioritize:

```text
Current Connection
       ↓
Shared Moments
       ↓
Send Moment
```

rather than introducing a public social feed.

---

# 5. Reference Application: BeReal

## 5.1 Why BeReal is Relevant

BeReal is useful for studying:

* Moment-oriented sharing
* Fast camera interaction
* Simple media creation
* Minimal UI
* Content-first experience

---

## 5.2 Media Capture Flow

### Observation

The sharing experience is designed around reducing the distance between the user and the moment they want to share.

### Advantages

* Fast
* Simple
* Content-focused
* Low cognitive load

### Memento Decision

**ADAPT**

Memento should avoid unnecessary steps in the media-sharing flow.

The MVP should use:

```text
1-to-1 Preview
      ↓
Send Media
      ↓
Android Photo Picker
      ↓
Media Preview
      ↓
Send
```

The MVP should not introduce unnecessary editing, captions, or configuration screens.

---

## 5.3 Content-First Design

### Observation

The media itself is the primary element of the experience.

### Memento Decision

**ADOPT**

Memento should prioritize media over secondary information.

For example:

```text
┌────────────────────────┐
│                        │
│                        │
│        PHOTO           │
│                        │
│                        │
└────────────────────────┘

Today · 14:32
```

Metadata should remain secondary to the media.

---

# 6. Reference Application: Instagram

Instagram provides useful patterns for media-heavy applications.

The research focuses on:

* Media selection
* Multiple image selection
* Carousel
* Grid presentation
* Video thumbnails
* Loading states
* Navigation
* Media interaction

---

# 7. Media Selection

## Observation

A mature media-sharing application needs to make media selection understandable and predictable.

Important UX principles include:

* Clearly indicate selected media
* Make the next action obvious
* Preserve the user's selection
* Allow the user to review the selected content

---

## Memento Decision

**ADOPT**

For the MVP, Memento will use the Android system Photo Picker.

```text
1-to-1 Preview
      ↓
Send Media
      ↓
Android Photo Picker
      ↓
Media Selected
      ↓
Media Preview
```

A custom gallery is not necessary for the MVP.

### Reason

Using the system Photo Picker:

* Reduces implementation complexity
* Provides a familiar Android experience
* Avoids unnecessary permission management
* Allows the project to focus on the core sharing flow

---

# 8. Media Preview

## Observation

A preview step gives users an opportunity to verify their selected media before performing an irreversible action.

This is particularly important when sharing personal photos or videos.

---

## Memento Decision

**ADOPT**

Memento will use an explicit preview step.

```text
Photo Picker
     ↓
Media Preview
     ↓
┌───────────────┐
│               │
│     Media     │
│               │
└───────────────┘
     │
 ┌───┴────┐
 ▼        ▼
Cancel    Send
```

### Reason

Memento focuses on private personal moments.

Accidentally sending the wrong photo or video should be prevented whenever possible.

The preview provides a clear confirmation point without significantly increasing interaction complexity.

---

# 9. Multiple Photo Layouts

Memento requires three photo presentation formats:

1. Single Image
2. Grid / Collage
3. Carousel

---

## 9.1 Single Image

When only one image exists:

```text
┌────────────────────┐
│                    │
│       IMAGE        │
│                    │
└────────────────────┘
```

### Decision

**ADOPT**

Use a large media-focused layout.

---

## 9.2 Grid / Collage

When multiple images need to be displayed together:

```text
┌─────────┬─────────┐
│         │         │
│ Image 1 │ Image 2 │
│         │         │
├─────────┼─────────┤
│         │         │
│ Image 3 │ Image 4 │
│         │         │
└─────────┴─────────┘
```

### Decision

**ADAPT**

Grid layouts are useful when users want to see several images at once.

Memento should use a grid when:

* Multiple photos belong to the same moment
* Quick overview is more important than detailed viewing

---

## 9.3 Carousel

```text
┌────────────────────┐
│                    │
│      Image 1       │
│                    │
└────────────────────┘
        ● ○ ○
```

### Decision

**ADOPT**

Carousel is appropriate when each image deserves more viewing space.

Memento can use carousel presentation when users need to swipe through multiple photos.

---

# 10. Video Experience

## Observation

Video content generally requires more resources than static images.

Displaying a video thumbnail before playback helps users understand the content without immediately starting playback.

---

## Memento Decision

**ADOPT**

Videos should have a generated thumbnail.

```text
┌────────────────────┐
│                    │
│     THUMBNAIL      │
│                    │
│        ▶           │
└────────────────────┘
```

The video should only start playback when the user interacts with it.

### Reason

This reduces unnecessary work and provides a predictable browsing experience.

Advanced video preloading will be implemented later.

---

# 11. History / Memories

## Observation

Memento's purpose is not only to send media but also to allow users to revisit previously shared moments.

A history screen should therefore make chronological browsing simple.

---

## Memento Decision

**ADOPT**

History will use a chronological presentation.

```text
History

Today
──────────────
[ Media ]

Yesterday
──────────────
[ Media ]

Aug 10
──────────────
[ Media ]
```

### Reason

Chronological organization is intuitive for memories.

It also provides a natural foundation for future features such as:

* Date grouping
* Memory summaries
* Search
* Favorite moments

These features are outside the MVP scope.

---

# 12. Loading State

## Problem

Media and Firebase data are asynchronous.

Without a loading state, the user may see an empty screen and incorrectly assume that the application is broken.

---

## Memento Decision

**ADOPT**

Screens that load remote data should explicitly represent the loading state.

Example:

```text
Loading
   ↓
Content
```

For media:

```text
┌────────────────────┐
│                    │
│    Loading...      │
│                    │
└────────────────────┘
```

A skeleton or placeholder may be introduced where appropriate.

### MVP Approach

The MVP can start with simple loading indicators/placeholders.

More advanced skeleton loading can be implemented during the optimization stage.

---

# 13. Empty State

## Problem

Some screens naturally have no data.

For example, a new user may not have shared any moments yet.

An empty screen should explain the situation and provide a useful next action.

---

## Memento Decision

**ADOPT**

Example:

```text
History

No moments yet.

Start sharing your first moment
with your connection.

[ Send a Moment ]
```

### Principle

An empty state should answer:

1. Why is this empty?
2. What can the user do next?

---

# 14. Error State

## Problem

Media sharing depends on:

* Network connectivity
* Firebase
* Storage
* File access
* Media processing

Failures are therefore expected.

---

## Memento Decision

**ADOPT**

Errors should be understandable and recoverable.

Example:

```text
Couldn't send this moment.

[ Retry ]   [ Cancel ]
```

Instead of exposing technical errors such as:

```text
FirebaseNetworkException
HTTP 500
```

the UI should present a user-friendly message.

Technical details should remain in logs/debugging tools.

---

# 15. Retry Interaction

For the MVP, retry should initially be manual.

```text
Upload
  ↓
Failed
  ↓
Error UI
  ↓
Retry
  ↓
Upload again
```

### Decision

**ADOPT**

Automatic retry and network-aware retry queues are considered post-MVP.

### Reason

Manual retry is simpler and sufficient for demonstrating the basic failure recovery flow.

---

# 16. Navigation

## Observation

Memento has significantly fewer core destinations than a large social network.

The MVP does not need complex navigation.

---

## Memento Decision

**ADAPT**

The MVP will use simple navigation rather than a large bottom-navigation structure.

Core flow:

```text
Splash
  ↓
Login / Register
  ↓
1-to-1 Preview
  ├── User Connection
  ├── Media Preview
  ├── Conversation
  └── History
```

### Reason

Adding unnecessary navigation destinations increases cognitive load and implementation complexity.

The MVP should focus on the core sharing experience.

---

# 17. Animation and Transition

## Observation

Animations can improve perceived continuity between screens and media states.

However, excessive animation can make a media-sharing application feel slow.

---

## Memento Decision

**ADAPT**

Use subtle animations for:

* Screen transitions
* Media appearance
* Loading → content transitions
* New realtime media appearing

Avoid unnecessary animations during the core send flow.

### Principle

Animation should communicate a state change rather than exist only for decoration.

---

# 18. Media Interaction

Memento should use familiar interactions whenever possible.

### Image

```text
Tap
 ↓
View / Expand
```

### Carousel

```text
Swipe left/right
```

### Video

```text
Tap
 ↓
Play
```

### Failed Media

```text
Tap Retry
 ↓
Reload
```

The MVP should avoid introducing unfamiliar gestures.

---

# 19. UX Pattern Comparison

| UX Area          | Locket         | BeReal         | Instagram | Memento     |
| ---------------- | -------------- | -------------- | --------- | ----------- |
| Private sharing  | Strong         | Moderate       | Weak      | Adopt       |
| Fast sharing     | Strong         | Strong         | Moderate  | Adopt       |
| Media preview    | Simple         | Simple         | Strong    | Adopt       |
| Media picker     | Camera-focused | Camera-focused | Strong    | Adapt       |
| Multiple images  | Limited        | Limited        | Strong    | Adapt       |
| Carousel         | Limited        | Limited        | Strong    | Adopt       |
| Video            | Limited        | Moderate       | Strong    | Adopt       |
| History          | Strong         | Moderate       | Strong    | Adopt       |
| Loading feedback | Simple         | Simple         | Strong    | Adopt       |
| Empty state      | Minimal        | Minimal        | Moderate  | Adapt       |
| Error recovery   | Simple         | Simple         | Strong    | Adapt       |
| Navigation       | Minimal        | Moderate       | Complex   | Keep simple |
| Animation        | Subtle         | Subtle         | Rich      | Keep subtle |

---

# 20. Adopt / Adapt / Reject

## Adopt

The following patterns should be directly adopted conceptually:

* Media preview before sending
* Clear loading states
* Meaningful empty states
* User-friendly error messages
* Manual retry
* Chronological history
* Video thumbnails
* Simple media-focused interaction
* Subtle transitions

---

## Adapt

The following patterns should be adapted to Memento's 1-to-1 model:

### Media Picker

Use Android Photo Picker instead of building a custom gallery.

### Multiple Images

Support:

* Grid
* Carousel

but keep the interaction simpler than a full social media publishing flow.

### Navigation

Use a smaller navigation structure because Memento has fewer destinations.

### Empty State

Use actionable empty states while keeping the UI minimal.

---

## Reject for MVP

The following patterns are intentionally excluded from the MVP:

* Public social feed
* Followers/following system
* Complex post editing
* Captions
* Hashtags
* Comments
* Public likes
* Stories
* Complex notifications
* Advanced content discovery
* Complex social graph

### Reason

These features do not contribute directly to the core Memento MVP:

> Two connected users sharing and viewing personal moments.

---

# 21. Memento UX Recommendations

Based on the research, the following UX principles are recommended for Memento.

## Recommendation 1 — Keep Sharing Fast

The core sharing flow should be short:

```text
1-to-1 Preview
      ↓
Photo Picker
      ↓
Preview
      ↓
Send
```

Avoid unnecessary configuration before sending.

---

## Recommendation 2 — Always Preview Before Sending

Because the content may be personal, users should explicitly confirm the media before sharing.

```text
Select
  ↓
Preview
  ↓
Send
```

---

## Recommendation 3 — Make Media the Primary UI Element

The media should receive the most visual attention.

Secondary information such as:

* Timestamp
* Sender
* Upload status

should not compete with the media itself.

---

## Recommendation 4 — Make Asynchronous States Visible

Whenever the application performs a network operation, the user should know what is happening.

```text
Idle
 ↓
Uploading
 ↓
Success
```

or:

```text
Uploading
 ↓
Failed
 ↓
Retry
```

---

## Recommendation 5 — Make Empty States Actionable

Instead of showing an empty screen, tell the user what they can do next.

Example:

```text
No moments yet.

Share your first moment.

[ Send a Moment ]
```

---

## Recommendation 6 — Keep Navigation Minimal

The MVP should not imitate Instagram's navigation complexity.

The core destinations are:

```text
1-to-1 Preview
Conversation
History
```

Additional screens should be introduced only when they solve a real user need.

---

# 22. Proposed MVP UX

The final proposed UX for Memento is:

```text
                         ┌──────────────┐
                         │    Splash    │
                         └──────┬───────┘
                                │
                         Authenticated?
                           /          \
                         No            Yes
                         ↓              ↓
                      Login       1-to-1 Preview
                         │              │
                      Register          │
                         │        ┌─────┼──────┐
                         │        │     │      │
                         └────────┘     │      │
                                      Send   History
                                       │       │
                                       ▼       ▼
                                Photo Picker History
                                       │
                                       ▼
                                 Media Preview
                                       │
                                  ┌────┴────┐
                                  │         │
                                Cancel     Send
                                            │
                                            ▼
                                         Upload
                                            │
                                            ▼
                                      Conversation
                                            │
                                            ▼
                                     Realtime Media
```

---

# 23. Proposed UI States

Each important asynchronous screen should follow a consistent state model.

```text
                    ┌─────────┐
                    │  Idle   │
                    └────┬────┘
                         │
                         ▼
                    ┌─────────┐
                    │ Loading │
                    └────┬────┘
                         │
                 ┌───────┴────────┐
                 ▼                ▼
            ┌─────────┐      ┌─────────┐
            │ Content │      │  Empty  │
            └─────────┘      └─────────┘
                 │
                 │ failure
                 ▼
            ┌─────────┐
            │  Error  │
            └────┬────┘
                 │
                 ▼
               Retry
```

This state model will later be represented using Kotlin state classes/sealed classes in the ViewModel layer.

---

# 24. UX Feature Opportunity

The research suggests that Memento should differentiate itself through the concept of a **private shared memory space** rather than attempting to become another general-purpose social network.

## Proposed Feature: Shared Moment

A connected pair can have a dedicated stream of moments shared only between them.

Concept:

```text
┌────────────────────────────┐
│       Our Moments          │
├────────────────────────────┤
│                            │
│         Photo              │
│                            │
├────────────────────────────┤
│         Video              │
│                            │
├────────────────────────────┤
│         Photo              │
│                            │
└────────────────────────────┘
```

### Value

This feature reinforces the core identity of Memento:

> A private place for two people to keep and share moments together.

It does not require a complicated social graph and can be built directly on top of the existing 1-to-1 sharing architecture.

---

# 25. MVP UX Decisions Summary

| Decision              | Status         | Reason                       |
| --------------------- | -------------- | ---------------------------- |
| Android Photo Picker  | Adopt          | Native and simple            |
| Media Preview         | Adopt          | Prevent accidental sharing   |
| Single Image          | Adopt          | Core media format            |
| Grid                  | Adopt          | Overview of multiple images  |
| Carousel              | Adopt          | Detailed multi-image viewing |
| Video Thumbnail       | Adopt          | Efficient browsing           |
| Chronological History | Adopt          | Natural memory organization  |
| Loading State         | Adopt          | Clear async feedback         |
| Empty State           | Adopt          | Explain what to do next      |
| Error + Retry         | Adopt          | Recover from failures        |
| Simple Navigation     | Adopt          | Keep MVP focused             |
| Subtle Animation      | Adopt          | Improve continuity           |
| Public Feed           | Reject         | Outside 1-to-1 scope         |
| Likes / Comments      | Reject         | Not required for core flow   |
| Complex Editing       | Reject         | Increases interaction cost   |
| Advanced ML           | Reject for MVP | Post-MVP                     |
| Advanced Caching      | Reject for MVP | Post-MVP                     |
| Automatic Retry       | Reject for MVP | Post-MVP                     |

---

# 26. Conclusion

The research indicates that the most important UX characteristic for Memento should be **simplicity around private media sharing**.

Memento should not attempt to reproduce the complete feature set of Instagram, Locket, or BeReal.

Instead, it should combine selected patterns:

```text
Locket
→ Private relationship-focused sharing

BeReal
→ Fast and simple moment interaction

Instagram
→ Strong media selection and presentation patterns
```

into a focused 1-to-1 experience:

```text
Connect
   ↓
Select
   ↓
Preview
   ↓
Send
   ↓
Realtime Receive
   ↓
Remember
```

The MVP should therefore prioritize:

1. Fast media sharing
2. Explicit media confirmation
3. Clear media presentation
4. Simple navigation
5. Reliable feedback for loading/error states
6. Easy access to previous moments

Advanced features such as caching, preload, performance optimization, and on-device ML should be introduced only after this core experience is stable.

The main UX principle for Memento is:

> **Make sharing a moment between two people feel fast, personal, and effortless.**
