# Thor Game Catalog

Natívna Android aplikácia navrhnutá pre AYN Thor. Zobrazuje online prehľad retro
hier ako moderný obchod, ale neobsahuje ani nesťahuje ROM/ISO súbory.

## Čo aplikácia obsahuje

- online katalóg načítavaný po 30 hrách z IGDB
- PlayStation 1, PlayStation 2, PSP, GameCube, Wii, Dreamcast, Nintendo DS, 3DS
  a Nintendo Switch
- adaptívnu mriežku vhodnú na výšku aj na šírku
- ovládanie dotykom, D-padom, tlačidlom A/B a analógovou páčkou
- vyhľadávanie podľa názvu alebo konzoly
- filtre platforiem, ktoré odrolujú spolu s katalógom
- zapamätaný filter **Iba natívne hry**, ktorý skryje staré kompatibilné a
  Virtual Console tituly
- zoradenie A–Z, Z–A, podľa IGDB popularity, hodnotenia, počtu hodnotení a dátumu
- hodnotenie hry priamo na karte aj v detaile
- **Surprise me** s tromi náhodnými hrami podľa vybranej platformy
- obaly, screenshoty a metadáta načítavané z internetu
- celoobrazovkovú galériu screenshotov s dotykovým gestom, zoomom a D-padom
- voliteľné retro gameplay videá a médiá zo ScreenScraper
- kompaktné menu Katalóg/Nastavenia vpravo hore
- vlastnú preferovanú webovú stránku pre každú platformu
- URL šablóny s premennými `{title}`, `{platform}`, `{region}` a `{year}`
- API údaje uložené šifrovane pomocou Android Keystore

V časti **Nastavenia** sa dá pre každú konzolu zadať samostatná HTTP alebo HTTPS
adresa. Napríklad `https://example.com/search?q={title}` doplní do odkazu názov
otvorenej hry. Adresa bez premennej sa otvorí presne tak, ako bola zadaná.
Aplikácia nemá žiadny prednastavený externý herný web ani lokálny zoznam hier.

Zvolené zoradenie aj filter natívnych hier si aplikácia zapamätá. Režim
**Surprise me** vyberá tri hry
s obalom a dostatočným počtom hodnotení, pričom pri zobrazení všetkých platforiem
uprednostní rozdielne konzoly a neopakuje bezprostredne predchádzajúci výber.

## Online API

V aplikácii otvor **Menu > Nastavenia** a zadaj vlastné IGDB `Client ID` a
`Client Secret`. IGDB je povinný zdroj online katalógu. Voliteľné vývojárske a
používateľské údaje ScreenScraper doplnia pri otvorení hry krátke MP4 video,
screenshoty a regionálne obaly. Tajné údaje sa nepridávajú do APK ani repozitára.

## Zostavenie v Android Studio

1. Nainštaluj Android Studio s Android SDK 35 a JDK 17.
2. Otvor priečinok `ThorGameCatalog` ako projekt.
3. Počkaj na dokončenie Gradle synchronizácie.
4. Vyber **Build > Build App Bundle(s) / APK(s) > Build APK(s)**.
5. Výsledok bude v `app/build/outputs/apk/debug/app-debug.apk`.

Minimálna verzia systému je Android 8.0 (API 26). Aplikácia používa Kotlin,
Jetpack Compose a Material 3.

## Automatické zostavenie cez GitHub

Projekt obsahuje workflow `.github/workflows/build-apk.yml`. Po nahratí projektu
do GitHub repozitára otvor **Actions > Build Android APK > Run workflow**.
Hotové APK bude po zostavení dostupné medzi Artifacts.

## Poznámka k obsahu

Názvy, obaly, screenshoty, popisy a videá sa načítavajú za behu z nastavených
online služieb a nie sú pribalené v aplikácii. Ochranné známky a médiá patria
príslušným vlastníkom.
