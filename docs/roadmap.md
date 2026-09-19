# 🧁 Baking Recipe Keeper — Roadmap

> Product roadmap & (future) monetization plan.
> A nicely styled printable version is also available in [roadmap.html](roadmap.html)
> (download and open it in a browser — GitHub shows `.html` files as raw source).

## 🍞 Where we are

A solid, offline-first core: recipes with ingredients & baking procedure, metadata
(servings / prep / bake / difficulty), live scaling, unit conversion, share, search &
filters, a dashboard with drill-down, reorderable steps, favorites & tags, shopping
list, media gallery (photos + video), backup/restore, light/dark theming with a
caramel brand palette, snackbar feedback, and reliable Room storage with migrations.

All data is stored locally on the device — no account or internet required.

---

## 🗺️ Feature roadmap (phased)

### ✅ Phase 1 — Complete the core app _(done)_
- ⭐ Favorites + Favorites filter
- Bottom navigation (Home · Categories · Shopping · More)
- Shopping list — aggregate & de-duplicate ingredients into a checklist
- Export & share — clean formatted text
- Media — multiple photos + video per recipe, full-screen viewer
- "Start Baking" mode — full-screen, screen-on, step-by-step
- Backup / restore (JSON), empty-state art, onboarding, tags

### 🧭 Phase 2 — Accounts + Cloud _(partly done)_
- ✅ Unit conversion (g↔oz, ml↔cups) & smart scaling
- 🔜 User accounts (Google / email) — in progress on the `feature/accounts-cloud` branch
- 🔜 Cloud sync & backup across devices
- 🔜 Meal / bake planner + reminders
- 🔜 Import recipe from a URL, and photo/OCR → recipe

### 🔮 Phase 3 — Learning + Community
- "Learn to Bake" hub — techniques, glossary, tips, fixes
- AI assistant — substitutions, troubleshooting, auto-scaling
- Community — publish, rate, comment, follow, collections
- Shareable web links + landing page

### 🚀 Phase 4 — Scale + Platforms
- Creator marketplace — sell/curate recipe packs (revenue share)
- iOS + Web (Compose Multiplatform)
- Localization (languages, metric/imperial)
- Home-screen widgets, WearOS timers, voice hands-free

---

## 💰 Monetization (future) — Freemium + Subscription

| Tier | Price | Includes |
|------|-------|----------|
| **Free 🆓** | $0 | ~15–25 recipes, core editing & scaling, search & share, optional light ads |
| **Premium 👑** | ~$3–5/mo · ~$25–30/yr | Unlimited recipes, cloud sync & backup, media/video, shopping list, PDF export, planner, AI, premium themes, no ads |
| **Lifetime / Family 🏡** | ~$40–60 once | One-time unlock, shared household library, great for classes / cafés |

**Other revenue streams:** affiliate links on ingredients & tools, in-app recipe/theme
packs, creator marketplace revenue share, and B2B licensing to baking schools.

**How:** Google Play Billing for subscriptions, a paywall gating premium features, an
`isPremium` check, and analytics to track conversion.

---

## 👩‍🍳 Useful to every baker (& cook)

- **Beginners:** guided Start-Baking mode, glossary, technique videos, timers, AI "why did this fail?"
- **Experienced:** scaling, unit conversion, yield math, notes & versioning, export/print
- **Cooks:** broaden to savory recipes — a general recipe keeper with a baking heart
- **Everyone:** cloud sync, shopping list, social sharing

---

## ✅ Suggested order

1. Phase 1 core — makes it sticky _(done)_
2. Accounts + cloud sync — the #1 reason people pay
3. Play Billing paywall + free-tier limits — turn on revenue
4. AI assistant + community/links — differentiation & growth

---

Made with 🧁 for bakers everywhere · Baking Recipe Keeper · 2026
