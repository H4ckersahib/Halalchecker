# HalalCheck – Produktspezifikation

## 🎯 Vision

**HalalCheck** ist eine gemeinnützige, community-getriebene Mobile App, die es Muslimen ermöglicht, im Supermarkt per Barcode-Scan sofort zu erfahren, ob ein Produkt **halal** oder **haram** ist. Die App analysiert Zutatenlisten und gleicht sie mit einer umfassenden Datenbank ab.

> **Motto**: *"Wissen, was du isst – einfach, schnell, halal."*

---

## 📱 Plattformen

| Phase | Plattform | Technologie |
|-------|-----------|-------------|
| Phase 1 (MVP) | Android | Kotlin + Jetpack Compose |
| Phase 2 | iOS | Swift / SwiftUI (oder KMP) |

> [!NOTE]
> Wir starten mit **Android** als erste Plattform. Die Architektur wird so gestaltet, dass eine spätere Portierung auf iOS (oder ein Cross-Platform-Ansatz via Kotlin Multiplatform) möglich ist.

---

## 🏗️ Architektur-Übersicht

```
┌─────────────────────────────────────────────────┐
│                  HalalCheck App                  │
├──────────┬──────────┬───────────┬───────────────┤
│ Barcode  │ Produkt- │ Halal-    │ Community-    │
│ Scanner  │ Lookup   │ Engine    │ Modul         │
├──────────┴──────────┴───────────┴───────────────┤
│              Lokale SQLite / Room DB             │
├─────────────────────────────────────────────────┤
│           Backend API (Firebase / Supabase)       │
├─────────────────────────────────────────────────┤
│        Open Food Facts API (extern)              │
└─────────────────────────────────────────────────┘
```

---

## 🔑 Kernfunktionen

### 1. Barcode-Scanner

| Feature | Details |
|---------|---------|
| Scan-Methode | Kamera-basierter Barcode-Scanner |
| Unterstützte Formate | EAN-13, EAN-8, UPC-A, UPC-E, QR-Code |
| Bibliothek | Google ML Kit Barcode Scanning |
| UX | Echtzeit-Scan mit Overlay, Vibration bei Erkennung |
| Fallback | Manuelle Eingabe der Barcode-Nummer |

### 2. Produkt-Lookup (Hybrid: Online + Offline)

Die App nutzt einen **dreistufigen Lookup**:

```
Barcode gescannt
      │
      ▼
┌─────────────────┐
│ 1. Lokale DB    │ ── Treffer? → Ergebnis anzeigen
│    (Room/SQLite) │
└────────┬────────┘
         │ Kein Treffer
         ▼
┌─────────────────┐
│ 2. HalalCheck   │ ── Treffer? → Ergebnis anzeigen + lokal cachen
│    Backend API   │
└────────┬────────┘
         │ Kein Treffer
         ▼
┌─────────────────┐
│ 3. Open Food    │ ── Treffer? → Zutaten durch Halal-Engine prüfen
│    Facts API     │              → Ergebnis anzeigen + an Backend senden
└────────┬────────┘
         │ Kein Treffer
         ▼
┌─────────────────┐
│ 4. Community     │ ── Nutzer kann Produkt manuell hinzufügen
│    Beitrag       │
└─────────────────┘
```

### 3. Halal-Engine (Zutaten-Analyse)

Die Halal-Engine ist das **Herzstück** der App. Sie bewertet jede Zutat eines Produkts.

#### Zutatenkategorien

| Kategorie | Symbol | Farbe | Beschreibung |
|-----------|--------|-------|-------------|
| **Halal** | ✅ | Grün | Eindeutig erlaubt (z.B. Wasser, Salz, Zucker, pflanzliche Öle) |
| **Haram** | ❌ | Rot | Eindeutig verboten (z.B. Schweinefleisch, Alkohol, Gelatine vom Schwein) |
| **Mashbooh** (Zweifelhaft) | ⚠️ | Orange | Unklar, hängt von Herkunft ab (z.B. Gelatine, Emulgatoren, Aromen) |
| **Unbekannt** | ❓ | Grau | Nicht in der Datenbank |

#### Zutatendatenbank (Offline-fähig)

Die App enthält eine **vorinstallierte Zutatendatenbank** mit ca. 500-1000 häufigen Zutaten:

```json
{
  "ingredient_id": "E471",
  "names": {
    "de": "Mono- und Diglyceride von Speisefettsäuren",
    "en": "Mono- and diglycerides of fatty acids",
    "ar": "أحاديات وثنائيات الغليسيريد",
    "tr": "Mono ve digliseridler"
  },
  "category": "mashbooh",
  "reason": "Kann aus tierischen oder pflanzlichen Fetten gewonnen werden. Ohne Zertifikat nicht eindeutig bestimmbar.",
  "source": "islamic_food_council",
  "alternatives": ["E471 (pflanzlich)", "Sonnenblumenlecithin"]
}
```

#### Bekannte Haram-Zutaten (Auszug)

- Schweinefleisch / Schweineschmalz / Schweinefett
- Gelatine (wenn nicht als halal zertifiziert)
- Alkohol / Ethanol (als Zutat, nicht als Trägerstoff)
- Karmin (E120) – aus Läusen gewonnen
- L-Cystein (E920) – oft aus Schweineborsten oder Menschenhaar
- Lab / Rennet (wenn nicht mikrobiell)
- Blutplasma / Blutmehl

### 4. Community-Features

#### 4.1 Produkt-Beiträge

- Nutzer können **neue Produkte** hinzufügen (Foto + Zutatenliste + Barcode)
- Nutzer können **bestehende Einträge korrigieren**
- **Review-System**: Community-Beiträge werden von verifizierten Reviewern geprüft
- **Gamification**: Punkte/Badges für aktive Beiträger

#### 4.2 Bewertungssystem

```
Produkt-Bewertung:
├── Automatische Bewertung (Halal-Engine)
├── Community-Bewertung (Nutzer-Votes)
└── Verifizierte Bewertung (Reviewer/Gelehrte)
     → Höchste Priorität
```

#### 4.3 Community-Rollen

| Rolle | Berechtigung |
|-------|-------------|
| **Nutzer** | Produkte scannen, bewerten, melden |
| **Beiträger** | Produkte hinzufügen, Zutaten korrigieren |
| **Reviewer** | Beiträge prüfen und freigeben |
| **Moderator** | Inhalte moderieren, Nutzer verwalten |
| **Admin** | Vollzugriff, Datenbank verwalten |

#### 4.4 Diskussionen & Feedback

- Kommentarfunktion pro Produkt
- Meldung von falschen Einträgen
- Vorschläge für neue Zutaten

---

## 🎨 UI/UX Design

### Farbschema

| Element | Farbe | Hex |
|---------|-------|-----|
| Primär (Halal/Vertrauen) | Smaragdgrün | `#10B981` |
| Sekundär (Akzent) | Gold | `#F59E0B` |
| Haram/Warnung | Korallrot | `#EF4444` |
| Zweifelhaft | Bernstein | `#F97316` |
| Hintergrund (Dark) | Dunkelblau-Grau | `#0F172A` |
| Hintergrund (Light) | Warmweiß | `#FAFAF9` |
| Text (Dark Mode) | Hellgrau | `#E2E8F0` |

### Screens

#### 1. Onboarding (3 Slides)
- Willkommen & Erklärung
- Kamera-Berechtigung
- Sprache & Präferenzen (Madhab-Auswahl optional)

#### 2. Homescreen
- Großer Scan-Button (zentral)
- Letzte Scans (horizontale Scrollliste)
- Tagesstatistik ("Heute 5 Produkte geprüft")
- Community-Highlights

#### 3. Scanner-Screen
- Vollbild-Kamera mit Scan-Rahmen
- Animierter Scan-Indikator
- Sofortiges Ergebnis als Bottom-Sheet

#### 4. Ergebnis-Screen
- Großes Status-Icon (✅/❌/⚠️)
- Produktname + Bild
- Zutatenliste mit farblicher Markierung jeder Zutat
- Detaillierte Erklärung für jede problematische Zutat
- Community-Bewertung
- "Teilen"-Button

#### 5. Produktdetail-Screen
- Vollständige Zutatenliste
- Nährwertinformationen
- Hersteller
- Alternativen (halal-zertifizierte Alternativen)
- Community-Kommentare

#### 6. Verlauf-Screen
- Chronologische Liste aller gescannten Produkte
- Filter: Alle / Halal / Haram / Zweifelhaft
- Suchfunktion

#### 7. Community-Screen
- Neueste Beiträge
- Top-Beiträger Leaderboard
- Offene Reviews
- Diskussionen

#### 8. Profil-Screen
- Scan-Statistiken
- Beiträge & Badges
- Einstellungen (Sprache, Madhab, Dark/Light Mode)

---

## 🗃️ Datenmodell

### Lokale Datenbank (Room)

```kotlin
// Produkt
@Entity(tableName = "products")
data class Product(
    @PrimaryKey val barcode: String,
    val name: String,
    val brand: String?,
    val imageUrl: String?,
    val ingredients: String,         // Komma-separierte Zutatenliste
    val halalStatus: HalalStatus,    // HALAL, HARAM, MASHBOOH, UNKNOWN
    val confidence: Float,           // 0.0 - 1.0
    val source: DataSource,          // LOCAL, API, OPENFOODFACTS, COMMUNITY
    val verifiedBy: String?,         // Reviewer ID
    val lastUpdated: Long,
    val isCached: Boolean = true
)

// Zutat
@Entity(tableName = "ingredients")
data class Ingredient(
    @PrimaryKey val id: String,      // z.B. "E471" oder "gelatine"
    val namesDe: String,
    val namesEn: String,
    val namesTr: String?,
    val namesAr: String?,
    val category: IngredientCategory, // HALAL, HARAM, MASHBOOH, UNKNOWN
    val reason: String?,
    val source: String?,
    val alternatives: String?         // JSON Array
)

// Scan-Verlauf
@Entity(tableName = "scan_history")
data class ScanEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val barcode: String,
    val productName: String,
    val halalStatus: HalalStatus,
    val scannedAt: Long
)

enum class HalalStatus { HALAL, HARAM, MASHBOOH, UNKNOWN }
enum class IngredientCategory { HALAL, HARAM, MASHBOOH, UNKNOWN }
enum class DataSource { LOCAL, API, OPENFOODFACTS, COMMUNITY }
```

### Backend-Datenbank (Firestore / Supabase)

```
products/
  ├── {barcode}/
  │     ├── name: String
  │     ├── brand: String
  │     ├── ingredients: [String]
  │     ├── halalStatus: String
  │     ├── confidence: Number
  │     ├── verifiedBy: String?
  │     ├── communityVotes: { halal: N, haram: N, mashbooh: N }
  │     ├── comments: [Comment]
  │     └── lastUpdated: Timestamp

users/
  ├── {uid}/
  │     ├── displayName: String
  │     ├── role: String
  │     ├── points: Number
  │     ├── badges: [String]
  │     ├── contributions: Number
  │     └── joinedAt: Timestamp

ingredients/
  ├── {ingredientId}/
  │     ├── names: { de, en, tr, ar }
  │     ├── category: String
  │     ├── reason: String
  │     └── lastUpdated: Timestamp

community_submissions/
  ├── {submissionId}/
  │     ├── barcode: String
  │     ├── submittedBy: String
  │     ├── status: "pending" | "approved" | "rejected"
  │     ├── reviewedBy: String?
  │     └── submittedAt: Timestamp
```

---

## 🔌 Externe APIs

### 1. Open Food Facts API (Primäre Datenquelle)

- **URL**: `https://world.openfoodfacts.org/api/v2/product/{barcode}.json`
- **Kosten**: Kostenlos (Open Source)
- **Daten**: Zutatenliste, Nährwerte, Bilder, Marke
- **Nutzung**: Fallback wenn Produkt nicht in eigener DB

### 2. Google ML Kit (Barcode Scanning)

- **Nutzung**: On-Device Barcode-Erkennung
- **Kosten**: Kostenlos
- **Vorteil**: Funktioniert offline

### 3. Firebase / Supabase (Backend)

| Option | Vorteile | Nachteile |
|--------|----------|-----------|
| **Firebase** | Einfache Auth, Firestore, kostenloser Tier | Vendor Lock-in, Google-Abhängigkeit |
| **Supabase** | Open Source, PostgreSQL, Self-Hosting möglich | Etwas mehr Setup |

> [!IMPORTANT]
> **Empfehlung**: **Supabase** – Open Source passt besser zum gemeinnützigen Charakter des Projekts. Kann bei Bedarf selbst gehostet werden.

---

## 🌍 Sprachen (i18n)

| Priorität | Sprache | Code |
|-----------|---------|------|
| 1 | Deutsch | `de` |
| 2 | Englisch | `en` |
| 3 | Türkisch | `tr` |
| 4 | Arabisch | `ar` |
| 5 | Französisch | `fr` |

> RTL-Support für Arabisch wird von Anfang an berücksichtigt.

---

## 📦 Tech-Stack (Android)

| Bereich | Technologie |
|---------|-------------|
| Sprache | Kotlin |
| UI Framework | Jetpack Compose + Material 3 |
| Navigation | Compose Navigation |
| Lokale DB | Room (SQLite) |
| Netzwerk | Retrofit + OkHttp |
| DI | Hilt (Dagger) |
| Kamera/Scanner | CameraX + ML Kit |
| Async | Kotlin Coroutines + Flow |
| Bilder | Coil |
| Backend | Supabase (Auth, Database, Storage) |
| Analytics | Firebase Analytics (optional, datenschutzfreundlich) |
| Crash Reporting | Firebase Crashlytics |

---

## 🚀 Phasenplan

### Phase 1 – MVP (4-6 Wochen)

- [x] Spezifikation erstellen
- [ ] Projekt-Setup (Kotlin + Jetpack Compose)
- [ ] Barcode-Scanner (ML Kit + CameraX)
- [ ] Lokale Zutatendatenbank (500+ Zutaten)
- [ ] Halal-Engine (Zutatenabgleich)
- [ ] Open Food Facts Integration
- [ ] Ergebnis-Anzeige (Halal/Haram/Mashbooh)
- [ ] Scan-Verlauf
- [ ] Basic UI (Homescreen, Scanner, Ergebnis)
- [ ] Offline-Modus

### Phase 2 – Backend & Community (3-4 Wochen)

- [ ] Supabase Backend Setup
- [ ] User-Authentifizierung (anonym + Google/Email)
- [ ] Produkt-Datenbank (Cloud)
- [ ] Community-Beiträge (Produkte hinzufügen)
- [ ] Review-System
- [ ] Sync (Offline → Online)

### Phase 3 – Polish & Features (2-3 Wochen)

- [ ] Onboarding Flow
- [ ] Community-Screen
- [ ] Gamification (Punkte, Badges)
- [ ] Produktalternativen
- [ ] Mehrsprachigkeit (DE, EN, TR, AR)
- [ ] Dark/Light Mode
- [ ] Push-Benachrichtigungen

### Phase 4 – iOS & Veröffentlichung

- [ ] iOS App (SwiftUI oder KMP)
- [ ] Play Store Veröffentlichung
- [ ] App Store Veröffentlichung
- [ ] Landing Page
- [ ] Community-Aufbau

---

## 🔒 Datenschutz & Ethik

- **Keine Werbung** – Die App ist werbefrei
- **Minimale Datenerhebung** – Nur notwendige Daten
- **Anonyme Nutzung** möglich (kein Zwangs-Login für Scans)
- **Open Source** – Der Code wird auf GitHub veröffentlicht
- **DSGVO-konform** – Datenschutzerklärung, Datenexport, Löschrecht
- **Transparente Quellen** – Jede Bewertung zeigt ihre Quelle

---

## 🤲 Islamische Richtlinien

> [!IMPORTANT]
> Die App ersetzt **keine** religiöse Beratung. Bei Zweifelsfällen wird empfohlen, einen Gelehrten zu konsultieren.

- Die Halal-Bewertung basiert auf den **allgemein anerkannten Grundsätzen** der islamischen Jurisprudenz (Fiqh)
- Bei umstrittenen Themen (z.B. E-Nummern, Alkohol als Trägerstoff) werden **verschiedene Meinungen** dargestellt
- Optionale **Madhab-Einstellung** (Hanafi, Maliki, Shafi'i, Hanbali) für abweichende Bewertungen
- Quellen werden transparent angegeben

---

## 📊 Erfolgsmetriken (KPIs)

| Metrik | Ziel (6 Monate) |
|--------|-----------------|
| Downloads | 10.000+ |
| Monatlich aktive Nutzer | 3.000+ |
| Produkte in der DB | 50.000+ |
| Community-Beiträge | 5.000+ |
| Bewertung im Play Store | 4.5+ ⭐ |
| Zutatendatenbank | 1.000+ Einträge |

---

## 💰 Finanzierung (Non-Profit)

Da das Projekt gemeinnützig ist:

- **Keine Werbung**
- **Keine In-App-Käufe**
- Mögliche Finanzierung durch:
  - Spenden (In-App + Website)
  - Crowdfunding (LaunchGood, GoFundMe)
  - Islamische Stiftungen (Waqf)
  - Grants (Google.org, Mozilla Foundation)
  - Moschee-Kooperationen

---

## ⚡ Entscheidungen & Offene Punkte

### ✅ Entschieden

| Frage | Entscheidung |
|-------|-------------|
| **Backend** | **Supabase** – Open Source, passt zum Non-Profit-Charakter |
| **Madhab-Unterstützung** | **Ja, alle 4 Madhab** – Hanafi, Maliki, Shafi'i, Hanbali. Nutzer wählt im Profil, Bewertungen passen sich entsprechend an |

### 🔜 Auf später verschoben

| Frage | Status |
|-------|--------|
| **Cross-Platform (iOS)** | Noch offen – KMP vs. native iOS wird später entschieden |
| **Moderationssystem** | Details (Reviewer-Anzahl, Freigabe-Prozess) werden in Phase 2 definiert |
| **KI-Integration** | Wird separat besprochen – Optionen: Cloud-API (z.B. GPT) oder On-Device SLM |
