# tipprunde-matches

[![CI](https://github.com/tipprunde-team/tipprunde-matches/actions/workflows/ci.yml/badge.svg)](https://github.com/tipprunde-team/tipprunde-matches/actions/workflows/ci.yml)

Match-Service der Tipprunde (Spring Boot 4.1, Gradle Groovy, Java 17). Der Dienst ist die einzige Komponente, die football-data.org aufruft. Er holt Spielplan und Ergebnisse der 1. Bundesliga und meldet jede Änderung als Ereignis `MATCH_UPDATED` über die Queue an den Core-Service (`tipprunde-rest`). Eine eigene Datenbank hat er nicht.

## Stand

Grundgerüst mit Health-Check, damit der Dienst schon leer online läuft. Abgleich mit football-data.org, Queue und Scheduler folgen in Sprint 3.

## Starten und testen (Windows)

Voraussetzung: JDK 17 oder neuer (`java -version`). Gradle lädt der Wrapper selbst.

```
cd "<Pfad zum Repo>\tipprunde-matches"
gradlew.bat test
gradlew.bat bootRun
```

In PowerShell stattdessen `.\gradlew.bat test` und `.\gradlew.bat bootRun`.

Danach im Browser `http://localhost:8082/actuator/health` öffnen, erwartet wird `{"status":"UP"}`. Port 8082, weil der Core-Service lokal auf 8080 läuft.

## Umgebungsvariablen

Zugangsdaten stehen nie im Code oder im Repo, nur lokal als Umgebungsvariable und auf Heroku als Config Var. Werte nur per Privatnachricht teilen.

| Variable | Bedeutung | ab |
|---|---|---|
| `PORT` | Port des Dienstes, Heroku setzt ihn selbst, lokal Standard 8082 | jetzt |
| `FOOTBALL_DATA_TOKEN` | API-Schlüssel von football-data.org | Abgleich |
| `QUEUE_URL` | AMQP-URL von CloudAMQP | Queue |

## Deployment auf Heroku

Wie beim Core-Service: Heroku-App mit dem GitHub-Repo verbinden, Automatic Deploys von `main` mit „Wait for CI to pass before deploy“. Heroku baut mit `system.properties` (Java 17) und startet laut `Procfile` das Jar `build/libs/tipprunde-matches.jar`. Prüfen mit `docs/rest-client/health.http`.
