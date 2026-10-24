# DresdenAirlines 4.0.0

Paper plugin für die Airline- und Flughafensimulation. Unterstützte Build-Ziele:
- Minecraft 1.21.11 / Java 21
- Minecraft 26.2 / Java 25
- Minecraft 26.3 / Java 25

## Steampunk-Server

Die Installationsanleitung für eine neue Steampunk-Welt mit Terralith, Amplified Nether, Stellarity, Dungeons and Taverns, Crop & Kettle und dem passenden Resourcepack steht in [STEAMPUNK_INSTALLATION.md](STEAMPUNK_INSTALLATION.md).

**Wichtig:** Weltgenerierungs-Datapacks müssen vor der ersten Welterstellung installiert sein. Der Server muss für Minecraft 26.x mit Java 25 laufen. Erst in einer Testwelt prüfen, bevor der Server für Spieler freigegeben wird.

## Neu in 4.0.0
- realistische Flugabfertigung: Gate -> Boarding -> Taxiing -> Start -> Cruise -> Approach -> Landung
- Gepäckwagen als sichtbare Chest-Minecarts bei Abflug und Ankunft
- zufällige Betriebsereignisse wie Gate-Wechsel, technische Kontrolle, Gepäck-Nachprüfung, verspätete Passagiere und Enteisung
- Flughäfen haben Nachfrage und Level 1-5
- stark frequentierte Flughäfen leveln automatisch auf
- Airport-Level bauen zusätzliche Gates, Parkflächen, Terminal-/Vorfeld-Erweiterungen und auf Level 5 eine zweite kurze Startbahn
- Airline-Wirtschaft mit Flugzeugkauf, Ticketumsatz, Treibstoff, Wartung, Landegebühren und Reputation
- erweitertes Airline-Dashboard
- `/airline ranking` für die serverweite Airline-Rangliste
- `/airport info` zeigt Airport-Level, Passagieraufkommen und Nachfrage
- bestehende Dorf-Bahnhöfe, Powered-Rail-Strecken und Minecart-Service bleiben enthalten
- bestehende dynamische Gates, NPC-Passagiere, Ticketnachfrage, Routen und persistente Flughäfen bleiben enthalten

## Airport-Level
Standardmäßig:
- Level 1: 0 Passagiere
- Level 2: 250 Passagiere
- Level 3: 750 Passagiere
- Level 4: 2.000 Passagiere
- Level 5: 5.000 Passagiere

Die Zählung erfolgt über abgefertigte Passagiere. Das Level und die Nachfrage werden in `airport-levels.yml` gespeichert.

## Wichtige Befehle
- `/flight` – verfügbare Passagierflüge
- `/flight money` – persönliches Passagierkonto
- `/airline` – Airline-Dashboard
- `/airline create <Name> [Code]`
- `/airline buy <Typ>`
- `/airline route <von> <nach> <Flugzeug-ID> <Preis>`
- `/airline fleet`
- `/airline routes`
- `/airline info`
- `/airline ranking`
- `/airport list`
- `/airport locate [ID/Name]`
- `/airport info`
- `/airport stations`
- `/airport generate` (Admin)

## Flugzeuge
- Airbus A220-300
- Airbus A320
- Airbus A330-300
- Airbus A350-900
- Embraer ERJ195

Keine Boeing-Flugzeuge.

## Persistenz
Airlines, Flotten, Routen, Geld und Reputation werden gespeichert. Flughäfen und deren Level werden gespeichert. Die tatsächlichen Airport-Blöcke bleiben im Welt-Save erhalten. Dorf-Bahnstationen und deren Strecken werden ebenfalls gespeichert.
