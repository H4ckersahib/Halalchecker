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
./gradlew connectedDebugAndroidTest  # instrumented tests; installs AND uninstalls on EVERY connected device
./gradlew assembleRelease          # R8 minify + resource shrinking; signed if keystore.properties exists
python tools/build_ingredient_db.py            # regenerate the ingredient asset (stdlib only)
python tools/build_ingredient_db.py --refresh  # same, re-downloading the OFF taxonomies first
python tools/analyze_feedback.py               # report from testers' exports in feedback/ (not in Git)
```

Unit tests:
- `HalalEngineTest` runs the engine against a small fixture index, one test per known matching pitfall.
- `IngredientAssetTest` loads the real `assets/ingredients_db.json` and runs real German labels through the engine. Run it after regenerating the asset.
- `ProductMappingTest` (OFF JSON to `Product`) and `GtinTest` (manual barcode check digits) are plain JVM tests too.
- `app/src/androidTest/.../MigrationTest` upgrades a v1 database through all migrations (`MigrationTestHelper`,
  schemas from `app/schemas/` via the androidTest assets). To run it on one device only (the developer's phone may be
  connected over wireless ADB), install `app-debug.apk` and `app-debug-androidTest.apk` with `adb -s <serial>` and run
  `adb -s <serial> shell am instrument -w -e class com.halalcheck.app.data.local.MigrationTest com.halalcheck.app.debug.test/androidx.test.runner.AndroidJUnitRunner`.
- Don't use hyphens in backtick test names: Android lint's Kotlin analysis crashes on them.

Release signing: `app/build.gradle.kts` reads `keystore.properties` from the project root (not in Git; template `keystore.properties.example`, key in `%USERPROFILE%\keys\`). Without it, release APKs are unsigned. Every APK given to testers must be signed with that same key, or updates can't install over it and testers lose their data. `versionCode` is the Git commit count, so it rises with every commit.

Release builds run R8. `app/proguard-rules.pro` keeps the `data.remote` (Moshi API models), `data.local.entity` (Room entities) and `data.local.seed` (Moshi asset models) and `data.export` (Moshi export models) packages. If you move those classes to another package, add new keep rules. Moshi uses codegen only (`@JsonClass(generateAdapter = true)`); there is no reflection adapter, so every Moshi model needs the annotation.

## Architecture

The layers are `data/` (Room, Retrofit, DataStore, repositories), `domain/` (models + engine), `ui/` (Compose screens with `@HiltViewModel`s), and `di/` (Hilt modules for Room, Retrofit, Moshi). Repositories are provided through their `@Inject` constructors. Navigation uses string routes in `ui/navigation/NavGraph.kt`: `home`, `scanner`, `result/{barcode}?record={record}`, `history`, `settings`. List taps go through `ifResumed`, so tapping twice opens one screen.

### Scan → result flow

1. `ScannerScreen`: CameraX `ImageAnalysis` (background executor) with ML Kit, restricted to EAN-13/8 and UPC-A/E. QR codes are deliberately not scanned: their URLs can't be looked up and broke the route. Manual entry accepts 8–14 digits. When a barcode is found, it navigates to `result/{barcode}?record=true` and pops the scanner off the back stack. Home and History open results with `record=false`.
2. `ResultViewModel` reads `barcode` and `record` from `SavedStateHandle`, the default madhab and the user's vegan confirmation for this barcode from `UserPreferencesRepository`.
3. `ProductRepository.lookupProduct()`:
   - A cached product younger than 7 days is used directly.
   - Otherwise it asks the Open Food Facts v2 API.
   - On a network error it falls back to a stale cache.
   - OFF answers unknown barcodes with **HTTP 404**; that becomes a placeholder product with `DataSource.UNKNOWN`, not an error. Placeholders are never cached or saved to history.
   - The refresh button (`ResultViewModel.refresh`) skips the cache, e.g. after the user corrected the product on OFF.
   - Spec tiers "own backend" and "community" are TODO.
4. `IngredientRepository.getIndex()` returns an in-memory `IngredientIndex`, built once per process after `IngredientSeeder` has run.
5. `HalalEngine.analyzeProduct(product, index, madhab)` always re-analyzes from the product's source data (text, OFF ingredients, labels, additives), also on a cache hit and on madhab change. It runs on `Dispatchers.Default`.
6. The status and confidence are written back to `products` (`ProductDao.updateAnalysis`). A real scan (`record=true`) inserts the `scan_history` row (one row per barcode; a rescan replaces it and moves it to the top); reopening only updates the stored status. `analyzedIngredients` is never persisted.

### HalalEngine (`domain/engine/`)

This is the core logic and the most likely place for behavior changes. It has no Android dependencies.

- **Two pipelines, merged.** The engine analyzes the ingredient text with `IngredientTextParser`, and separately OFF's own parsed `ingredients[]` (taxonomy ids, looked up via `IngredientIndex.byOffId`). It keeps whichever recognizes the larger share, then adds HARAM/MASHBOOH findings only the other one had (same ingredient = same E-number family or curated ancestor) and sets `Product.sourceConflict`. Finally it adds doubtful or haram additives from OFF's `additives_tags` that aren't already covered (compared by `Ingredient.eNumber` family, so soy lecithin covers E322).
- **Parsing** (`IngredientTextParser`):
  - Strips percentages, empty brackets, `*`/`_`, and a leading "Zutaten:".
  - Drops note sentences (traces, origin, "mindestens") and note items ("Kann …").
  - `Class: X` yields X when the head is a functional class (`Ingredient.isClass`, e.g. Emulgator, Säuerungsmittel). Otherwise the head is a "list head" that only counts if HARAM.
  - A bare class directly followed by an additive ("Farbstoff, Zuckerkulör (E150d)") is treated as a typo for a colon; otherwise ("Farbstoff, Zucker") it stays as "not specified".
  - A class prefix is stripped ("Emulgator Lecithine (Soja)" → "Lecithine").
  - Parentheses hold an E-number (the ingredient), only source words (qualifiers: "Gelatine (Rind)"), or a composition (children). A recognized parent counts next to its composition if it is HARAM, if it has a single child ("Süßmolkenpulver (aus Milch)"; a HALAL single child is an annotation and is not listed), or if it is MASHBOOH in a way the composition can't answer ("Wurst (Salz, Gewürze)", "Aroma (Wasser, Zucker)"). It is dropped when its doubt only asks what is inside: OFF-derived entries (`source` `off_*`, "Füllung") and curated entries with `compositionResolves` ("Milchschokolade", "Käse").
  - " - " separates entries except before und/oder ("Mono - und Diglyceride").
- **Matching** (`IngredientIndex.findAll`), hash based:
  1. Whole phrase on names/synonyms in all languages, ignoring spacing and simple plural endings.
  2. E/INS numbers as whole tokens (`e322i` falls back to `e322`), plus whatever the words around them contain ("Schweinefleisch mit E471" yields both).
  3. Longest known word spans. A phrase can yield several matches; the most severe counts.
  4. Compound heads ("Putenschinken" → "Schinken"), else the base product of a form word ("Kartoffelpulver" → "Kartoffel"; no "-extrakt"). Both are marked `inferred` and never match a class.
  - A functional class only counts when the phrase contains nothing more specific.
  - **There is no substring matching**; it produced false HARAM ("Weinbeeren" → Wein).
- **Rules** (`HalalEngine.Analysis.evaluate`), applied the same way on every path:
  - A haram qualifier or compound modifier (`Qualifiers`, token based) makes the ingredient HARAM.
  - An animal word turns a pork-by-default HARAM ("Speck", "Schinken") into MASHBOOH.
  - For MASHBOOH: an animal word keeps it MASHBOOH; a plant/synthetic/fish word makes it HALAL only if `doubt == ANIMAL_SOURCE` ("Sojasoße (Soja)" stays MASHBOOH: its doubt is alcohol).
  - OFF labels are matched exactly against `KnownLabels` (OFF labels taxonomy descendants of en:vegan / en:vegetarian / en:halal). Never use substring matching: OFF has `en:non-vegan` and `en:non-vegetarian`.
  - A vegan or vegetarian claim (from OFF or from the user's `Product.userClaims`) makes MASHBOOH HALAL only if `doubt == ANIMAL_SOURCE`, and only if no recognized, non-inferred ingredient has `vegan == NO` (resp. `vegetarian == NO`). Each claim is checked on its own; contradicted claims are reported in `veganConflicts` / `vegetarianConflicts` and ignored. `Product.dietLabelWouldResolve` tells the result screen to ask.
  - A halal label counts only when the user confirmed it on the package (`PackageClaim.HALAL`). It makes MASHBOOH HALAL but never UNKNOWN or HARAM. An OFF halal tag alone is shown as an unverified hint; with a HARAM ingredient it sets `labelConflict`.
  - Reasons are a sealed `Reason`; the UI localizes them.
- **Aggregation** (`determineOverallStatus`):
  - Any HARAM makes the product HARAM.
  - Otherwise any MASHBOOH makes it MASHBOOH.
  - Otherwise any unrecognized ingredient, or nothing to analyze, makes it UNKNOWN (the UI says "nicht vollständig geprüft").
  - Otherwise HALAL.
  - Confidence is the recognized share, with inferred matches counting half. The UI calls it "Zutaten erkannt", not certainty.
- **Madhab**: `Ingredient.getRulingForMadhab()` uses the per-madhab ruling if one is set, otherwise `defaultCategory`. Only a few curated entries have per-madhab rulings; the result screen says so.

### Ingredient database

- **Generated, don't edit by hand.** `app/src/main/assets/ingredients_db.json` (about 6,300 entries, ODbL) is produced by `tools/build_ingredient_db.py`. It merges the Open Food Facts ingredient/additive/additive-class taxonomies (names and synonyms in de/en/fr/tr/ar, vegan flags, parent hierarchy; cached in `tools/.cache/`) with the curated `tools/halal_rulings.json`.
- **Change rulings in `tools/halal_rulings.json`**, then regenerate:
  - Keys are OFF taxonomy ids (`en:pork`); use `local:<id>` for ingredients OFF lacks.
  - MASHBOOH rulings need a `doubt`.
  - Optional: `references` (list of `{url, title, title_de}`, shown as links under the ingredient: sources for context, not a ruling on the product), `composition_resolves`, and `vegan`/`vegetarian` for `local:` entries (OFF nodes get them from the taxonomy).
  - Descendants inherit the nearest curated ruling (`en:wine` → `en:alcohol`).
  - Uncurated nodes are classified from OFF's vegan flags (`source: off_vegan` / `off_vegetarian` / `off_maybe` / `off_animal`).
  - The script prints conflicts between equally near ancestors, and entries classified HALAL only from OFF vegan flags whose names mention alcohol (this found "Himbeerlikör"); add a ruling for the node to override.
- **Entry format.** The root is `{version, generated, license, ingredients}`. Ids are the OFF id without `en:` and with `-`→`_` (`soya_lecithin`, `e471`); other languages keep a prefix (`fr_…`). `synonyms` is an array in the JSON and a `|`-joined string in Room. `alternatives` is a comma-separated string.
- **Seeding.** `IngredientSeeder` replaces the `ingredients` table whenever the asset's `version` (a content hash) differs from the one stored in DataStore, so asset updates reach existing installs. Callers await it; the first start after an update takes a few seconds.

### Persistence details

- Room stores enums as their `name` strings. Read them back with `HalalStatus.fromString()`, which is lenient and falls back to UNKNOWN. `DataSource` is read back with `valueOf`.
- **Version and migration.**
  - `HalalDatabase` is at **version 5**. `MIGRATION_1_2` adds the ingredient columns and `products.off_payload`, and keeps the scan history. `MIGRATION_2_3` removes duplicate history rows and adds a unique index on `scan_history.barcode`. `MIGRATION_3_4` adds `vegan`, `vegetarian`, `ruling_references` and `composition_resolves` to `ingredients`. `MIGRATION_4_5` adds the `feedback` table.
  - There is **no** `fallbackToDestructiveMigration()`: a missing migration must fail instead of silently wiping the history. Extend `ALL_MIGRATIONS` and `MigrationTest`.
  - Bumping the version needs a new `Migration`.
  - Schemas are exported to `app/schemas/`.
- **Product cache.** `products.off_payload` stores the fetched `OpenFoodFactsProduct` as Moshi JSON, so labels and structured ingredients survive caching. Rows from v1 have no payload and are re-fetched. When OFF can't be reached and a cached copy is used, `Product.offlineSince` carries its fetch time and the result screen says so. HTTP 429 becomes `ResultError.RATE_LIMITED`.
- **Settings.** `UserPreferencesRepository` owns the Preferences DataStore ("settings"): madhab, dark mode (null = system), ingredient DB version, and per `PackageClaim` (vegan, vegetarian, halal) the barcodes whose package the user confirmed. Settings can delete these claims. The UI language is stored by AppCompat (`AppCompatDelegate.setApplicationLocales`, `autoStoreLocales` service in the manifest). That's why `MainActivity` is an `AppCompatActivity`.

### Test phase: feedback and export

- Testers report wrong verdicts on the result screen ("Fehler melden": expected status or "don't know", comment). Stored in the `feedback` table (one per barcode), see `FeedbackRepository`.
- Settings → Testphase exports a JSON file (`data/export`): every history product plus every feedback, re-analyzed with the current rules from the cached OFF data, with app build and ingredient DB version, the optional tester name, per-ingredient match and reason. `ExportBuilder` is Android-free (tested by `ExportBuilderTest`); `ExportRepository` writes to `cacheDir/exports/` and the settings screen shares it through the `FileProvider` (`res/xml/file_paths.xml`).
- Exports contain personal data. They stay on the phone until shared; put received files into `feedback/` (git-ignored) and run `tools/analyze_feedback.py`. Bump `ExportFile.EXPORT_VERSION` and the script together when fields change.

### UI language

- UI strings are resources in `res/values/` (German default) and `res/values-en/`. Composables use `stringResource`, and ViewModels expose enums (`ResultError`), not text.
- Settings offers System / Deutsch / English. Turkish and Arabic are in the spec but not translated yet; add `values-tr`/`values-ar` and `res/xml/locales_config.xml` entries together.

### Known gaps

- User package claims are not invalidated when the product's OFF data changes later.
- Rulings still use `defaultCategory` plus a few madhab fields; a model with separate product facts, sourced fiqh rules and assessments (see the external review) is not implemented.
- `ProductEntity`'s `name`/`brand`/`image_url`/`ingredients_text` columns are still written, but a cache hit is rebuilt from `off_payload` only.
