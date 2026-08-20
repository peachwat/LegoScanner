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
prezentowane są wraz ze stopniem pewności, detekcje poniżej przyjętego progu kierowane
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

| Objaw                               | Przyczyna |
|-------------------------------------|---|
| `SDK location not found`            | Brak pliku `local.properties` lub nieprawidłowa wartość `sdk.dir` |
| `Gradle JVM version is incompatible` | Wersja JDK spoza zakresu obsługiwanego przez Gradle 8.13 |
| `index.lock: File exists`           | Przerwana operacja Git; wymagane usunięcie pliku `.git/index.lock` |
| `Komunikat „Błędny klucz API"`        | Brak wartości `REBRICKABLE_API_KEY` lub pominięta synchronizacja projektu |
| `Komunikat „Nie znaleziono zestawu"`  | Numer zestawu podany bez sufiksu wersji, np. `30510` zamiast `30510-1` |

## Architektura

W projekcie zastosowano podział warstwowy, oddzielający dostęp do danych od logiki
prezentacji. Stan ekranu opisany jest typem zamkniętym, co pozwala
obsłużyć wszystkie warianty - ładowanie, powodzenie i błąd - w sposób kompletny
i sprawdzany na etapie kompilacji.

```
app/src/main/java/com/example/legoscanner/
├── Config.kt                  parametry i progi, klucze z BuildConfig
├── MainActivity.kt            nawigacja dolna, obsługa obszarów systemowych
├── HomeFragment.kt            ekran listy elementów zestawu
├── CameraFragment.kt          akwizycja obrazu i wyniki rozpoznania
├── HistoryFragment.kt         historia skanowań i eksport danych
├── PartsAdapter.kt            adapter listy elementów
├── data/
│   ├── RebrickableModels.kt   model danych inwentarza
│   ├── RebrickableApi.kt      punkty końcowe Rebrickable
│   ├── RoboflowModels.kt      model danych detekcji
│   ├── RoboflowApi.kt         punkt końcowy usługi detekcji
│   ├── ApiClient.kt           konfiguracja klientów HTTP
│   ├── PartsRepository.kt     pobieranie inwentarza, typowana obsługa błędów
│   ├── DetectionRepository.kt rozpoznanie, dopasowanie koloru, filtr zestawu
│   ├── ClassMapping.kt        odwzorowanie klas modelu na numery katalogowe
│   ├── SetStore.kt            wybrany zestaw
│   ├── ProgressStore.kt       liczniki skompletowanych elementów
│   ├── ScanRecord.kt          rekord pojedynczego skanowania
│   ├── HistoryStore.kt        trwała historia skanowań
│   └── CsvExporter.kt         eksport wyników do pliku CSV
├── ui/
│   ├── HomeViewModel.kt       stan ekranu listy
│   ├── CameraViewModel.kt     stan skanowania i weryfikacji
│   ├── HistoryViewModel.kt    historia i statystyki zbiorcze
│   ├── DetectionOverlayView.kt rysowanie ramek na zdjęciu
│   ├── DetectionsAdapter.kt   lista rozpoznanych elementów
│   └── HistoryAdapter.kt      lista skanowań
└── util/
    ├── ImageUtils.kt          skalowanie, obrót, kodowanie base64
    └── ColorMatcher.kt        wyznaczanie koloru elementu z pikseli
```

### Rozpoznawanie elementu

Identyfikacja elementu przebiega dwuetapowo, ponieważ model detekcji rozpoznaje
wyłącznie kształt i nie rozróżnia barw, natomiast inwentarz Rebrickable opisuje
pozycję jako parę element-kolor.

**Kształt** wyznacza model detekcji uruchamiany w chmurze. Zwraca on klasę oraz
poziom pewności dla każdego wykrytego obiektu.

**Kolor** wyznaczany jest lokalnie w module `ColorMatcher`. Z centralnej części
ramki pobierana jest siatka pikseli, dla której liczona jest mediana każdej
składowej RGB - mediana zamiast średniej ogranicza wpływ refleksów świetlnych
oraz cieni między elementami. Uzyskana barwa porównywana jest z paletą kolorów
Rebrickable przy użyciu metryki *redmean*, uwzględniającej nierównomierną
wrażliwość oka na poszczególne składowe.

Wynik uznawany jest za niepewny, gdy dwa najbliższe kolory różnią się mniej niż
o przyjęty margines albo gdy najlepsze dopasowanie leży zbyt daleko od zmierzonej
barwy. Takie przypadki kierowane są do ręcznej weryfikacji.

### Progi decyzyjne

| Parametr | Wartość | Znaczenie                                            |
|---|---|------------------------------------------------------|
| `CONFIDENCE_ACCEPT` | 0,75 | powyżej - element zaliczany automatycznie            |
| `CONFIDENCE_REVIEW` | 0,40 | poniżej - wynik oznaczany jako niepewny              |
| `MIN_CONFIDENCE` | 0,30 | poniżej - detekcja pomijana całkowicie               |
| `AMBIGUITY_MARGIN` | 60 | minimalna różnica między dwoma najbliższymi kolorami |
| `MAX_TRUSTED_DISTANCE` | 110 | maksymalna akceptowalna odległość barwy              |

### Zbieranie danych pomiarowych

Każde skanowanie zapisywane jest w historii wraz z liczbą wykrytych obiektów,
poziomami pewności oraz statusem końcowym każdej detekcji. Rozróżniane są
przypadki zaliczone automatycznie, potwierdzone ręcznie, poprawione oraz
odrzucone przez użytkownika.

Liczba poprawek i odrzuceń stanowi bezpośrednią miarę błędów modelu, co pozwala
wyznaczyć skuteczność rozwiązania bez ręcznego zliczania wyników. Historię można
wyeksportować do pliku CSV i poddać dalszej analizie w arkuszu kalkulacyjnym.

## Stan realizacji

- [x] Pobieranie inwentarza zestawu z API Rebrickable, prezentacja listy elementów
      wraz ze zdjęciami, nazwami, kolorami i licznikiem kompletności, wybór zestawu
      oraz obsługa sytuacji wyjątkowych
- [x] Akwizycja obrazu (CameraX) oraz wczytywanie zdjęć z pamięci urządzenia,
      przesłanie do usługi detekcji i odczyt wyników wraz z poziomem pewności
- [x] Ograniczenie rozpoznawania do elementów występujących w wybranym zestawie
- [x] Wyznaczanie koloru elementu na podstawie analizy pikseli i dopasowanie
      do palety Rebrickable
- [x] Prezentacja wyników na zdjęciu wraz z ramkami i poziomem pewności
- [x] Próg pewności, automatyczne zaliczanie oraz ekran ręcznej weryfikacji
      z możliwością potwierdzenia i korekty wyniku
- [x] Historia skanowań i eksport danych pomiarowych do pliku CSV
- [ ] Własny model detekcji wytrenowany na wybranym zestawie
- [ ] Badanie skuteczności rozwiązania i analiza błędów klasyfikacji
- [ ] Analiza istniejących rozwiązań do katalogowania elementów LEGO

### Ograniczenia obecnej wersji

Wykorzystywany jest publiczny model detekcji rozpoznający ograniczoną liczbę
klas, co nie pozwala na obsługę dowolnego zestawu. Model uruchamiany jest
w chmurze, a więc aplikacja wymaga połączenia z internetem. Wyznaczanie koloru
opiera się na uśrednionej barwie i jest wrażliwe na oświetlenie oraz na tło
widoczne wewnątrz ramki detekcji; elementy przezroczyste rozpoznawane są
najsłabiej.

## Wykorzystane technologie

Kotlin, Android SDK 36, Retrofit, Gson, OkHttp, Coil, RecyclerView, ViewModel,
Kotlin Coroutines i StateFlow, Navigation Component, CameraX.

## Źródła danych

- [Rebrickable API](https://rebrickable.com/api/v3/docs/) - katalog zestawów i elementów LEGO
- [Roboflow Serverless API](https://docs.roboflow.com/guides/run-model-serverless-api) - detekcja obiektów w chmurze
