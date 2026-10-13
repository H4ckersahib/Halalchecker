# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

HalalCheck is an Android app (Kotlin, Jetpack Compose, Material 3) that scans a product barcode, fetches the ingredient list from Open Food Facts, and classifies the product as HALAL / HARAM / MASHBOOH (doubtful) / UNKNOWN, optionally per madhab (Hanafi, Maliki, Shafi'i, Hanbali). It is a non-profit app with no ads or tracking.

`spec.md` (German) is the product spec and roadmap. Only the Phase-1 MVP exists in code. The Supabase backend, community features, onboarding, and auth described there are **not implemented**. Many code comments are in German.

## Commands

Single Gradle module `:app`. Use `./gradlew` from Git Bash or `.\gradlew.bat` from PowerShell. Gradle uses the Android Studio JBR via `JAVA_HOME`. Configuration cache is on, and a cold build takes about 3 minutes.

```bash
./gradlew assembleDebug            # debug APK -> app/build/outputs/apk/debug/
./gradlew installDebug             # install on device/emulator (appId com.halalcheck.app.debug)
./gradlew lintDebug
./gradlew testDebugUnitTest        # JVM unit tests (app/src/test)
./gradlew testDebugUnitTest --tests "com.halalcheck.app.domain.engine.HalalEngineTest"   # single class
./gradlew connectedDebugAndroidTest  # instrumented tests (app/src/androidTest), needs a device
./gradlew assembleRelease          # R8 minify + resource shrinking
python tools/build_ingredient_db.py            # regenerate the ingredient asset (stdlib only)
python tools/build_ingredient_db.py --refresh  # same, re-downloading the OFF taxonomies first
```

Unit tests:
- `HalalEngineTest` runs the engine against a small fixture index, one test per known matching pitfall.
- `IngredientAssetTest` loads the real `assets/ingredients_db.json` and runs real German labels through the engine. Run it after regenerating the asset.
- Don't use hyphens in backtick test names: Android lint's Kotlin analysis crashes on them.

Release builds run R8. `app/proguard-rules.pro` keeps the `data.remote` (Moshi API models), `data.local.entity` (Room entities) and `data.local.seed` (Moshi asset models) packages. If you move those classes to another package, add new keep rules. Moshi uses codegen only (`@JsonClass(generateAdapter = true)`); there is no reflection adapter, so every Moshi model needs the annotation.

## Architecture

The layers are `data/` (Room, Retrofit, DataStore, repositories), `domain/` (models + engine), `ui/` (Compose screens with `@HiltViewModel`s), and `di/` (Hilt modules for Room, Retrofit, Moshi). Repositories are provided through their `@Inject` constructors. Navigation uses string routes in `ui/navigation/NavGraph.kt`: `home`, `scanner`, `result/{barcode}`, `history`, `settings`.

### Scan → result flow

1. `ScannerScreen`: CameraX `ImageAnalysis` (background executor) with ML Kit, restricted to EAN-13/8 and UPC-A/E. QR codes are deliberately not scanned: their URLs can't be looked up and broke the route. Manual entry accepts 8–14 digits. When a barcode is found, it navigates to `result/{barcode}` and pops the scanner off the back stack.
2. `ResultViewModel` reads `barcode` from `SavedStateHandle` and the default madhab from `UserPreferencesRepository`.
3. `ProductRepository.lookupProduct()`:
   - A cached product younger than 7 days is used directly.
   - Otherwise it asks the Open Food Facts v2 API.
   - On a network error it falls back to a stale cache.
   - OFF answers unknown barcodes with **HTTP 404**; that becomes a placeholder product with `DataSource.UNKNOWN`, not an error. Placeholders are never cached or saved to history.
   - Spec tiers "own backend" and "community" are TODO.
4. `IngredientRepository.getIndex()` returns an in-memory `IngredientIndex`, built once per process after `IngredientSeeder` has run.
5. `HalalEngine.analyzeProduct(product, index, madhab)` always re-analyzes from the product's source data (text, OFF ingredients, labels, additives), also on a cache hit and on madhab change. It runs on `Dispatchers.Default`.
6. The status and confidence are written back to `products` (`ProductDao.updateAnalysis`), and a row is inserted into `scan_history`. `analyzedIngredients` is never persisted.

### HalalEngine (`domain/engine/`)

This is the core logic and the most likely place for behavior changes. It has no Android dependencies.

- **Two pipelines, best one wins.** The engine analyzes the ingredient text with `IngredientTextParser`, and separately OFF's own parsed `ingredients[]` (taxonomy ids, looked up via `IngredientIndex.byOffId`). It keeps whichever recognizes the larger share. It then adds doubtful or haram additives from OFF's `additives_tags` that aren't already covered (compared by `Ingredient.eNumber` family, so soy lecithin covers E322).
- **Parsing** (`IngredientTextParser`):
  - Strips percentages, empty brackets, `*`/`_`, and a leading "Zutaten:".
  - Drops note sentences (traces, origin, "mindestens") and note items ("Kann …").
  - `Class: X` yields X when the head is a functional class (`Ingredient.isClass`, e.g. Emulgator, Säuerungsmittel). Otherwise the head is a "list head" that only counts if HARAM.
  - A bare class followed by more entries is treated as a typo for a colon.
  - A class prefix is stripped ("Emulgator Lecithine (Soja)" → "Lecithine").
  - Parentheses hold an E-number (the ingredient), only source words (qualifiers: "Gelatine (Rind)"), or a composition (children). A recognized parent counts next to its composition only if HARAM or if it has a single child ("Molke (Milch)").
- **Matching** (`IngredientIndex.findAll`), hash based:
  1. Whole phrase on names/synonyms in all languages, ignoring spacing and simple plural endings.
  2. E/INS numbers as whole tokens (`e322i` falls back to `e322`).
  3. Longest known word spans. A phrase can yield several matches; the most severe counts.
  4. Compound heads ("Putenschinken" → "Schinken", marked `inferred`, never a class).
  - **There is no substring matching**; it produced false HARAM ("Weinbeeren" → Wein).
- **Rules** (`HalalEngine.Analysis.evaluate`), applied the same way on every path:
  - A haram qualifier or compound modifier (`Qualifiers`, token based) makes the ingredient HARAM.
  - An animal word turns a pork-by-default HARAM ("Speck", "Schinken") into MASHBOOH.
  - For MASHBOOH: an animal word keeps it MASHBOOH, and a plant/synthetic/fish word makes it HALAL.
  - A vegan/vegetarian label makes MASHBOOH HALAL only if `doubt == ANIMAL_SOURCE`.
  - A halal label makes MASHBOOH HALAL.
  - Reasons are a sealed `Reason`; the UI localizes them.
- **Aggregation** (`determineOverallStatus`):
  - Any HARAM makes the product HARAM (even with a halal label, which sets `labelConflict`).
  - Otherwise a halal label makes it HALAL.
  - Otherwise any MASHBOOH makes it MASHBOOH.
  - Otherwise any unrecognized ingredient makes it UNKNOWN (the UI says "nicht vollständig geprüft").
  - Otherwise HALAL.
  - Confidence is the recognized share, with inferred matches counting half.
- **Madhab**: `Ingredient.getRulingForMadhab()` uses the per-madhab ruling if one is set, otherwise `defaultCategory`.

### Ingredient database

- **Generated, don't edit by hand.** `app/src/main/assets/ingredients_db.json` (about 6,300 entries, ODbL) is produced by `tools/build_ingredient_db.py`. It merges the Open Food Facts ingredient/additive/additive-class taxonomies (names and synonyms in de/en/fr/tr/ar, vegan flags, parent hierarchy; cached in `tools/.cache/`) with the curated `tools/halal_rulings.json`.
- **Change rulings in `tools/halal_rulings.json`**, then regenerate:
  - Keys are OFF taxonomy ids (`en:pork`); use `local:<id>` for ingredients OFF lacks.
  - MASHBOOH rulings need a `doubt`.
  - Descendants inherit the nearest curated ruling (`en:wine` → `en:alcohol`).
  - Uncurated nodes are classified from OFF's vegan flags (`source: off_vegan` / `off_vegetarian` / `off_maybe` / `off_animal`).
  - The script prints conflicts between equally near ancestors; add a ruling for the node to override.
- **Entry format.** The root is `{version, generated, license, ingredients}`. Ids are the OFF id without `en:` and with `-`→`_` (`soya_lecithin`, `e471`); other languages keep a prefix (`fr_…`). `synonyms` is an array in the JSON and a `|`-joined string in Room. `alternatives` is a comma-separated string.
- **Seeding.** `IngredientSeeder` replaces the `ingredients` table whenever the asset's `version` (a content hash) differs from the one stored in DataStore, so asset updates reach existing installs. Callers await it; the first start after an update takes a few seconds.

### Persistence details

- Room stores enums as their `name` strings. Read them back with `HalalStatus.fromString()`, which is lenient and falls back to UNKNOWN. `DataSource` is read back with `valueOf`.
- **Version and migration.**
  - `HalalDatabase` is at **version 2**. `MIGRATION_1_2` adds the ingredient columns and `products.off_payload`, and keeps the scan history.
  - `fallbackToDestructiveMigration()` is still set as a safety net.
  - Bumping the version needs a new `Migration`.
  - Schemas are exported to `app/schemas/`.
- **Product cache.** `products.off_payload` stores the fetched `OpenFoodFactsProduct` as Moshi JSON, so labels and structured ingredients survive caching. Rows from v1 have no payload and are re-fetched.
- **Settings.** `UserPreferencesRepository` owns the Preferences DataStore ("settings"): madhab, dark mode (null = system), ingredient DB version. The UI language is stored by AppCompat (`AppCompatDelegate.setApplicationLocales`, `autoStoreLocales` service in the manifest). That's why `MainActivity` is an `AppCompatActivity`.

### UI language

- UI strings are resources in `res/values/` (German default) and `res/values-en/`. Composables use `stringResource`, and ViewModels expose enums (`ResultError`), not text.
- Settings offers System / Deutsch / English. Turkish and Arabic are in the spec but not translated yet; add `values-tr`/`values-ar` and `res/xml/locales_config.xml` entries together.

### Known gaps

- The status colors in `ui/components/HalalStatusBadge.kt` are light-mode colors, so ingredient rows have low contrast in dark mode.
- `ProductEntity`'s `name`/`brand`/`image_url`/`ingredients_text` columns are still written, but a cache hit is rebuilt from `off_payload` only.
