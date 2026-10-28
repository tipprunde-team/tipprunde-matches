# tipprunde-matches

[![CI](https://github.com/tipprunde-team/tipprunde-matches/actions/workflows/ci.yml/badge.svg)](https://github.com/tipprunde-team/tipprunde-matches/actions/workflows/ci.yml)

Match-Service der Tipprunde (Spring Boot 4.1, Gradle Groovy, Java 17). Der Dienst ist die einzige Komponente, die football-data.org aufruft. Er holt Spielplan und Ergebnisse der 1. Bundesliga und meldet jede Änderung als Ereignis `MATCH_UPDATED` über die Queue an den Core-Service (`tipprunde-rest`). Eine eigene Datenbank hat er nicht.

## Stand

- Health-Check unter `/actuator/health`, damit der Dienst schon leer online läuft
- Abgleich mit football-data.org (TE-02, US-18): aktueller und vorheriger Spieltag sowie die ganze Saison, Statusabbildung auf `UPCOMING`, `LIVE`, `FINISHED`, `POSTPONED`, `CANCELLED`, Ergebnis nach 90 Minuten (FA-40)
- Ratenlimit (mindestens 6 s Abstand, also höchstens 10 Anfragen pro Minute) und Wiederholung bei 429, Timeout und 5xx
- Die Events `MATCH_UPDATED` landen vorerst im Log; die Queue folgt als Nächstes

## Aufbau

| Paket | Inhalt |
|---|---|
| `client` | `FootballDataClient` und `HttpFootballDataClient` (drei Endpunkte), `RetryingRequestExecutor` für Ratenlimit und Wiederholung, Antwortformate in `dto` |
| `domain` | `MatchMapper` (Statusabbildung, Ergebnis nach 90 Minuten), Event `MatchUpdated` nach dem Vertrag aus dem Projektplan |
| `sync` | `MatchSyncService` (Abgleich), `SyncRunner` (Start als Job), `MatchEventPublisher` |

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

Der Dienst holt den aktuellen und den vorherigen Spieltag, schreibt für jedes Spiel eine Zeile `MATCH_UPDATED …` ins Log und beendet sich danach. Mit `--app.sync.mode=season` wird die ganze Saison abgeglichen. Die Aufrufe einzeln testen geht mit `docs/rest-client/football-data.http`.

## Umgebungsvariablen

Zugangsdaten stehen nie im Code oder im Repo, nur lokal als Umgebungsvariable und auf Heroku als Config Var. Werte nur per Privatnachricht teilen.

| Variable | Bedeutung | ab |
|---|---|---|
| `PORT` | Port des Dienstes, Heroku setzt ihn selbst, lokal Standard 8082 | jetzt |
| `FOOTBALL_DATA_TOKEN` | API-Schlüssel von football-data.org | Abgleich |
| `QUEUE_URL` | AMQP-URL von CloudAMQP | Queue |

## Deployment auf Heroku

Wie beim Core-Service: Heroku-App mit dem GitHub-Repo verbinden, Automatic Deploys von `main` mit „Wait for CI to pass before deploy“. Heroku baut mit `system.properties` (Java 17) und startet laut `Procfile` das Jar `build/libs/tipprunde-matches.jar`. Prüfen mit `docs/rest-client/health.http`.

Der Abgleich läuft auf Heroku nicht als Dauerprozess, sondern als Job im Heroku Scheduler, weil Eco-Dynos nach 30 Minuten einschlafen:

| Häufigkeit | Befehl |
|---|---|
| alle 10 Minuten | `java -Dspring.main.web-application-type=none -jar build/libs/tipprunde-matches.jar --app.sync.mode=matchday` |
| täglich | `java -Dspring.main.web-application-type=none -jar build/libs/tipprunde-matches.jar --app.sync.mode=season` |
