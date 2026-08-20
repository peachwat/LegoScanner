# LegoScanner

Aplikacja mobilna do wspomagania kompletowania zestawów LEGO z wykorzystaniem analizy obrazu.

Aplikacja pobiera listę elementów wybranego zestawu LEGO z otwartej bazy Rebrickable,
pozwala sfotografować posiadane klocki i automatycznie oznacza je jako dostępne
na podstawie rozpoznania obrazu. Wyniki poniżej progu pewności trafiają do ręcznej
weryfikacji przez użytkownika.

---

## Wymagania

- Android Studio (Ladybug lub nowsze)
- JDK 21 — Gradle 8.13 nie działa z JDK 24 i nowszymi
- Android SDK 36
- Urządzenie z Androidem 7.0 (API 24) lub emulator

---

## Konfiguracja

### 1. Sklonuj repozytorium

```bash
git clone https://github.com/peachwat/LegoScanner.git
cd LegoScanner
```

### 2. Utwórz plik `local.properties`

Skopiuj `local.properties.example` i zmień nazwę kopii na `local.properties`.

Ten plik jest w `.gitignore` i celowo nie trafia do repozytorium — zawiera
ścieżki lokalne oraz klucze API, które u każdego są inne.

### 3. Uzupełnij wartości

```properties
sdk.dir=C\:\\Users\\NAZWA_UZYTKOWNIKA\\AppData\\Local\\Android\\Sdk

REBRICKABLE_API_KEY=
ROBOFLOW_API_KEY=
ROBOFLOW_MODEL_ID=
```

**`sdk.dir`** — ścieżka do Android SDK. Podejrzysz ją w
File → Settings → Languages & Frameworks → Android SDK.
Uwaga na format: podwójne ukośniki i `\:` po literze dysku.

**`REBRICKABLE_API_KEY`** — darmowe konto na https://rebrickable.com,
klucz generuje się na https://rebrickable.com/api/

**`ROBOFLOW_API_KEY`** — darmowe konto na https://roboflow.com,
klucz na https://app.roboflow.com/settings/api

**`ROBOFLOW_MODEL_ID`** — identyfikator modelu w formacie `nazwa-projektu/wersja`.
Publiczne modele: https://universe.roboflow.com (szukaj `lego bricks`).

Pola Roboflow są potrzebne dopiero od Kroku 2 — na etapie listy elementów
mogą zostać puste.

### 4. Ustaw JDK

File → Settings → Build, Execution, Deployment → Build Tools → Gradle → **Gradle JDK**

Wybierz JDK w wersji **21**. Gradle 8.13 obsługuje JDK od 8 do 23;
przy nowszej wersji sync kończy się błędem niezgodności.

### 5. Zsynchronizuj i uruchom

File → Sync Project with Gradle Files, następnie Run.

---

## Rozwiązywanie problemów

| Objaw | Przyczyna i rozwiązanie |
|---|---|
| `SDK location not found` | Brak pliku `local.properties` lub zła ścieżka `sdk.dir` |
| `Gradle JVM version 25 is incompatible` | Ustaw Gradle JDK na 21 (punkt 4) |
| `index.lock: File exists` | Zamknij panel Commit w Android Studio, potem `rm -f .git/index.lock` |
| Aplikacja pokazuje „Błędny klucz API" | Brak `REBRICKABLE_API_KEY` w `local.properties` — po dodaniu wykonaj Sync |
| „Nie znaleziono zestawu" | Numer zestawu wymaga sufiksu wersji: `30510-1`, nie `30510` |

---

## Struktura projektu

```
app/src/main/java/com/example/legoscanner/
├── Config.kt                 klucze i ustawienia, czytane z BuildConfig
├── MainActivity.kt           nawigacja dolna, obsługa window insets
├── HomeFragment.kt           ekran listy elementów
├── CameraFragment.kt         ekran aparatu (Krok 2)
├── HistoryFragment.kt        historia skanowań (do zrobienia)
├── PartsAdapter.kt           adapter listy z DiffUtil
├── data/
│   ├── RebrickableModels.kt  klasy danych odpowiedzi API
│   ├── RebrickableApi.kt     interfejs Retrofit
│   ├── ApiClient.kt          konfiguracja OkHttp i Retrofit
│   ├── PartsRepository.kt    logika pobierania, typowane błędy
│   └── SetStore.kt           zapamiętywanie wybranego zestawu
└── ui/
    └── HomeViewModel.kt      stan ekranu jako sealed interface
```

---

## Stan prac

- [x] **Krok 1** — pobranie inwentarza zestawu z Rebrickable, lista ze zdjęciami,
      nazwami, kolorami i licznikiem `0/2`, wybór zestawu, obsługa błędów
- [ ] **Krok 2** — aparat (CameraX), wysłanie zdjęcia do Roboflow Serverless API,
      odczyt detekcji wraz z poziomem pewności
- [ ] **Krok 3** — próg pewności 75%, automatyczne zaznaczanie powyżej progu,
      ekran ręcznej weryfikacji poniżej progu
- [ ] Własny model wytrenowany na wybranym zestawie
- [ ] Testy skuteczności i analiza błędów klasyfikacji

---

## Technologie

Kotlin, Android SDK 36, Retrofit + Gson, OkHttp, Coil, RecyclerView,
ViewModel + StateFlow, Navigation Component, CameraX (Krok 2).

## Źródła danych

- [Rebrickable API](https://rebrickable.com/api/v3/docs/) — katalog zestawów i elementów LEGO
- [Roboflow Serverless API](https://docs.roboflow.com/guides/run-model-serverless-api) — detekcja obiektów w chmurze
