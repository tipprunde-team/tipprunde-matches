# tipprunde-matches

[![CI](https://github.com/tipprunde-team/tipprunde-matches/actions/workflows/ci.yml/badge.svg)](https://github.com/tipprunde-team/tipprunde-matches/actions/workflows/ci.yml)

Match-Service der Tipprunde (Spring Boot 4.1, Gradle Groovy, Java 17). Der Dienst ist die einzige Komponente, die football-data.org aufruft. Er holt Spielplan und Ergebnisse der 1. Bundesliga und meldet jede Änderung als Ereignis `MATCH_UPDATED` über die Queue an den Core-Service (`tipprunde-rest`). Eine eigene Datenbank hat er nicht.

## Stand

- Health-Check unter `/actuator/health`, damit der Dienst schon leer online läuft
- Abgleich mit football-data.org (TE-02, US-18): aktueller und vorheriger Spieltag sowie die ganze Saison, Statusabbildung auf `UPCOMING`, `LIVE`, `FINISHED`, `POSTPONED`, `CANCELLED`, Ergebnis nach 90 Minuten (FA-40)
- Ratenlimit (mindestens 6 s Abstand, also höchstens 10 Anfragen pro Minute) und Wiederholung bei 429, Timeout und 5xx
- Queue (F-AH-2): Mit `QUEUE_URL` geht jedes Event als JSON in die Queue `tipprunde.match-events` (RabbitMQ bei CloudAMQP). Der Broker bestätigt jede Nachricht (Publisher Confirms), die Nachrichten sind persistent und warten in der Queue, solange der Core schläft. Ohne `QUEUE_URL` landen die Events nur im Log.

## Aufbau

| Paket | Inhalt |
|---|---|
| `client` | `FootballDataClient` und `HttpFootballDataClient` (drei Endpunkte), `RetryingRequestExecutor` für Ratenlimit und Wiederholung, Antwortformate in `dto` |
| `domain` | `MatchMapper` (Statusabbildung, Ergebnis nach 90 Minuten), Event `MatchUpdated` nach dem Vertrag aus dem Projektplan |
| `sync` | `MatchSyncService` (Abgleich), `SyncRunner` (Start als Job), `MatchEventPublisher` mit `AmqpMatchEventPublisher` (Queue) und `LoggingMatchEventPublisher` (ohne Queue) |
| `config` | Einstellungen für football-data.org und Queue (`FootballDataProperties`, `QueueProperties`), `QueueConfig` legt die Queue dauerhaft an |

## Starten und testen (Windows)

Voraussetzung: JDK 17 oder neuer (`java -version`). Gradle lädt der Wrapper selbst.

```
cd "<Pfad zum Repo>\tipprunde-matches"
gradlew.bat test
gradlew.bat bootRun
```

In PowerShell stattdessen `.\gradlew.bat test` und `.\gradlew.bat bootRun`.

Danach im Browser `http://localhost:8082/actuator/health` öffnen, erwartet wird `{"status":"UP"}`. Port 8082, weil der Core-Service lokal auf 8080 läuft.

## Abgleich lokal ausführen

Voraussetzung ist die Umgebungsvariable `FOOTBALL_DATA_TOKEN` (siehe unten). Dann in PowerShell:

```
.\gradlew.bat bootRun --args="--app.sync.mode=matchday"
```

Der Dienst holt den aktuellen und den vorherigen Spieltag, gibt jedes Spiel als Event weiter und beendet sich danach. Ohne `QUEUE_URL` steht für jedes Spiel eine Zeile `MATCH_UPDATED …` im Log. Ist `QUEUE_URL` gesetzt, gehen die Events in die echte Queue bei CloudAMQP und der Core übernimmt sie in die Datenbank; im Log stehen dann `Created new connection: rabbitConnectionFactory…` und `Sync matchday 7: 9 matches published, 9 finished`. Mit `--app.sync.mode=season` wird die ganze Saison abgeglichen. Die Aufrufe einzeln testen geht mit `docs/rest-client/football-data.http`.

## Umgebungsvariablen

Zugangsdaten stehen nie im Code oder im Repo, nur lokal als Umgebungsvariable und auf Heroku als Config Var. Werte nur per Privatnachricht teilen.

| Variable | Bedeutung | ab |
|---|---|---|
| `PORT` | Port des Dienstes, Heroku setzt ihn selbst, lokal Standard 8082 | jetzt |
| `FOOTBALL_DATA_TOKEN` | API-Schlüssel von football-data.org | Abgleich |
| `QUEUE_URL` | AMQP-URL der CloudAMQP-Instanz `tipprunde-queue` (`amqps://…`), dieselbe wie beim Core | Queue |

## Deployment auf Heroku

Wie beim Core-Service: Heroku-App mit dem GitHub-Repo verbinden, Automatic Deploys von `main` mit „Wait for CI to pass before deploy“. Heroku baut mit `system.properties` (Java 17) und startet laut `Procfile` das Jar `build/libs/tipprunde-matches.jar`. Prüfen mit `docs/rest-client/health.http`.

Der Abgleich läuft auf Heroku nicht als Dauerprozess, sondern als Job im Heroku Scheduler, weil Eco-Dynos nach 30 Minuten einschlafen:

| Häufigkeit | Befehl |
|---|---|
| alle 10 Minuten | `java -Dspring.main.web-application-type=none -jar build/libs/tipprunde-matches.jar --app.sync.mode=matchday` |
| täglich | `java -Dspring.main.web-application-type=none -jar build/libs/tipprunde-matches.jar --app.sync.mode=season` |

Config Vars der App (Settings › Reveal Config Vars): `FOOTBALL_DATA_TOKEN` und `QUEUE_URL`. Fehlt `QUEUE_URL`, schreibt der Job die Events nur ins Log. Ist die Queue nicht erreichbar, endet der Job mit `Sync run 'matchday' failed` und Exit-Code 1; der nächste Lauf zehn Minuten später schickt alle Spiele erneut, der Core verarbeitet sie idempotent.

## Queue einrichten (einmalig, erledigt am 29.10.)

1. Auf https://www.cloudamqp.com kostenlos registrieren (ohne Kreditkarte) und eine Instanz anlegen: Name `tipprunde-queue`, Plan Little Lemur, Region Amazon Web Services EU-West-1 (Ireland), passend zu Heroku Europe.
2. In der Instanz unter AMQP details die URL über das Kopier-Symbol kopieren. Sie enthält das Passwort und wird nur privat weitergegeben.
3. Lokal als Umgebungsvariable setzen (PowerShell, die URL wird dabei nicht angezeigt), danach neues Terminal öffnen:

```powershell
$s = Read-Host "AMQP-URL einfuegen" -AsSecureString
[Environment]::SetEnvironmentVariable("QUEUE_URL", [System.Net.NetworkCredential]::new("", $s).Password, "User")
Remove-Variable s
```

4. Auf Heroku in `tipprunde-matches` und `tipprunde-rest` als Config Var `QUEUE_URL` eintragen.

Die Queue `tipprunde.match-events` legen Match-Service und Core beim ersten Verbinden selbst an (durable, gleiche Einstellungen auf beiden Seiten). Nachrichten und Verbraucher sind im CloudAMQP-Dashboard unter **RabbitMQ Manager › Queues and Streams** zu sehen. Ist die URL einmal öffentlich geworden: **Rotate password** und die neue URL lokal und auf Heroku neu eintragen.
