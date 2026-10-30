package com.mosbach.tipprunde.matches.sync;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

// Startet einen Abgleich und beendet danach den Prozess, z. B. als Job im Heroku Scheduler:
// --app.sync.mode=matchday (alle 10 Minuten) oder --app.sync.mode=season (einmal täglich)
@Component
@ConditionalOnProperty(name = "app.sync.mode")
public class SyncRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SyncRunner.class);

    private final MatchSyncService syncService;
    private final ConfigurableApplicationContext context;
    private final String mode;
    private final boolean exitAfterRun;

    public SyncRunner(MatchSyncService syncService, ConfigurableApplicationContext context,
            Environment environment) {
        this.syncService = syncService;
        this.context = context;
        this.mode = environment.getProperty("app.sync.mode", "");
        this.exitAfterRun = environment.getProperty("app.sync.exit", Boolean.class, true);
    }

    @Override
    public void run(ApplicationArguments args) {
        int exitCode = 0;
        try {
            int count = switch (mode) {
                case "matchday" -> syncService.syncCurrentMatchdays();
                case "season" -> syncService.syncSeason();
                default -> throw new IllegalArgumentException("Unknown app.sync.mode: " + mode);
            };
            log.info("Sync run '{}' finished: {} matches", mode, count);
        } catch (RuntimeException e) {
            log.error("Sync run '{}' failed: {}", mode, e.getMessage());
            exitCode = 1;
        }
        if (exitAfterRun) {
            int code = exitCode;
            System.exit(SpringApplication.exit(context, () -> code));
        }
    }
}
