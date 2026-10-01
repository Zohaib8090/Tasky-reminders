# TaskNest (Tasky-reminders) — Project Index

Native Android app (Kotlin, Jetpack Compose, Material 3) for tasks with reminders, rich notes, and a calendar view. Local-first: all data lives in a Room database; no backend sync.

- **Application ID:** `com.zohaib.tasknest` · **namespace:** `com.example`
- **SDK:** minSdk 24, target/compile 36 · **Version:** 1.0 (code 1)
- **Build:** AGP 9.1.1, Kotlin 2.2.10, KSP, JDK 11 source level (CI uses JDK 21)
- **Origin:** generated with Google AI Studio (`metadata.json`, `.env.example`, Secrets plugin)

## Architecture

Single-activity, single `TaskViewModel` (MVVM, no DI framework, no NavHost — tab state is a `StateFlow<NavTab>` and sheets/editor are shown via boolean StateFlows).

```
MainActivity ──> TaskViewModel ──> TaskRepository ──> TaskDao / NoteDao ──> AppDatabase (Room v4)
     │                 │
     │                 ├──> AlarmScheduler (AlarmManager exact alarms)
     │                 └──> BackupManager (zip export/import)
     └── Compose screens/components (observe ViewModel StateFlows)

AlarmManager ──> TaskAlarmReceiver ──> notification (Mark done / Snooze actions)
BOOT / package replaced ──> BootCompletedReceiver ──> AlarmScheduler.rescheduleAllActiveAlarms
WorkManager (12h) ──> TaskSyncWorker
```

## Source map (`app/src/main/java/com/example/`)

### Entry
| File | Role |
|---|---|
| `MainActivity.kt` | Hosts Compose; theme setup; requests POST_NOTIFICATIONS; routes `NavTab.TODAY/CALENDAR/NOTES/SETTINGS`; shows sheets, FAB (Add Task / Add Note), snackbar; handles `EXTRA_NAVIGATE_TASK_ID` from notification taps; enqueues `TaskSyncWorker`. |

### Data (`data/`)
| File | Role |
|---|---|
| `model/Task.kt` | `Task` entity (`tasks`): title, description, `dueDate` (day millis), `dueTime` "HH:mm", `Priority`, `Category`, `isCompleted`, `checklistJson`, `attachmentsJson`, reminder fields (`reminderEnabled`, `reminderMinutesBefore`), `createdAt`. Also `ChecklistItem` with `encodeList/decodeList` (custom `\|\|\|` delimited text, not JSON despite field names). |
| `model/Note.kt` | `Note` entity (`notes`, optional FK `taskId`): title, content, `colorIndex`, checklist/attachments text, bold/italic flags. `AttachmentItem` + `AttachmentType` (CHECKLIST, IMAGE, VIDEO, LINK, AUDIO, FILE) with custom encode/decode. |
| `local/AppDatabase.kt` | Room DB (version 4), singleton `getInstance`, seeds sample tasks/notes. |
| `local/TaskDao.kt`, `local/NoteDao.kt` | Flow queries + suspend CRUD; bulk insert/delete used by restore. |
| `repository/TaskRepository.kt` | Thin wrapper over DAOs; `clearAllData()`. |
| `backup/BackupManager.kt` | `createBackup` / `restoreBackup` to a zip (JSON for tasks/notes + copied media); `BackupResult` / `RestoreResult`. Covered by `BackupManagerTest`. |

### Notifications & background
| File | Role |
|---|---|
| `notifications/AlarmScheduler.kt` | `object`: schedules exact + early alarms, snooze (default 10 min), test alarm, cancel, `rescheduleAllActiveAlarms`. Intent actions `com.example.tasknest.ACTION_{TASK_REMINDER,MARK_DONE,SNOOZE}`. |
| `notifications/TaskAlarmReceiver.kt` | Posts reminder notification; handles Mark done / Snooze. |
| `notifications/BootCompletedReceiver.kt` | Re-arms alarms after boot / app update. |
| `workers/TaskSyncWorker.kt` | Periodic (12h) maintenance; re-schedules alarms for upcoming tasks. |

### UI (`ui/`)
| File | Role |
|---|---|
| `viewmodel/TaskViewModel.kt` (644 lines) | All app state: tab, category filter, search, calendar date, sheet/dialog visibility, note editor, theme palette + dark mode (SharedPreferences), backup UI state. Derived flows `todayTasks`, `calendarTasks`, `taskNotePreviews`. Task/note/checklist mutations and reminder toggling. `NavTab`, `BackupUiState`. |
| `screens/TodayScreen.kt` | Today list with search + category filter. |
| `screens/CalendarScreen.kt` | Month calendar + tasks for selected day. |
| `screens/NotesScreen.kt` | Notes grid/list. |
| `screens/NoteEditorScreen.kt` (2230 lines, largest file) | Full-screen rich editor: checklist, image/video/link/audio/file attachments, voice recording dialog, media preview. |
| `screens/AddTaskBottomSheet.kt` | Create/edit task. |
| `screens/TaskDetailBottomSheet.kt` | Task detail, checklist, notes, reminder controls. |
| `screens/SettingsScreen.kt` | Theme palettes, dark mode, backup/restore, reminder test, stats. |
| `components/` | `TaskCard`, `TaskSearchBar`, `FloatingPillBottomBar`, `SquircleFab`, `EmptyStateIllustration`. |
| `theme/` | `Theme.kt` (`TaskNestTheme`, `AppThemePalette`), `Color.kt` (category/priority colors), `Type.kt`, `Shape.kt`. |

### Util
- `util/AudioHelper.kt` — `MediaRecorder` creation, synthetic WAV / sample audio generation.

## Resources & config
- `AndroidManifest.xml` — permissions: POST_NOTIFICATIONS, SCHEDULE_EXACT_ALARM, USE_EXACT_ALARM, VIBRATE, RECORD_AUDIO, RECEIVE_BOOT_COMPLETED; one activity, two non-exported receivers.
- `res/` — launcher icons, `ic_tasknest_icon.jpg`, two sample images, colors/strings/themes, backup/data-extraction rules.
- `gradle/libs.versions.toml` — version catalog. Notable deps: Compose BOM 2024.09.00, Room 2.7.0, WorkManager 2.9.1, Navigation Compose, Coil, Retrofit/OkHttp/Moshi, Firebase BOM 34.17.0 (`firebase-ai`, App Check).
- `.env.example` — `GEMINI_API_KEY` placeholder (commented out). `metadata.json` declares server-side Gemini capability.

## Tests (`app/src/test`, `app/src/androidTest`)
- `BackupManagerTest` — backup/restore logic (the only real test).
- `ExampleUnitTest`, `ExampleRobolectricTest`, `GreetingScreenshotTest` (Roborazzi), `ExampleInstrumentedTest` — template stubs.

## CI (`.github/workflows/build-and-release.yml`)
Builds on push/PR to main/master, tags `v*`, or manual dispatch (debug/release/both). Generates/restores `debug.keystore`; release requires `KEYSTORE_BASE64`, `STORE_PASSWORD`, `KEY_PASSWORD` secrets. Uploads APKs as artifacts; publishes a GitHub Release on `v*` tags. No test or lint step.

## Observations
- Firebase AI, Retrofit/Moshi, OkHttp, Navigation Compose are declared but unused in source (no Gemini/network code yet); `google-services` plugin only warns if `google-services.json` is missing.
- Release build has minification disabled.
- Checklist/attachment "JSON" columns actually use a custom delimiter format; content containing `|||` or the separator would corrupt decoding (newlines are stripped on encode).
- `NoteEditorScreen`, `AddTaskBottomSheet`, `SettingsScreen`, `TaskDetailBottomSheet` are very large single composables — candidates for splitting.
- Manifest has `allowBackup="true"`; RECORD_AUDIO is declared but runtime request handling should be verified in the editor.
- Only `BackupManager` has meaningful test coverage; alarm scheduling and ViewModel logic are untested.
