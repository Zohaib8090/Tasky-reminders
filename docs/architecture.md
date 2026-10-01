# Architecture

TaskNest is a single-activity Android app written in Kotlin with Jetpack Compose and Material 3. It is local-first: all data lives on the device.

- **Application ID:** `com.zohaib.tasknest`, source namespace `com.example`
- **Android:** minSdk 24 (7.0), target and compile SDK 36
- **CPU:** pure Kotlin/Java, no native code, so one universal APK

## Layers

```
Compose UI (screens, components, theme)
        |
TaskViewModel  (single ViewModel, StateFlow state)
        |
TaskRepository (encrypts/decrypts locked items)
        |
TaskDao / NoteDao  ->  AppDatabase (Room, version 5)
```

Side systems: `AlarmScheduler` (AlarmManager), `TaskSyncWorker` (WorkManager), `BackupManager`, `UpdateChecker`, `BiometricAuth`, `NoteCrypto`.

## Navigation and state
There is no nav graph. `MainActivity` shows one of four tabs (`NavTab`: Today, Calendar, Notes, Settings). Sheets, dialogs and the full-screen note editor are shown from boolean or nullable `StateFlow`s in `TaskViewModel`. Theme palette, dark mode and update preferences are kept in `SharedPreferences`.

## Data model
- `Task`: title, description, due date and time, priority, category, completion, checklist, attachments, reminder settings, `isLocked`.
- `Note`: optional `taskId`, title, content, colour index, checklist, attachments, bold/italic, `isLocked`.
- Checklists and attachments are stored in a custom delimited text format (not JSON) by `ChecklistItem` and `AttachmentItem` encode/decode helpers.

### Database migrations
Version 5 adds `isLocked` to both tables through `MIGRATION_4_5`, so existing data is kept. A destructive fallback exists for unknown version jumps, so **every schema change needs a real migration** or users lose data.

## Reminders
`AlarmScheduler` sets an exact alarm at the due time and an optional early alarm. Intents go to `TaskAlarmReceiver`, which shows the notification and handles Mark done and Snooze. `BootCompletedReceiver` re-arms alarms after a reboot or app update. `TaskSyncWorker` runs every 12 hours as upkeep. Locked tasks send an empty description in the notification.

## Locking flow
1. `Task.isLocked` / `Note.isLocked` mark an item as locked.
2. The repository encrypts a locked item's private fields on write and decrypts on read, so UI code sees plain text.
3. Lists and cards render a "Locked" placeholder for locked items.
4. `MainActivity` gates opening a locked note, task detail or edit sheet: it prompts via `BiometricAuth`, and on success records the id in `unlockedTaskIds` / `unlockedNoteIds` in the ViewModel. `onStop` clears both sets, so everything re-locks when the app leaves the foreground.
5. Deleting a locked item prompts again.

## Backup format
A zip containing `data.json`, plus `images/`, `videos/`, `audio/` and `files/`. If locked items exist, `data.json` has a `locked` block (salt, iterations, KDF), and each locked item carries a `sealed` value holding its private fields. Their media is stored as `locked/<path>.enc`. See [security.md](security.md).

## Source map
See [../PROJECT_INDEX.md](../PROJECT_INDEX.md).

## Tests
`BackupManagerTest` covers backup and restore without locked items. There are no tests yet for the lock, encryption or password-protected export, alarm scheduling, or the ViewModel.
