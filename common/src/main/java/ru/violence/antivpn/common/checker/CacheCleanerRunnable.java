package ru.violence.antivpn.common.checker;

import lombok.SneakyThrows;

import java.sql.PreparedStatement;
import java.util.function.LongSupplier;
import java.util.logging.Logger;

public class CacheCleanerRunnable implements Runnable {
    private final IPChecker ipChecker;
    private final Logger logger;
    private final LongSupplier dbCacheLifetimeSupplier;

    public CacheCleanerRunnable(IPChecker ipChecker, Logger logger, LongSupplier dbCacheLifetimeSupplier) {
        this.ipChecker = ipChecker;
        this.logger = logger;
        this.dbCacheLifetimeSupplier = dbCacheLifetimeSupplier;
    }

    @SneakyThrows
    @Override
    public void run() {
        synchronized (ipChecker.connection) {
            long dbCacheLifetime = dbCacheLifetimeSupplier.getAsLong();
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
