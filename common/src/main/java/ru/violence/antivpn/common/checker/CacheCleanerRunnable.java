package ru.violence.antivpn.common.checker;

import lombok.SneakyThrows;

import java.sql.PreparedStatement;
import java.util.logging.Logger;

public class CacheCleanerRunnable implements Runnable {
    private final IPChecker ipChecker;
    private final long dbCacheLifetime;
    private final Logger logger;

    public CacheCleanerRunnable(IPChecker ipChecker, long dbCacheLifetime, Logger logger) {
        this.ipChecker = ipChecker;
        this.dbCacheLifetime = dbCacheLifetime;
        this.logger = logger;
    }

    @SneakyThrows
    @Override
    public void run() {
        synchronized (ipChecker.connection) {
            try (PreparedStatement ps = ipChecker.connection.prepareStatement("DELETE FROM `check_cache` WHERE `since` <= ?")) {
                ps.setLong(1, System.currentTimeMillis() - dbCacheLifetime);
                int deleted = ps.executeUpdate();
                if (deleted != 0) {
                    logger.info("Deleted " + deleted + " expired cache entries");
                }
            }
        }
    }
}
