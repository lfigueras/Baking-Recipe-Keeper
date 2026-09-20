# 🧁 Baking Recipe Keeper

A warm, easy-to-use baking recipe app for Android — keep every recipe, ingredient, and step in one place. Built entirely with **Kotlin** and **Jetpack Compose**.

> Your personal baking companion: save recipes, scale servings on the fly, and follow a clean step-by-step procedure.

---

## 📲 Download & try it

Grab the latest APK from the **[Releases page](https://github.com/lfigueras/Baking-Recipe-Keeper/releases/latest)**.

1. Open the link on an **Android** phone (Android 8.0 / API 26+) and download the `.apk`.
2. Open the file; if prompted, allow **install from unknown sources**.
3. Play Protect may note it's from an unknown developer (normal for sideloaded apps) — tap **Install anyway**.

> ⚠️ Android only — APKs don't run on iPhone.

---

## ✨ Features

- **Recipes** with photo, category, description, servings, prep/bake time, and difficulty
- **Ingredients** with amounts and units (g, kg, ml, cups, tbsp, tsp, pieces…)
- **Baking procedure** — numbered steps with **hold-and-drag reordering**
- **Start Baking mode** — distraction-free, step-by-step guided view
- **Live scaling** — change servings and every ingredient amount recalculates
- **Unit conversion** — convert ingredient amounts between compatible units on the fly
- **Favorites & tags** — mark go-to recipes and organize with custom tags
- **Shopping list** — add ingredients from any recipe and check them off
- **Media gallery** — attach multiple photos and videos per recipe (video playback via ExoPlayer)
- **Backup & restore** — export/import your data so nothing is lost
- **Dashboard** — tappable count tiles, search, and category filters
- **Ingredients overview** — grouped across all recipes with per-unit totals
- **Share** a recipe as clean, formatted text
- **Light / Dark / System** theme with a custom caramel-and-cream palette
- **Offline-first** — all data is stored locally on the device; no account or internet required

---

## 🛠️ Tech stack

- **Kotlin**, **Jetpack Compose**, **Material 3**
- **MVVM** architecture with **StateFlow**
- **Room** (SQLite) with proper schema **migrations** (v1 → v7)
- **Navigation Compose**, **Coil** image & video-thumbnail loading
- **Media3 / ExoPlayer** for in-app video playback
- **Kotlin Coroutines / Flow**
- **JUnit** unit tests for core logic

### Architecture

```
UI (Compose screens)
      │   observes StateFlow
ViewModel (MVVM)  ──►  Repository  ──►  Room DAO / SQLite
```

Each screen renders immutable UI state exposed by a ViewModel; the ViewModel talks to a repository that wraps Room, so data flows one way and the UI stays reactive.

---

## 🚀 Build from source

```bash
git clone https://github.com/lfigueras/Baking-Recipe-Keeper.git
cd Baking-Recipe-Keeper
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

Open in **Android Studio** (latest stable) and run on an emulator or device.

---

## 📸 Screenshots

<!-- Add screenshots here, e.g. drag images into docs/screenshots/ and reference them:
| Home | Recipe | Add | Dark |
|------|--------|-----|------|
| ![](docs/screenshots/home.png) | ![](docs/screenshots/detail.png) | ![](docs/screenshots/add.png) | ![](docs/screenshots/dark.png) |
-->

_Screenshots coming soon._

---

## 💬 Feedback & suggestions

Found a bug or have an idea? I'd love to hear it!

- 🐛 **Report a bug:** [open an issue](https://github.com/lfigueras/Baking-Recipe-Keeper/issues/new/choose)
- 💡 **Suggest a feature:** [open an issue](https://github.com/lfigueras/Baking-Recipe-Keeper/issues/new/choose)

(A free GitHub account is needed to file an issue.)

---

## 🗺️ Roadmap

Most of the original roadmap is now shipped — favorites, tags, shopping list, unit
conversion, media gallery, backup/restore, and "Start Baking" step-by-step mode.

**Next up:** optional cloud sync and accounts (an in-progress version lives on the
`feature/accounts-cloud` branch). See the full plan in [docs/roadmap.md](docs/roadmap.md)
(or the printable [PDF](docs/Baking-Recipe-Keeper-Roadmap.pdf) / [HTML](docs/roadmap.html)).

---

Made with 🧁 by [@lfigueras](https://github.com/lfigueras)
