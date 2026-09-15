# Building the scaffold

This is the UI scaffold for the app described in [README.md](README.md): a real Kotlin/Compose
Android project with every screen the README lists, rendered against synthetic data. There is no
Room, no Hilt and no repository layer yet — those land under the screens without moving them.

## Open it

Android Studio (Ladybug or newer) → **Open** → this folder. It bundles a JDK 17+ and will sync
Gradle on its own.

From a terminal you need JDK 17 or newer on `JAVA_HOME` (the system JDK on this machine is 15,
which AGP 8.7 rejects):

```bash
./gradlew :app:assembleDebug
```

`local.properties` points at your Android SDK and is machine-specific, so it is gitignored. After
cloning, Android Studio writes it for you on first sync; from a terminal, create it with a single
line: `sdk.dir=/path/to/Android/Sdk`.

## What is here

| Path | What it holds |
|---|---|
| `app/src/main/java/com/moneymanager/MainActivity.kt` | Entry point, and the direction contract the whole design answers to |
| `app/src/main/java/com/moneymanager/ui/Theme.kt` | The depth ladder, both colour schemes, the Archivo type scale, shapes |
| `app/src/main/java/com/moneymanager/ui/Water.kt` | `WaterField`, `WaterColumn`, `WaterBar`, the glass plate modifier |
| `app/src/main/java/com/moneymanager/ui/Charts.kt` | Ring, paired columns, sounding line, calendar grid, vessel, depth gauge — all drawn on Canvas |
| `app/src/main/java/com/moneymanager/ui/Components.kt` | `MoneyText`, `Plate`, `LedgerRow`, `Pill`, `Chip`, `NavRow`, empty states |
| `app/src/main/java/com/moneymanager/ui/Nav.kt` | Routes, navigation bar and rail, FAB, transitions |
| `app/src/main/java/com/moneymanager/ui/screens/` | Every screen |
| `app/src/main/java/com/moneymanager/data/Sample.kt` | Synthetic data. Delete this when Room lands. |

## Screens

Tabs: **Home**, **Ledger**, **Budgets**, **Reports**, **More**.

Under them: transaction editor and detail, budget detail, accounts and account detail, bills and
subscriptions, goals, debt payoff, capture (receipt / email / statement), progress and badges,
currencies, lock and privacy, backup and sync, settings, and the PIN lock screen.

## What is deliberately not wired

- **No persistence.** Screen state is `remember`-scoped and resets on rotation. Room is next.
- **No ViewModels.** Nothing has enough state yet to earn one; adding them now would only be
  boilerplate to rewrite when the repository arrives.
- **No Vico, no MPAndroidChart.** Every chart is a `Canvas` composable, because the design needs
  a ring with labels on the arc and a mirrored net-worth sounding, and because a chart library
  brings its own house style into a committed visual world.
- **No Material You.** The depth palette is what makes the waterline legible; a wallpaper-derived
  scheme reorders it. It is offered as an explicit opt-in in Settings → Appearance.
- **Pinned clock.** `data/Sample.kt` fixes "today" at 12 September 2026 so previews and
  screenshots are stable. Swap it for the system clock when the data layer lands.

## Verified, and not

**Verified:** the project compiles. AGP 8.7.3, Kotlin 2.0.21, compileSdk 35, minSdk 26, built
with a JDK 17 fetched for the purpose (the JDK on this machine is 15, which AGP 8.7 rejects).
Colour contrast was checked numerically: every foreground/background token pair in both schemes
clears 4.5:1.

**Not verified:** anything that needs a screen. This machine has no AVD, no system image and no
emulator package, so no capture pass ran against the built APK and no claim here covers rendered
pixels. Before shipping, run it and check the five things a static build cannot show you:

1. The Home hero at font scale 1.3 (`adb shell settings put system font_scale 1.3`, restore
   `1.0` after) — the 54sp balance figure is the most likely thing to clip.
2. Dark and light (`adb shell cmd uimode night yes|no`). Both are full schemes, not inversions.
3. The water swell with "Remove animations" on and off, and the level rise on first open.
4. The ring chart's arc labels on a narrow screen — wedges under 26 degrees drop their label by
   design and fall back to the list beneath.
5. Predictive Back on Android 13+, and the keyboard not covering the note or search fields.
6. The status bar over the Home hero. The shell no longer applies the top inset — the hero
   runs under the clock and pays the inset internally — so check that the system icons stay
   legible against the water in both themes, and that no other screen renders under the clock.

7. The Goals row. The photographs are arbitrary stock keyed off seed strings, so check that the
   water level reads clearly over whatever the image happens to be, in both themes. Provenance and
   the replacement path are in `.impeccable/assets/provenance.md`.

Capture with `adb exec-out screencap -p > shot.png` into `.impeccable/review/` if you want the
finish review re-run against real pixels.
