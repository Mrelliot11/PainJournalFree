# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

PainTrackerFree is a free, offline Android pain journal app for chronic pain patients. All data is stored locally using Room. No network permissions or backend required.

## Build Commands

Run from the project root (`C:/Users/Sniper/Desktop/PainTrackerFree/`):

```bash
# Assemble debug APK
./gradlew assembleDebug

# Run unit tests
./gradlew test

# Run instrumented tests (requires connected device/emulator)
./gradlew connectedAndroidTest

# Run a single unit test class
./gradlew test --tests "com.example.paintrackerfree.ExampleUnitTest"

# Check for lint errors
./gradlew lint

# Clean build
./gradlew clean assembleDebug
```

## AGP 9.x compileSdk Syntax

This project uses AGP 9.1.0. The `compileSdk` block uses a new DSL — **do not change this syntax**:

```kotlin
compileSdk {
    version = release(36) {
        minorApiLevel = 1
    }
}
```

`targetSdk = 36` remains a plain integer in `defaultConfig`.

## Dependency Management

All versions are declared in `gradle/libs.versions.toml`. Add new dependencies there first, then reference via `libs.*` aliases in `app/build.gradle.kts`. Use **KSP** (not kapt) for annotation processors — kapt is deprecated with Kotlin 2.x.

## Architecture

**Single-activity** app using the Navigation Component. `MainActivity` hosts one `NavHostFragment` and a `BottomNavigationView`. Bottom nav has 3 top-level destinations; the Log Entry screen is a 4th non-top-level destination that hides the bottom nav.

```
MainActivity
├── NavHostFragment
│   ├── HomeFragment          (today's stats + recent entries + FAB)
│   ├── HistoryFragment       (full list with date headers, swipe-to-delete)
│   ├── ReportsFragment       (chart + stats + CSV/PDF export)
│   └── LogEntryFragment      (add/edit form; receives entryId: Long arg, 0 = new)
└── BottomNavigationView      (hidden when LogEntryFragment is active)
```

**MVVM** throughout. Each Fragment has a paired ViewModel that takes `PainRepository` via `ViewModelFactory`. The `PainTrackerApp` Application subclass holds the `PainDatabase` and `PainRepository` singletons (lazy-initialized); Fragments access them via `requireActivity().application as PainTrackerApp`.

**Data flow:** Room DAO → Flow → Repository → `asLiveData()` in ViewModel → observed in Fragment via `viewLifecycleOwner`.

## Package Structure

```
com.example.paintrackerfree/
  PainTrackerApp.kt            Application; exposes .repository singleton
  MainActivity.kt
  data/
    model/PainEntry.kt         @Entity; multi-select fields stored as comma-joined strings directly (no TypeConverters)
    db/PainDatabase.kt         @Database singleton
    db/PainEntryDao.kt         @Dao; all queries return Flow or suspend
    repository/PainRepository.kt
  ui/
    home/                      HomeFragment + HomeViewModel
    logentry/                  LogEntryFragment + LogEntryViewModel
    history/                   HistoryFragment + HistoryViewModel + HistoryAdapter
    reports/                   ReportsFragment + ReportsViewModel + PainChartView
  util/
    DateUtils.kt               Timestamp formatting helpers
    CsvExporter.kt             Writes CSV to exports/ subdir, returns FileProvider URI; also saves to Downloads
    CsvImporter.kt             Parses a CSV URI into List<PainEntry>; returns sealed Result
    PdfExporter.kt             Renders entries to a PDF via android.graphics.pdf.PdfDocument; share intent + save to Downloads
    CustomOptionsStore.kt      SharedPreferences store for user-added chip options (locations, pain types, triggers)
    WindowInsets.kt            applyStatusBarPadding() extension on View
    ViewModelFactory.kt        ViewModelProvider.Factory wrapping PainRepository
```

## Key Conventions

- **ViewBinding** is enabled. Every Fragment uses the `_binding`/`binding` nullable pattern; always null out in `onDestroyView`.
- **Navigation IDs must match**: bottom nav menu item IDs in `res/menu/bottom_nav_menu.xml` must exactly match the destination IDs in `res/navigation/nav_graph.xml` for `NavigationUI.setupWithNavController` to work.
- **LogEntry nav argument**: `entryId: Long`, default `0L`. Value `0` means new entry; any positive value means edit.
- **Multi-select chips** (body locations, pain types, triggers) are stored as comma-separated strings directly on `PainEntry` — there is no `Converters.kt` or `@TypeConverter` class.
- **PainChartView** (`ui/reports/PainChartView.kt`) is a custom `View` drawn entirely with `Canvas`/`Paint`/`Path` — no external chart library.
- **Export files** are written to `getExternalFilesDir("exports")` and shared via FileProvider with authority `${packageName}.fileprovider`. The paths config is at `res/xml/file_paths.xml` (scoped to `exports/`).
- Theme is `Theme.MaterialComponents.DayNight.NoActionBar`. Each fragment owns its own `MaterialToolbar`.
