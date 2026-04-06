# CLAUDE.md

PainTrackerFree is a free, offline Android pain journal app. All data is stored locally via Room. No network permissions or backend.

## Build Commands

```bash
./gradlew assembleDebug
./gradlew test
./gradlew connectedAndroidTest
./gradlew lint
./gradlew clean assembleDebug
```

## AGP 9.x compileSdk Syntax

AGP 9.1.0 uses a new DSL — **do not change this**:

```kotlin
compileSdk {
    version = release(36) {
        minorApiLevel = 1
    }
}
```

`targetSdk = 36` stays a plain integer in `defaultConfig`.

## Dependency Management

Versions in `gradle/libs.versions.toml`; reference via `libs.*` in `app/build.gradle.kts`. Use **KSP** (not kapt) for annotation processors.

## Architecture

Single-activity, Navigation Component. `MainActivity` hosts `NavHostFragment` + `BottomNavigationView` (hidden on LogEntryFragment).

```
MainActivity
├── NavHostFragment
│   ├── HomeFragment       (today's stats + recent entries + quick-log FAB)
│   ├── HistoryFragment    (full list, search/filter, swipe-to-delete)
│   ├── ReportsFragment    (chart + stats)
│   ├── SettingsFragment   (theme, reminders, auto-backup, import/export, custom options)
│   └── LogEntryFragment   (add/edit; entryId: Long arg, 0 = new)
└── BottomNavigationView   (hidden when LogEntryFragment is active)
```

MVVM throughout. Each Fragment has a paired ViewModel taking `PainRepository` via `ViewModelFactory`. `PainTrackerApp` holds `PainDatabase` and `PainRepository` singletons; Fragments access via `requireActivity().application as PainTrackerApp`.

Data flow: Room DAO → Flow → Repository → `asLiveData()` in ViewModel → observed in Fragment via `viewLifecycleOwner`.

## Package Structure

```
com.example.paintrackerfree/
  PainTrackerApp.kt
  MainActivity.kt
  data/
    model/PainEntry.kt          @Entity; multi-select fields as comma-joined strings (no TypeConverters)
    db/PainDatabase.kt
    db/PainEntryDao.kt          all queries return Flow or suspend
    repository/PainRepository.kt
  ui/
    home/                       HomeFragment + HomeViewModel
    logentry/                   LogEntryFragment + LogEntryViewModel
    history/                    HistoryFragment + HistoryViewModel + HistoryAdapter
    reports/                    ReportsFragment + ReportsViewModel + PainChartView
    settings/                   SettingsFragment + SettingsViewModel
  util/
    DateUtils.kt
    CsvExporter.kt              exports/ subdir + Downloads; buildCsvBytes() for DocumentFile writes
    CsvImporter.kt              parses CSV URI → List<PainEntry>; sealed Result
    PdfExporter.kt              android.graphics.pdf.PdfDocument; renders passed entries only
    CustomOptionsStore.kt       SharedPreferences for user-added chip options
    AutoBackupStore.kt          SharedPreferences for auto-backup settings (enabled, frequency, folderUri)
    AutoBackupScheduler.kt      AlarmManager.setInexactRepeating; request code 9001
    ReminderScheduler.kt
    ReminderStore.kt
    ThemeStore.kt
    ViewModelFactory.kt
    WindowInsets.kt             applyStatusBarPadding() extension
  receiver/
    AutoBackupReceiver.kt       goAsync(); writes CSV to chosen folder or Downloads fallback
    BootReceiver.kt             reschedules reminders + auto-backup on boot
    ReminderReceiver.kt
  widget/
    QuickLogWidget.kt           1×1 AppWidgetProvider; tap opens quick-log sheet via ACTION_QUICK_LOG
```

## Key Conventions

- **ViewBinding**: every Fragment uses `_binding`/`binding`; null out in `onDestroyView`.
- **Nav IDs must match**: `res/menu/bottom_nav_menu.xml` IDs must match `res/navigation/nav_graph.xml` destination IDs.
- **LogEntry nav arg**: `entryId: Long`, default `0L`. `0` = new, positive = edit.
- **Multi-select chips** (locations, pain types, triggers): stored as comma-separated strings on `PainEntry` — no `@TypeConverter`.
- **PainChartView**: custom `View` with `Canvas`/`Paint`/`Path` — no chart library.
- **Export files**: `getExternalFilesDir("exports")` shared via FileProvider `${packageName}.fileprovider`; paths at `res/xml/file_paths.xml`.
- **Auto-backup folder**: user picks via `OpenDocumentTree`; URI persisted with `takePersistableUriPermission`; written via `DocumentFile.fromTreeUri`.
- **Widget/shortcut quick-log**: `ACTION_QUICK_LOG` intent → `MainActivity.handleQuickLog()` → sets `HomeViewModel.openQuickLogSheet = true` → `HomeFragment` shows bottom sheet.
- **MainActivity** `launchMode="singleTop"`; handles repeated widget/shortcut taps via `onNewIntent`.
- Theme: `Theme.MaterialComponents.DayNight.NoActionBar`. Each fragment owns its `MaterialToolbar`.
