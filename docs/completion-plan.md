# Baking Recipe Keeper — Completion Plan

A phased plan to finish the product roadmap, grounded in the current codebase
(MVVM + Jetpack Compose + Room v5, Kotlin 2.0.21). The core app is solid:
recipe CRUD, live scaling, drag-reorder steps, search & category filters,
dashboard drill-down, caramel light/dark theming, snackbar feedback, single-photo
media, and reliable Room migrations.

---

## Phase 1 — Complete the Core App (Free · Retention)

Goal: make the app sticky. All local, no backend required. This is the bulk of
near-term work.

### 1.1 Data-layer foundations (do first — unblocks everything)
Bump Room to **v6** in `PastryDatabase.kt` with one migration adding:
- `isFavorite: Boolean` column on `Pastry` (default 0)
- New `tags` table + `pastry_tag` cross-ref (many-to-many)
- New `shopping_list_item` table (name, amount, unit, isChecked, sourcePastryId)
- New DAO methods: `setFavorite(id, bool)`, tag queries, shopping-list CRUD

### 1.2 Favorites ⭐
- Favorite toggle (heart icon) on `PastryDetailScreen.kt` and list cards in
  `PastryListScreen.kt`
- Repository + ViewModel wiring; Favorites-only query/filter

### 1.3 Bottom navigation
- `Scaffold` with `NavigationBar` (Home · Favorites · Categories · More) wrapping
  `AppNavigation.kt`
- Move Settings / backup / export under "More"
- Favorites tab reuses the list screen with a favorites-only filter

### 1.4 Duplicate recipe
- "Duplicate" action in detail overflow menu → deep-copy `PastryWithIngredients`
  (new id, steps/ingredients re-inserted) via `PastryRepository.kt`

### 1.5 Shopping list
- Reuse existing `groupIngredients()` aggregation logic from
  `IngredientsListViewModel.kt`
- "Add to shopping list" from a recipe (respects current scaling); aggregate &
  de-dupe by name + unit
- Checklist screen with checkable rows, clear-completed, clear-all

### 1.6 Tags
- Tag chips in `AddPastryScreen.kt` (add/remove, autocomplete from existing tags)
- Tag filter on list/search screens

### 1.7 Export & print (text / CSV / PDF)
- Text: extend the existing share formatter
- CSV: ingredients table export
- PDF: render via `PdfDocument` (Canvas) or `PrintDocumentAdapter` for system print
- Share sheet + save-to-file (SAF)

### 1.8 "Start Baking" mode
- Full-screen step-by-step player with `keepScreenOn`
- Per-step timers (detect durations or manual set), next/prev, progress indicator

### 1.9 Media — multiple photos + short video
- Migrate single `imageUri` to a `media` table (type: photo/video, uri, position)
- Multi-select picker, gallery carousel, full-screen viewer (extend existing),
  ExoPlayer/VideoView for video

### 1.10 Backup / restore (JSON)
- Serialize all entities to JSON via kotlinx.serialization (already a dependency)
- Export to user-picked file (SAF); import with merge/replace choice + copy media

### 1.11 Polish
- Empty-state artwork (replace generic icons)
- First-launch onboarding (3–4 slide walkthrough + prefs flag)

---

## Phase 2 — Accounts + Cloud (Unlocks subscriptions)

Goal: cross-device value people pay for. Requires a backend (Firebase recommended
for speed).

- **User accounts**: Firebase Auth (Google + email)
- **Cloud sync & backup**: Firestore / Firebase Storage; offline-first sync layer
  over Room with conflict resolution
- **Unit conversion** (g↔oz, ml↔cups): conversion engine + smart scaling using the
  existing `IngredientUnit` enum
- **Meal / bake planner + reminders**: calendar entries + `WorkManager` /
  `AlarmManager` notifications
- **Import from URL + photo/OCR**: HTML recipe parser (JSON-LD / schema.org);
  ML Kit OCR → parse to recipe

---

## Phase 3 — Learning + Community (Expands audience)

- **"Learn to Bake" hub**: techniques, glossary, tips, fixes (static/remote content)
- **AI assistant**: substitutions, troubleshooting, auto-scaling (LLM API + prompt
  layer over recipe data)
- **Community**: publish / rate / comment / follow / collections (needs Phase 2
  backend)
- **Shareable web links + landing page**: dynamic links (open-in-app / get-the-app)

---

## Phase 4 — Scale + Platforms (Growth)

- **Creator marketplace**: sell/curate recipe packs (revenue share)
- **iOS + Web**: migrate shared logic to Compose Multiplatform
- **Localization**: string extraction, metric/imperial
- **Widgets / WearOS timers / voice hands-free**

---

## Monetization (parallel to Phase 2–3)

- Google Play Billing + paywall + `isPremium` check
- Free-tier limits (recipe count); premium unlocks (sync, media, PDF, planner, AI,
  themes, no ads)
- Analytics for conversion tracking

---

## Suggested execution order

1. **Phase 1 core** — Favorites, Duplicate, Shopping list, Export/PDF, Start
   Baking → makes it sticky
2. **Phase 2** accounts + cloud sync → the #1 reason people pay
3. **Play Billing paywall + free-tier limits** → turn on revenue
4. **Phase 3** AI + community/links → differentiation & growth
