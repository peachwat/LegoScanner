# LegoScanner

Aplikacja mobilna do wspomagania kompletowania zestawów LEGO z wykorzystaniem analizy obrazu.

## Opis

Celem projektu jest zaprojektowanie i implementacja aplikacji mobilnej wspierającej
użytkownika w sprawdzaniu kompletności zestawu LEGO na podstawie jego listy elementów.
Aplikacja pobiera inwentarz wybranego zestawu z otwartej bazy Rebrickable, umożliwia
identyfikację posiadanych klocków poprzez analizę zdjęć wykonywanych przez użytkownika
oraz automatyczne oznaczanie elementów jako dostępne lub brakujące.

Istotnym założeniem jest ograniczenie zakresu rozpoznawania do elementów występujących
w jednym, zdefiniowanym zestawie, co upraszcza problem klasyfikacji i pozwala skupić się
na praktycznych aspektach zastosowania metod wizji komputerowej. Wyniki rozpoznania
prezentowane są wraz ze stopniem pewności; detekcje poniżej przyjętego progu kierowane
są do ręcznej weryfikacji przez użytkownika, co zwiększa odporność systemu na błędy.

## Wymagania

Do uruchomienia projektu wymagane są:

- Android Studio w wersji Ladybug lub nowszej
- JDK w wersji 21 - Gradle 8.13 nie obsługuje wersji nowszych niż 23
- Android SDK 36
- urządzenie z systemem Android 7.0 (API 24) lub nowszym albo emulator

## Konfiguracja środowiska

### Pobranie repozytorium

```bash
git clone https://github.com/peachwat/LegoScanner.git
cd LegoScanner
```

### Utworzenie pliku konfiguracyjnego

Plik `local.properties` nie jest przechowywany w repozytorium, ponieważ zawiera ścieżki
zależne od stanowiska pracy oraz klucze dostępu do zewnętrznych usług. W repozytorium
znajduje się jedynie wzorzec `local.properties.example`, który należy skopiować i zmienić
nazwę kopii na `local.properties`.

Wymagane wartości:

```properties
sdk.dir=C\:\\Users\\NAZWA_UZYTKOWNIKA\\AppData\\Local\\Android\\Sdk

REBRICKABLE_API_KEY=
ROBOFLOW_API_KEY=
ROBOFLOW_MODEL_ID=
```

**`sdk.dir`** - ścieżka do Android SDK. Można ją odczytać w oknie
File → Settings → Languages & Frameworks → Android SDK. Format wymaga podwójnych
ukośników odwrotnych oraz znaku `\:` po literze dysku.

**`REBRICKABLE_API_KEY`** - klucz dostępu do API Rebrickable, generowany po założeniu
bezpłatnego konta pod adresem https://rebrickable.com/api/

**`ROBOFLOW_API_KEY`** - klucz dostępu do usługi Roboflow, dostępny pod adresem
https://app.roboflow.com/settings/api

**`ROBOFLOW_MODEL_ID`** - identyfikator modelu detekcji w formacie `nazwa-projektu/wersja`.
Modele publiczne udostępniane są w serwisie https://universe.roboflow.com

Parametry dotyczące Roboflow wykorzystywane są dopiero w module analizy obrazu
i na etapie pobierania inwentarza mogą pozostać puste.

### Konfiguracja JDK

W oknie File → Settings → Build, Execution, Deployment → Build Tools → Gradle
należy ustawić parametr **Gradle JDK** na wersję 21. Gradle 8.13 obsługuje wersje
od 8 do 23; przy nowszej wersji synchronizacja kończy się błędem niezgodności.

### Uruchomienie

Po uzupełnieniu konfiguracji należy wykonać synchronizację projektu
(File → Sync Project with Gradle Files), a następnie uruchomić aplikację.

## Najczęstsze problemy

| Objaw | Przyczyna |
|---|---|
| `SDK location not found` | Brak pliku `local.properties` lub nieprawidłowa wartość `sdk.dir` |
| `Gradle JVM version is incompatible` | Wersja JDK spoza zakresu obsługiwanego przez Gradle 8.13 |
| `index.lock: File exists` | Przerwana operacja Git; wymagane usunięcie pliku `.git/index.lock` |
| Komunikat „Błędny klucz API" | Brak wartości `REBRICKABLE_API_KEY` lub pominięta synchronizacja projektu |
| Komunikat „Nie znaleziono zestawu" | Numer zestawu podany bez sufiksu wersji, np. `30510` zamiast `30510-1` |

## Architektura

W projekcie zastosowano podział warstwowy, oddzielający dostęp do danych od logiki
prezentacji. Stan ekranu opisany jest typem zamkniętym (`sealed interface`), co pozwala
obsłużyć wszystkie warianty - ładowanie, powodzenie i błąd - w sposób kompletny
i sprawdzany na etapie kompilacji.

```
app/src/main/java/com/example/legoscanner/
├── Config.kt                 parametry konfiguracyjne odczytywane z BuildConfig
├── MainActivity.kt           nawigacja dolna oraz obsługa obszarów systemowych
├── HomeFragment.kt           ekran listy elementów zestawu
├── CameraFragment.kt         ekran akwizycji obrazu
├── HistoryFragment.kt        historia skanowań
├── PartsAdapter.kt           adapter listy wykorzystujący DiffUtil
├── data/
│   ├── RebrickableModels.kt  model danych odpowiedzi API
│   ├── RebrickableApi.kt     deklaracja punktów końcowych (Retrofit)
│   ├── ApiClient.kt          konfiguracja klienta HTTP
│   ├── PartsRepository.kt    pobieranie inwentarza, typowana obsługa błędów
│   └── SetStore.kt           trwałe przechowywanie wybranego zestawu
└── ui/
    └── HomeViewModel.kt      stan ekranu i logika prezentacji
```

## Stan realizacji

- [x] Pobieranie inwentarza zestawu z API Rebrickable, prezentacja listy elementów
      wraz ze zdjęciami, nazwami, kolorami i licznikiem kompletności, wybór zestawu
      oraz obsługa sytuacji wyjątkowych
- [ ] Akwizycja obrazu (CameraX) i przesłanie zdjęcia do usługi detekcji
- [ ] Mechanizm progu pewności oraz ekran ręcznej weryfikacji wyników
- [ ] Własny model detekcji wytrenowany na wybranym zestawie
- [ ] Badanie skuteczności rozwiązania i analiza błędów klasyfikacji

## Wykorzystane technologie

Kotlin, Android SDK 36, Retrofit, Gson, OkHttp, Coil, RecyclerView, ViewModel,
Kotlin Coroutines i StateFlow, Navigation Component, CameraX.

## Źródła danych

- [Rebrickable API](https://rebrickable.com/api/v3/docs/) - katalog zestawów i elementów LEGO
- [Roboflow Serverless API](https://docs.roboflow.com/guides/run-model-serverless-api) - detekcja obiektów w chmurze
