package ru.violence.antivpn.common.checker;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Sets;
import lombok.Data;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.violence.antivpn.common.util.FastException;
import ru.violence.antivpn.common.util.Utils;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@SuppressWarnings({"SqlResolve", "SqlNoDataSourceInspection", "HttpUrlsUsage"})
public class IPChecker implements AutoCloseable {
    private static final String API_URL_PREFIX = "http://ip-api.com/json/";
    private static final String API_URL_SUFFIX = "?fields=21122562";
    private final Cache<String, CheckResult> cache;
    private final Set<QueueEntry> queue = Sets.newConcurrentHashSet();
    private final long cooldown;
    private final long dbCacheLifetime;
    final Connection connection;
    private long cooldownTime = 0;

    @SneakyThrows
    public IPChecker(File dataFolder, long memCacheLifetime, long dbCacheLifetime, long cooldown) {
        Class.forName("org.sqlite.JDBC");
        this.dbCacheLifetime = dbCacheLifetime;
        this.cooldown = cooldown;
        this.connection = DriverManager.getConnection("jdbc:sqlite://" + dataFolder.getAbsolutePath() + "/data.db");
        this.cache = CacheBuilder.newBuilder().expireAfterAccess(memCacheLifetime, TimeUnit.MINUTES).build();
        createTables();
        new QueueTask().start();
    }

    @SneakyThrows
    private void createTables() {
        synchronized (connection) {
            try (Statement st = connection.createStatement()) {
                st.execute("CREATE TABLE IF NOT EXISTS `check_cache`(`id` INTEGER NOT NULL UNIQUE, `ip` TEXT NOT NULL UNIQUE, `result` TEXT NOT NULL, `since` INTEGER NOT NULL, PRIMARY KEY (`id` AUTOINCREMENT));");
                st.execute("CREATE UNIQUE INDEX IF NOT EXISTS `check_cache_ip` ON `check_cache` (`ip`);");
                st.execute("CREATE INDEX IF NOT EXISTS `check_cache_since` ON `check_cache` (`since`);");
                st.execute("CREATE TABLE IF NOT EXISTS `block`(`id` INTEGER NOT NULL UNIQUE, `type` TEXT NOT NULL, `value` TEXT NOT NULL, `since` INTEGER NOT NULL, PRIMARY KEY(`id` AUTOINCREMENT));");
                st.execute("CREATE INDEX IF NOT EXISTS `block_type_value` ON `block` (`type`, `value`);");
                st.execute("CREATE TABLE IF NOT EXISTS `bypass`(`id` INTEGER NOT NULL UNIQUE, `type` TEXT NOT NULL, `value` TEXT NOT NULL, `since` INTEGER NOT NULL, PRIMARY KEY(`id` AUTOINCREMENT));");
                st.execute("CREATE INDEX IF NOT EXISTS `bypass_type_value` ON `bypass` (`type`, `value`);");
            }
        }
    }

    @SneakyThrows
    public void close() {
        synchronized (connection) {
            connection.close();
        }
    }

    public @NotNull Future<CheckResult> check(@NotNull String ip) {
        CompletableFuture<CheckResult> future = new CompletableFuture<>();

        CheckResult memoryResult = getFromMemoryCache(ip);
        if (memoryResult != null) {
            future.complete(memoryResult);
            return future;
        }

        queue.add(new QueueEntry(ip, future));

        return future;
    }

    private boolean isCooledDown() {
        return System.currentTimeMillis() < cooldownTime;
    }

    private void setCooldown() {
        this.cooldownTime = System.currentTimeMillis() + cooldown;
    }

    @SneakyThrows
    private @Nullable CheckResult getFromMemoryCache(@NotNull String ip) {
        return cache.getIfPresent(ip);
    }

    @SneakyThrows
    private @Nullable CheckResult getFromCache(@NotNull String ip) {
        CheckResult memoryCache = getFromMemoryCache(ip);
        if (memoryCache != null) return memoryCache;

        synchronized (connection) {
            try (PreparedStatement ps = connection.prepareStatement("SELECT `result` FROM `check_cache` WHERE `ip` = ? AND `since` > ?")) {
                ps.setString(1, ip);
                ps.setLong(2, System.currentTimeMillis() - dbCacheLifetime);
                ResultSet rs = ps.executeQuery();
                if (!rs.next()) return null;

                CheckResult result = new CheckResult(rs.getString(1));
                cache.put(ip, result);

                return result;
            }
        }
    }

    @SneakyThrows
    private void putToCache(@NotNull String ip, @NotNull String result) {
        synchronized (connection) {
            try (PreparedStatement ps = connection.prepareStatement("INSERT OR REPLACE INTO `check_cache`(`ip`, `result`, `since`) VALUES (?, ?, ?)")) {
                ps.setString(1, ip);
                ps.setString(2, result);
                ps.setLong(3, System.currentTimeMillis());
                ps.execute();
            }
        }
    }

    @SneakyThrows
    public boolean isBlocked(@NotNull FieldType type, @NotNull String value) {
        synchronized (connection) {
            try (PreparedStatement ps = connection.prepareStatement("SELECT `id` FROM `block` WHERE `type` = ? AND `value` = ?")) {
                ps.setString(1, type.toKey());
                ps.setString(2, value);
                ResultSet rs = ps.executeQuery();
                return rs.next();
            }
        }
    }

    @SneakyThrows
    public boolean isBypassed(@NotNull FieldType type, @NotNull String value) {
        synchronized (connection) {
            try (PreparedStatement ps = connection.prepareStatement("SELECT `id` FROM `bypass` WHERE `type` = ? AND `value` = ?")) {
                ps.setString(1, type.toKey());
                ps.setString(2, value);
                ResultSet rs = ps.executeQuery();
                return rs.next();
            }
        }
    }

    @SneakyThrows
    public boolean toggleBlock(@NotNull FieldType type, @NotNull String value) {
        synchronized (connection) {
            boolean isBlocked = isBlocked(type, value);

            if (isBlocked) {
                try (PreparedStatement ps = connection.prepareStatement("DELETE FROM `block` WHERE `type` = ? AND `value` = ?")) {
                    ps.setString(1, type.toKey());
                    ps.setString(2, value);
                    ps.execute();
                }
            } else {
                try (PreparedStatement ps = connection.prepareStatement("INSERT INTO `block`(`type`, `value`, `since`) VALUES (?, ?, ?)")) {
                    ps.setString(1, type.toKey());
                    ps.setString(2, value);
                    ps.setLong(3, System.currentTimeMillis());
                    ps.execute();
                }
            }

            return !isBlocked;
        }
    }

    @SneakyThrows
    public boolean toggleBypass(@NotNull FieldType type, @NotNull String value) {
        synchronized (connection) {
            boolean isBypassed = isBypassed(type, value);

            if (isBypassed) {
                try (PreparedStatement ps = connection.prepareStatement("DELETE FROM `bypass` WHERE `type` = ? AND `value` = ?")) {
                    ps.setString(1, type.toKey());
                    ps.setString(2, value);
                    ps.execute();
                }
            } else {
                try (PreparedStatement ps = connection.prepareStatement("INSERT INTO `bypass`(`type`, `value`, `since`) VALUES (?, ?, ?)")) {
                    ps.setString(1, type.toKey());
                    ps.setString(2, value);
                    ps.setLong(3, System.currentTimeMillis());
                    ps.execute();
                }
            }

            return !isBypassed;
        }
    }

    @SneakyThrows
    public boolean removeFromDatabaseCache(@NotNull String ip) {
        synchronized (connection) {
            try (PreparedStatement ps = connection.prepareStatement("DELETE FROM `check_cache` WHERE `ip` = ?")) {
                ps.setString(1, ip);
                boolean isDeleted = ps.executeUpdate() != 0;
                if (isDeleted) {
                    cache.invalidate(ip);
                    return true;
                }
                return false;
            }
        }
    }

    @Data
    private static class QueueEntry {
        private final String ip;
        private final CompletableFuture<CheckResult> future;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            QueueEntry that = (QueueEntry) o;

            return ip.equals(that.ip);
        }

        @Override
        public int hashCode() {
            return ip.hashCode();
        }
    }

    private class QueueTask extends Thread {
        @SuppressWarnings("BusyWait")
        @Override
        public void run() {
            task:
            while (true) {
                try {
                    Thread.sleep(10);

                    if (isCooledDown()) continue;

                    for (Iterator<QueueEntry> iterator = queue.iterator(); iterator.hasNext(); ) {
                        QueueEntry entry = iterator.next();

                        String ip = entry.getIp();
                        CompletableFuture<CheckResult> future = entry.getFuture();

                        CheckResult cachedResult = getFromCache(ip);
                        if (cachedResult != null) {
                            iterator.remove();
                            future.complete(cachedResult);
                            continue;
                        }

                        try {
                            String jsonString = Utils.readStringFromUrl(API_URL_PREFIX + ip + API_URL_SUFFIX);
                            CheckResult result = new CheckResult(jsonString);

                            if (!result.getStatus().equals("success")) {
                                iterator.remove();
                                future.completeExceptionally(new FastException("Unsuccessful IP query \"" + ip + "\": " + jsonString));
                                continue;
                            }

                            putToCache(ip, jsonString);
                            cache.put(ip, result);
                            iterator.remove();
                            future.complete(result);
                        } catch (IOException e) {
                            setCooldown();
                            continue task;
                        }
                    }
                } catch (InterruptedException e) {
                    return;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
