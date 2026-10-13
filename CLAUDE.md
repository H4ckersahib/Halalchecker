# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

HalalCheck is an Android app (Kotlin, Jetpack Compose, Material 3) that scans a product barcode, fetches the ingredient list from Open Food Facts, and classifies the product as HALAL / HARAM / MASHBOOH (doubtful) / UNKNOWN, optionally per madhab (Hanafi, Maliki, Shafi'i, Hanbali). It is a non-profit app with no ads or tracking.

`spec.md` (German) is the product spec and roadmap. Only the Phase-1 MVP exists in code. The Supabase backend, community features, onboarding, and auth described there are **not implemented**. User-facing text and many code comments are in German.

## Commands

Single Gradle module `:app`. Use `./gradlew` from Git Bash or `.\gradlew.bat` from PowerShell. Gradle uses the Android Studio JBR via `JAVA_HOME`. Configuration cache is on, and a cold build takes about 3 minutes.

```bash
./gradlew assembleDebug            # debug APK -> app/build/outputs/apk/debug/
./gradlew installDebug             # install on device/emulator (appId com.halalcheck.app.debug)
./gradlew lintDebug
./gradlew testDebugUnitTest        # JVM unit tests (app/src/test)
./gradlew testDebugUnitTest --tests "com.halalcheck.app.SomeTest.someMethod"   # single test
./gradlew connectedDebugAndroidTest  # instrumented tests (app/src/androidTest), needs a device
./gradlew assembleRelease          # R8 minify + resource shrinking
```

No test source sets exist yet. JUnit4, AndroidX test, and Compose UI-test dependencies are already declared. `HalalEngine` has no Android dependencies (its constructor takes no arguments), so you can test it with plain JVM unit tests.

Release builds run R8. `app/proguard-rules.pro` keeps the `data.remote` package (Moshi models) and the `data.local.entity` package (Room entities). If you move those classes to another package, add new keep rules.

## Architecture

The layers are `data/` (Room, Retrofit, repositories), `domain/` (models + `HalalEngine`), `ui/` (Compose screens with `@HiltViewModel`s), and `di/` (Hilt modules). Navigation uses string routes in `ui/navigation/NavGraph.kt`: `home`, `scanner`, `result/{barcode}`, `history`, `settings`.

Repositories have `@Inject` constructors but are *also* constructed manually in `di/RepositoryModule.kt`. If you change a repository's constructor, update both places.

### Scan → result flow

1. `ScannerScreen`: CameraX `ImageAnalysis` with ML Kit barcode scanning (EAN-13/8, UPC-A/E, QR). It also has a manual-entry fallback. When a barcode is found, it navigates to `result/{barcode}` and pops the scanner off the back stack.
2. `ResultViewModel` reads `barcode` from `SavedStateHandle`.
3. `ProductRepository.lookupProduct()` checks the Room `products` cache first, then the Open Food Facts v2 API. Spec tiers 2 and 4 (own backend, community) are TODO. A network failure returns `Result.failure`. A product not found on Open Food Facts returns a placeholder product with `DataSource.UNKNOWN`.
4. `IngredientRepository.getAllIngredients()` loads the whole ingredient table into memory.
5. `HalalEngine.analyzeProduct(product, ingredients, madhab)` **always re-analyzes from `ingredientsText`**, even on a cache hit. The result is written back to `products` and a row is inserted into `scan_history`. `analyzedIngredients` is never persisted.

### HalalEngine (`domain/engine/HalalEngine.kt`)

This is the core logic and the most likely place for behavior changes.

- **Parsing** (`parseIngredientsListV2`): the engine strips percentages, splits on top-level commas only (it tracks parenthesis depth), and classifies each parenthesized part:
  - An E-number in parentheses becomes the matched ingredient: `Emulgator (E471)` matches E471.
  - A single short or known qualifier becomes a source qualifier: `Lecithin (Soja)`, `Gelatine (Schwein)`.
  - Anything else is parsed as sub-ingredients, recursively.
  - Results are de-duplicated by normalized main name.
- **Matching** (`findIngredientMatch`) tries these in order:
  1. Exact match on the id or any localized name.
  2. E-number regex, which matches the id `e<number>`.
  3. Compound match with spaces and underscores removed.
  4. Substring match in either direction (4 characters or more).
  - The full text is tried before the main name, so `Sojalecithin` can match directly.
- **Resolving MASHBOOH**:
  - The hardcoded `haramQualifiers` / `halalQualifiers` / `animalQualifiers` sets can upgrade or downgrade a ruling. Haram qualifiers win.
  - If the product's Open Food Facts `labels` say vegan or vegetarian, source-ambiguous ingredients become HALAL. `isSourceAmbiguousIngredient` holds a hardcoded id list that must match the JSON ids. `glycerin`, `lecithin`, and `stearic_acid` currently don't exist in the JSON.
- **Aggregation** (`determineOverallStatus`):
  - Any HARAM makes the product HARAM.
  - Otherwise, any MASHBOOH makes it MASHBOOH.
  - A mix of HALAL and UNKNOWN is also MASHBOOH.
  - All UNKNOWN makes it UNKNOWN.
  - Confidence is known ingredients divided by total ingredients.
- **Madhab**: `Ingredient.getRulingForMadhab()` uses the per-madhab ruling if one is set, otherwise `defaultCategory`.

### Ingredient database

- The source of truth is `app/src/main/assets/ingredients_db.json`. It is a flat array of objects:
  - Required: `id`, `name_de`, `name_en`, `default_category`, `source`.
  - Optional: `name_tr`, `name_ar`, `hanafi_ruling` / `maliki_ruling` / `shafi_ruling` / `hanbali_ruling`, `reason`, `reason_de`, `alternatives`.
- Ids are lowercase. E-numbers look like `e471` or `e470a`, and everything else is snake_case (`wheat_flour`). The E-number matcher depends on this format.
- `alternatives` is a **comma-separated string**, not a JSON array.
- The JSON is imported only in `RoomDatabase.Callback.onCreate` (`HalalDatabase.prepopulateIngredients`), using a hand-written `INSERT` statement. Its column list must stay in sync with `IngredientEntity`.
- **JSON edits do not reach an existing install.** Uninstall or clear app data, or bump the DB version.
- The import runs asynchronously on `Dispatchers.IO`. The first scan right after a fresh install can therefore see an empty ingredient table, and every ingredient shows as UNKNOWN.

### Persistence details

- Room stores enums as their `name` strings. Read them back with `HalalStatus.fromString()`, which is lenient and falls back to UNKNOWN. `DataSource` is read back with `valueOf`.
- `HalalDatabase` is at **version 1**, with `fallbackToDestructiveMigration()`. Bumping the version wipes the DB and re-seeds the ingredients.
- Schemas are exported to `app/schemas/`. `2.json` there is stale: it describes a `scan_history` table keyed by `barcode` that the current code doesn't use. The next version bump will regenerate it.
- `ProductEntity` does not store Open Food Facts `labels`. On a cache hit the vegan/vegetarian rule therefore can't fire.
- Settings (madhab, dark mode, language) are stored in Preferences DataStore. The `Context.dataStore` extension is defined in `ui/screens/settings/SettingsViewModel.kt`.

### Not yet wired up

- Nothing reads the saved settings yet:
  - `ResultViewModel` always starts with `Madhab.HANAFI`. Users can switch madhab per result in the UI.
  - `HalalCheckTheme` follows `isSystemInDarkTheme()`.
  - The language setting has no effect.
- UI strings are hardcoded German literals in the composables. `res/values/strings.xml` (German default) and `res/values-en/strings.xml` exist but are not referenced; there are no `stringResource` calls.
