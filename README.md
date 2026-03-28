# DoubleTake

An Android app that automatically syncs local folders on your device with Google Drive. Set up a sync pair once, and DoubleTake handles the rest in the background.

> **Status: Work in progress.** Core sync engine and UI are functional. Some rough edges remain.

---

## The problem it solves

Android doesn't have a native way to automatically keep a local folder in sync with a Google Drive folder (bidirectionally). DoubleTake fills that gap — you pick a local folder, pick a Drive folder, and it keeps them in sync.

---

## Features

- **Sync pairs** — Link any local folder to any Google Drive folder
- **Background sync** — Runs via WorkManager; survives app restarts
- **Bidirectional sync** — Uploads new/changed local files, downloads new/changed remote files
- **Conflict resolution** — Newest file wins; true conflicts (same timestamp, different content) produce a dated conflict copy instead of silently overwriting
- **Deletion handling** — Tracks deletes on both sides via a local DB; won't re-upload files the other side intentionally removed
- **Duplicate pair guard** — Prevents you from creating the same local + Drive folder pairing twice

---

## Tech stack

| Layer           | Libraries                           |
|-----------------|-------------------------------------|
| UI              | Jetpack Compose, Material 3         |
| Navigation      | Navigation Compose                  |
| DI              | Hilt                                |
| Local storage   | Room, DataStore                     |
| Background work | WorkManager (Hilt-integrated)       |
| Cloud           | Google Drive API v3, Google Sign-In |
| Language        | Kotlin (coroutines + Flow)          |
| Min SDK         | Android 10 (API 29)                 |

Architecture follows a standard Clean Architecture layering: `presentation` → `domain` → `data`.

---

## Getting started

### Prerequisites

- Android Studio Hedgehog or newer
- A Google Cloud project with the **Google Drive API** enabled
- An OAuth 2.0 **Android client ID** configured for your app's package name and signing certificate

### Google Cloud setup

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a project (or use an existing one)
3. Enable the **Google Drive API**
4. Under **Credentials**, create an **OAuth 2.0 Client ID** of type **Android**
   - Package name: `com.pratheekbhat.doubletake`
   - SHA-1: run `./gradlew signingReport` to get your debug SHA-1
5. No `google-services.json` is needed — Drive API auth is handled directly via `GoogleSignIn`

### Build and run

```bash
git clone https://github.com/PratheekRBhat/double-take.git
cd double-take
```

Open in Android Studio, let Gradle sync, then run on a device or emulator (API 29+).

> Note: Google Sign-In will only work on a real device or an emulator with Google Play Services installed and a Google account added.

---

## How it works

1. **Setup** — Sign in with Google, choose a local folder, choose (or create) a Drive folder. This creates a *sync pair*.
2. **Diff** — On each sync, DoubleTake compares local files, remote Drive files, and a local DB snapshot. It determines the action for each file: `UPLOAD`, `DOWNLOAD`, `TRASH_LOCAL`, `TRASH_REMOTE`, `CONFLICT`, or `NO_OP`.
3. **Conflict resolution** — If both sides changed a file:
   - Newest modification time wins
   - If timestamps are within 2 seconds of each other, a conflict copy is created (e.g., `file (conflict 2025-03-28).txt`)
4. **Sync worker** — A `CoroutineWorker` runs the sync. Transient failures trigger a retry; auth failures fail permanently.

---

## Project structure

```
app/src/main/java/com/pratheekbhat/doubletake/
├── data/
│   ├── local/          # Room DB, DAOs, DataStore
│   ├── remote/         # Drive API client
│   └── repository/     # Repository implementations
├── di/                 # Hilt modules
├── domain/
│   ├── model/          # Domain models (SyncAction, SyncResult, etc.)
│   ├── repository/     # Repository interfaces
│   └── usecase/        # Business logic (SyncDiffer, ConflictResolver, PerformSyncUseCase)
├── presentation/
│   ├── dashboard/      # Sync pairs list, cards
│   ├── navigation/     # Nav host, bottom bar, routes
│   └── setup/          # Onboarding flow, Drive folder picker
├── ui/theme/           # Colors, typography, theme
└── worker/             # SyncWorker, SyncWorkManager
```

---

## License

MIT
