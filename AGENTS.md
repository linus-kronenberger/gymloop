# AGENTS.md – gymloop

## Projektüberblick
**gymloop** ist eine Fitness-App auf Basis von **Kotlin Multiplatform (KMP)** und **Compose Multiplatform** für **Android, iOS und Web**, ergänzt durch einen **Ktor-Server** mit **PostgreSQL**.

Fachlicher Kern:
- Übungen anlegen und verwalten
- Persönliche Rekorde pro Übung erfassen (**Gewicht in kg** + **maximale Wiederholungen**)
- Zeitlichen Verlauf pro Übung als **Graph** visualisieren (Trend: *besser*, *gleich* oder *schlechter*)
- Serverseitige Persistenz in **PostgreSQL**

## Modulstruktur
- `:core` (`commonMain`) – Geteilte Domäne (Entities, Value Objects, Domain Services, Repository-Interfaces)
- `:app:shared` (`commonMain`, `androidMain`, `iosMain`, `jvmMain`, `jsMain`/`wasmJsMain`) – Geteilte UI (Compose Multiplatform) & Präsentationslogik
- `:app:androidApp` – Android-Einstiegspunkt
- `:app:iosApp` – iOS-Einstiegspunkt (Xcode / SwiftUI Wrapper)
- `:app:webApp` – Web-Einstiegspunkt (Wasm / JS)
- `:server` – Ktor-Server, API-Endpunkte und PostgreSQL-Persistenz

---

## Kernanweisungen

### 1. Domain-Driven Design (DDD)
- **Domäne im Zentrum**: Fachliche Konzepte (z. B. `Exercise`, `PersonalRecord`, `Weight`, `Repetitions`, `ProgressTrend`) gehören nach `:core` in `commonMain`.
- **Rich Domain Model**: Invarianten und Validierungen (z. B. Gewicht > 0, Wiederholungen > 0, Berechnung von Leistungsänderungen) liegen in Entities und Value Objects – nicht in UI-Komponenten oder SQL-Queries.
- **Klare Schichttrennung**:
  - *Domain*: Keine Abhängigkeiten zu Frameworks, UI, HTTP-Clients oder Datenbanken.
  - *Application*: Use Cases / Application Services orchestrieren Abläufe.
  - *Infrastructure*: PostgreSQL-Repositories (`:server`) und API-Clients implementieren Domain-Interfaces.
  - *Presentation*: Compose-UI und ViewModels/State-Holder konsumieren nur Application-/Domain-Modelle.
- **Ubiquitous Language**: Verwende durchgängig konsistente, fachliche Begriffe in Code, API und UI.

### 2. Kotlin & Multiplatform First
- **Idiomatisches Kotlin**: Nutze Immutability (`val`, `data class`), `value class` für Domain-Typen, `sealed interface`/`sealed class` für Zustände und Ergebnisse sowie Coroutines/`Flow` für Asynchronität.
- **Maximaler Code-Share**: Business-Logik und UI gehören standardmäßig in `commonMain`. Plattformspezifische Implementierungen (`expect`/`actual`) nur dort einsetzen, wo Plattform-APIs zwingend nötig sind.
- **Typsicherheit & Fehlerbehandlung**: Keine stillen Fehler oder `null`-Hacks; Domänenfehler explizit modellieren.

### 3. Responsivität (Adaptive Compose UI)
- **Alle Zielplattformen mitdenken**: Jedes UI-Screen- und Komponenten-Design muss auf schmalen Smartphone-Displays (iOS/Android), Tablets und breiten Web-Viewports funktionieren.
- **Adaptive Layouts**: Nutze flexible Compose-Layouts (`WindowSizeClass` / `BoxWithConstraints`, `LazyColumn`/`LazyVerticalGrid`, adaptive Navigation statt fixer Pixelgrößen).
- **Responsive Charts**: Der Fortschrittsgraph je Übung muss sich dynamisch an verfügbare Breite/Höhe anpassen und klare visuelle Zustände für Verbesserung, Stagnation und Verschlechterung bieten.

### 4. Server & PostgreSQL
- **Persistenz**: Alle Übungen und Rekord-Zeitreihen werden sauber relational in PostgreSQL gespeichert (inkl. Zeitstempel für die Verlaufsauswertung).
- **API-Vertrag**: Klare, versionierbare DTO-Grenzen zwischen `:server` und den Clients; Domain-Objekte niemals direkt als DB-Tabellen oder Wire-DTOs missbrauchen.

---

## Wichtige Befehle
- Android Build: `./gradlew :app:androidApp:assembleDebug`
- Server starten: `./gradlew :server:run`
- Web (Wasm) starten: `./gradlew :app:webApp:wasmJsBrowserDevelopmentRun`
- Server Tests: `./gradlew :server:test`
- Shared Tests (Android/Wasm/iOS): `./gradlew :app:shared:testAndroidHostTest`, `./gradlew :app:shared:wasmJsTest`, `./gradlew :app:shared:iosSimulatorArm64Test`
