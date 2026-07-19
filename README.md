# Thor Game Catalog

Natívna Android aplikácia navrhnutá pre AYN Thor. Zobrazuje prehľad retro hier
ako moderný obchod, ale neobsahuje ani nesťahuje ROM/ISO súbory.

## Čo aplikácia obsahuje

- 80 ručne vybraných hier, po 10 pre každú platformu
- PlayStation 1, PlayStation 2, PSP, GameCube, Wii, Dreamcast, Nintendo DS a 3DS
- adaptívnu mriežku vhodnú na výšku aj na šírku
- ovládanie dotykom a štandardné Compose focus správanie pre D-pad
- vyhľadávanie podľa názvu alebo konzoly
- rýchle filtre platforiem
- obaly načítavané z projektu Libretro Thumbnails
- detail hry so slovenským popisom
- spodnú navigáciu medzi katalógom a nastaveniami
- vlastnú preferovanú webovú stránku pre každú platformu
- URL šablóny s premennými `{title}`, `{platform}`, `{region}` a `{year}`
- lokálne uloženie nastavení, ktoré zostanú zachované po reštarte aplikácie

V časti **Nastavenia** sa dá pre každú konzolu zadať samostatná HTTP alebo HTTPS
adresa. Napríklad `https://example.com/search?q={title}` doplní do odkazu názov
otvorenej hry. Adresa bez premennej sa otvorí presne tak, ako bola zadaná.
Aplikácia nemá žiadny prednastavený externý herný web.

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

## Pridanie ďalšej hry

Katalóg je v súbore:

`app/src/main/assets/games.json`

Každá položka má tento tvar:

```json
{
  "id": "ps1-crash-bandicoot",
  "title": "Crash Bandicoot",
  "platform": "ps1",
  "year": 1996,
  "description": "Krátky slovenský popis.",
  "thumbnailName": "Crash Bandicoot (USA)"
}
```

Povolené hodnoty `platform` sú `ps1`, `ps2`, `psp`, `gamecube`, `wii`,
`dreamcast`, `nds` a `n3ds`. `thumbnailName` musí zodpovedať názvu obrázka v
príslušnom repozitári Libretro Thumbnails. Ak sa obrázok nenájde alebo zariadenie
nemá internet, aplikácia zobrazí vlastnú farebnú náhradu.

## Poznámka k obsahu

Zdrojový kód a lokálne popisy sú súčasťou tohto projektu. Obaly hier sa načítajú
za behu zo samostatných verejných repozitárov Libretro Thumbnails a nie sú
pribalené v aplikácii. Ochranné známky a obaly patria príslušným vlastníkom.
