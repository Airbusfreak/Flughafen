# DresdenAirlines – Steampunk-Serverinstallation (Paper 26.2)

Diese Anleitung ist für einen frischen Entwicklungsserver gedacht, auf dem noch keine Spielerwelten oder Flughäfen existieren. Sie verwendet nur serverseitige Datapacks/Plugins und Resourcepacks; Spieler benötigen keine Client-Mods.

## Geprüfte Versionsbasis

| Komponente | Auswahl | Status |
|---|---|---|
| Server | Paper 26.2, neuester Stable-Build der 26.2-Linie | Versionslinie vorhanden; nimm im Paper-Download den neuesten als **Release** markierten Build |
| Java | Java 25 | Von Paper für 26.x gefordert |
| Oberwelt | [Terralith 2.6.4](https://modrinth.com/datapack/terralith/versions?g=26.2), Datapack für 26.2 | Modrinth führt eine explizite 26.2-Datapack-Version |
| Nether | [Amplified Nether 1.2.15](https://modrinth.com/datapack/amplified-nether/version/xIayvf8F), Datapack für 26.2 | Modrinth führt 26.2; nicht mit Incendium kombinieren |
| End | [Stellarity](https://modrinth.com/datapack/stellarity), Datapack-Version für 26.2 | Modrinth führt 26.2 |
| Strukturen | [Dungeons and Taverns](https://modrinth.com/datapack/dungeons-and-taverns), Datapack-Version für 26.2 | Modrinth führt 26.2; ergänzt Strukturen, nicht eine neue Terrain-Engine |
| Kochen/Pflanzen | [Crop & Kettle](https://modrinth.com/datapack/crop-and-kettle), Datapack-Version für 26.2 | Modrinth führt 26.2; enthält eigene Resourcepack-Datei für die Item-Optik |
| Steampunk-Look | [Steampunk Texture Pack](https://modrinth.com/resourcepack/steampunk-texture-pack), Version für 26.2 | Modrinth führt 26.2 |
| DresdenAirlines | JAR und passende Packs aus GitHub Actions | CI muss grün sein; danach Artefakt für 26.2 wählen |

Quellen zur Versionsauswahl: [Paper-Installationsdoku](https://docs.papermc.io/paper/getting-started/), [Terralith](https://modrinth.com/datapack/terralith), [Amplified Nether](https://modrinth.com/datapack/amplified-nether), [Stellarity](https://modrinth.com/datapack/stellarity), [Dungeons and Taverns](https://modrinth.com/datapack/dungeons-and-taverns), [Mechanism](https://modrinth.com/plugin/mechanism), [Orbit Items](https://modrinth.com/plugin/orbit-items), [Crop & Kettle](https://modrinth.com/datapack/crop-and-kettle), [Steampunk Texture Pack](https://modrinth.com/resourcepack/steampunk-texture-pack).

**Kompatibilitätshinweis:** Die Projektseiten nennen Minecraft 26.2 als kompatibel. Das beweist nicht, dass alle Packs untereinander oder mit DresdenAirlines bereits gemeinsam im Spiel getestet wurden. Deshalb erst mit Testwelt starten. Terralith warnt davor, es aus einer bestehenden Welt zu entfernen oder nachträglich zu einer bestehenden Welt hinzuzufügen.

## Teil A – Paper 26.2 vorbereiten

**Weltname:** Die Welt heißt bei dir `world`. Behalte `level-name=world` in `server.properties`; der Plugin-Config-Eintrag `airports.allowed-worlds: - world` ist bereits darauf eingestellt.

1. Öffne das CloudNord-Panel und stoppe den Server.
2. Sichere den aktuellen Serverordner, auch wenn die Welten bereits gelöscht wurden. So kannst du Konfigurationen wiederherstellen.
3. Wähle Paper **26.2** und Java **25** im Panel. Verwende nicht den Vanilla-Server und keinen Snapshot-Build.
4. Setze den Weltnamen in `server.properties` auf `level-name=world`. Falls CloudNord dafür ein eigenes Feld im Panel hat, nutze dieses Feld.
5. Setze in `server.properties` außerdem `level-seed=` auf leer oder entferne die Zeile nicht manuell, wenn das Panel die Datei automatisch verwaltet. Optional kannst du später einen festen Seed setzen.
6. Noch **nicht** starten: Die Weltgenerierungs-Datapacks müssen vor dem ersten Start vorhanden sein.

## Teil B – Datapacks vor der Welterstellung installieren

Lade von den oben verlinkten offiziellen Modrinth-Projektseiten jeweils die **Data Pack**-Datei für Minecraft 26.2 herunter, keine Fabric-/Forge-/NeoForge-Mod-JARs.

1. Lege im Server-Dateimanager den Ordner `world/datapacks` an. Er muss neben `server.properties` im Server-Hauptordner liegen.
2. Lade die ZIP-Dateien für **Terralith**, **Amplified Nether**, **Stellarity**, **Dungeons and Taverns** und **Crop & Kettle** direkt in diesen Ordner. Die ZIP-Dateien nicht in weitere Unterordner entpacken.
3. Prüfe, dass jede ZIP-Datei im Stamm eine `pack.mcmeta` und die zugehörigen Daten enthält. Wenn der Download eine zusätzliche oberste Ordner-Ebene hat, wähle die ZIP, die direkt mit `pack.mcmeta` beginnt.
4. Jetzt den Server starten. Der erste Weltstart erzeugt die neue Welt mit den installierten Datapacks.
5. Warte, bis der Server vollständig gestartet ist. Prüfe in der Konsole auf Meldungen zu ungültigen Datapacks oder fehlenden Funktionen. Im Spiel als OP `/datapack list enabled` ausführen.

### Reihenfolge und mögliche Konflikte

- **Terralith** übernimmt die Oberwelt-Biome und das Terrain.
- **Amplified Nether** übernimmt die Nether-Terrainform; **Amplified Nether Height** ergänzt die dafür vorgesehene Höhen-/Logical-Height-Anpassung. Nicht zusätzlich Incendium installieren; dessen Projektseite erklärt diese Kombination für inkompatibel.
- **Stellarity** gestaltet das End neu und ergänzt Waffen/Mechaniken.
- **Dungeons and Taverns** ergänzt Strukturen. Die gemeinsame Kombination mit Stellarity und den Weltgenerierungs-Packs wurde nicht in einer Ingame-Testwelt verifiziert.
- **Crop & Kettle** ergänzt Kochen, Pflanzen und viele Rezepte. Es ist ein Datapack, kein Paper-Plugin und kein vollständiger Create-Ersatz.

Wenn der Server beim Weltstart einen Datapack-Fehler meldet, stoppe ihn, entferne nur das zuletzt hinzugefügte Pack und teste erneut. Lösche nicht wahllos Dateien aus der neu erstellten Welt.

## Teil C – DresdenAirlines installieren

1. Öffne [GitHub Actions für DresdenAirlines](https://github.com/Airbusfreak/Flughafen/actions).
2. Öffne den neuesten grünen Build und scrolle zu **Artifacts**.
3. Lade diese drei Dateien für 26.2 herunter:
   - `DresdenAirlines-26.2`
   - `DresdenAirlines-Resourcepack-26.2`
   - `DresdenAirlines-Datapack-26.2`
4. Entpacke den Artifact-Download, falls GitHub ihn als äußere ZIP-Datei herunterlädt. Lege nur `DresdenAirlines-26.2.jar` in den Ordner `plugins`.
5. Das DresdenAirlines-Datapack ist optional für die Fluglinien-Funktionen. Wenn du es verwenden möchtest, kopiere seinen Inhalt in `world/datapacks`, sodass `pack.mcmeta` direkt im Datapack-Stamm liegt.
6. Starte den Server und kontrolliere die Konsole auf Fehler. Prüfe danach `plugins/DresdenAirlines/config.yml`: Die erlaubte Welt ist bereits auf `world` eingestellt.
7. Teste die Flughafen-Generierung erst in einem abgelegenen Bereich. Terralith hat teils steiles Gelände; DresdenAirlines sucht selbst geeignete ebene Flächen. Automatisch generierte Flughäfen können deshalb seltene Versuche benötigen.

## Teil D – Resourcepacks für alle Spieler

Minecraft-Server verteilt standardmäßig nur **eine** Server-Resourcepack-URL. DresdenAirlines, Steampunk Texture Pack, Crop & Kettle und optional Dungeons and Taverns können jeweils eigene Resourcepack-Dateien haben. Nicht einfach mehrere ZIP-Dateien nacheinander in die Server-Resourcepack-Einstellung eintragen.

1. Lade die Version 26.2 des Steampunk Texture Packs herunter.
2. Lade die zu Crop & Kettle 1.3.10 gehörende **Resource Pack**-Datei aus dessen Modrinth-Versionsseite herunter. Optional kannst du auch das Resourcepack von Dungeons and Taverns hinzufügen, damit dessen Schlüssel-Texturen sichtbar sind.
3. Nutze das DresdenAirlines-Resourcepack aus dem GitHub-Artefakt.
4. Erstelle einen gemeinsamen Resourcepack-Ordner und kopiere die Dateien aller drei Packs hinein, wobei die Ordnerstruktur `assets/` und `pack.mcmeta` erhalten bleibt. Bei Dateikonflikten müssen wir die Modelle und Texturen gezielt zusammenführen; überschreibe nicht blind die DresdenAirlines-Flugzeugmodelle.
5. Erzeuge aus dem zusammengeführten Ordner eine ZIP-Datei, bei der `pack.mcmeta` direkt im ZIP-Stamm liegt. Setze in der Datei die Resourcepack-Metadaten auf Format **88.0** für Minecraft 26.2.
6. Lade die fertige ZIP auf einen direkten HTTPS-Dateihost hoch, der einen echten Datei-Download liefert. Trage diese URL in die Server-Resourcepack-Einstellung im CloudNord-Panel bzw. in `server.properties` ein. Falls das Panel einen SHA-1-Hash verlangt, berechne ihn aus genau der fertigen ZIP-Datei.
7. Teste mit einem normalen Client. Die Flugzeugmodelle, Koch-Items und Steampunk-Texturen müssen sichtbar sein. Wenn es Modellkonflikte gibt, erst die Resourcepack-Zusammenführung korrigieren.

**Wichtig:** Das gemeinsame Resourcepack muss tatsächlich zusammengeführt werden; allein die Metadaten zu ändern macht die Packs nicht kompatibel. Die zusammengeführte ZIP-Datei ist nicht Teil der DresdenAirlines-CI-Artefakte, da die Drittanbieter-Packs separat heruntergeladen werden müssen.

## Teil E – Erste Tests

- `/plugins`: DresdenAirlines wird angezeigt.
- `/datapack list enabled`: die installierten Datapacks sind aktiv.
- `/airport list` und `/airport generate`: Flughafenverwaltung testen.
- Neue Chunks in der Oberwelt erkunden; Nether und End über Portale testen.
- Crop-&-Kettle-Kochbuch craften und neue Pflanzen/Rezepte prüfen.
- Mechanism-Maschinen in einem Testbereich aufbauen und prüfen, ob ihre Rotation/Interaktionen laufen.
- Mit Orbit Items die Beispielwaffen testen und danach Steampunk-Gegenstände konfigurieren.
- Resourcepack mit normalem Minecraft-Client testen.
- Vor Freigabe den Server neu starten und erneut Konsole/Logs prüfen.

## Wenn du fertig bist

Schicke mir bei einem Fehler die genaue Konsolenmeldung oder die letzten Zeilen aus `logs/latest.log`. Ändere nicht mehrere Packs gleichzeitig, damit wir die Ursache finden.
