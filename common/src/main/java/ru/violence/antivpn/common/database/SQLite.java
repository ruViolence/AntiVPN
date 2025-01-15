package ru.violence.antivpn.common.database;

import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.violence.antivpn.common.AntiVPN;
import ru.violence.antivpn.common.config.Config;
import ru.violence.antivpn.common.model.CheckResult;
import ru.violence.antivpn.common.model.FieldType;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.logging.Level;

public class SQLite implements AutoCloseable {
    private static final long COMMIT_INTERVAL = 60_000; // 1 minute

    private final AntiVPN antiVPN;
    private final Connection connection;
    private volatile boolean running = true;

    @SneakyThrows
    public SQLite(@NotNull AntiVPN antiVPN) {
        this.antiVPN = antiVPN;

        Class.forName("org.sqlite.JDBC");
        this.connection = DriverManager.getConnection("jdbc:sqlite://" + antiVPN.getDataFolder().getAbsolutePath() + "/data.db");
        this.connection.setAutoCommit(false);
        createTables();
        connection.commit();

        new CacheCleanThread().start();
        new CommitThread().start();
    }

    @SneakyThrows
    private void createTables() {
        synchronized (connection) {
            try (Statement st = connection.createStatement()) {
                st.execute("CREATE TABLE IF NOT EXISTS `ipapi_cache`(`id` INTEGER NOT NULL UNIQUE, `ip` TEXT NOT NULL UNIQUE, `result` TEXT NOT NULL, `since` INTEGER NOT NULL, PRIMARY KEY (`id` AUTOINCREMENT));");
                st.execute("CREATE UNIQUE INDEX IF NOT EXISTS `ipapi_cache_ip` ON `ipapi_cache` (`ip`);");
                st.execute("CREATE INDEX IF NOT EXISTS `ipapi_cache_since` ON `ipapi_cache` (`since`);");

                st.execute("CREATE TABLE IF NOT EXISTS `proxylist_cache`(`id` INTEGER NOT NULL UNIQUE, `ip` TEXT NOT NULL UNIQUE, `since` INTEGER NOT NULL, PRIMARY KEY (`id` AUTOINCREMENT));");
                st.execute("CREATE UNIQUE INDEX IF NOT EXISTS `proxylist_cache_ip` ON `proxylist_cache` (`ip`);");
                st.execute("CREATE INDEX IF NOT EXISTS `proxylist_cache_since` ON `proxylist_cache` (`since`);");

                st.execute("CREATE TABLE IF NOT EXISTS `block`(`id` INTEGER NOT NULL UNIQUE, `type` TEXT NOT NULL, `value` TEXT NOT NULL, `since` INTEGER NOT NULL, PRIMARY KEY(`id` AUTOINCREMENT));");
                st.execute("CREATE INDEX IF NOT EXISTS `block_type_value` ON `block` (`type`, `value`);");

                st.execute("CREATE TABLE IF NOT EXISTS `bypass`(`id` INTEGER NOT NULL UNIQUE, `type` TEXT NOT NULL, `value` TEXT NOT NULL, `since` INTEGER NOT NULL, PRIMARY KEY(`id` AUTOINCREMENT));");
                st.execute("CREATE INDEX IF NOT EXISTS `bypass_type_value` ON `bypass` (`type`, `value`);");
            }
        }
    }

    @SneakyThrows
    @Override
    public void close() {
        running = false;
        synchronized (connection) {
            if (!connection.isClosed()) {
                connection.commit();
                connection.close();
            }
        }
    }

    @SneakyThrows
    public @Nullable CheckResult getIPAPIResult(@NotNull String ip) {
        synchronized (connection) {
            try (PreparedStatement ps = connection.prepareStatement("SELECT `result` FROM `ipapi_cache` WHERE `ip` = ? AND `since` > ?")) {
                ps.setString(1, ip);
                ps.setLong(2, System.currentTimeMillis() - Config.IpApi.CACHE_DATABASE);
                ResultSet rs = ps.executeQuery();
                if (!rs.next()) return null;
                return new CheckResult(rs.getString(1));
            }
        }
    }

    @SneakyThrows
    public void putIPAPIResult(@NotNull String ip, @NotNull String result) {
        synchronized (connection) {
            try (PreparedStatement ps = connection.prepareStatement("INSERT OR REPLACE INTO `ipapi_cache`(`ip`, `result`, `since`) VALUES (?, ?, ?)")) {
                ps.setString(1, ip);
                ps.setString(2, result);
                ps.setLong(3, System.currentTimeMillis());
                ps.execute();
            }
        }
    }

    @SneakyThrows
    public boolean isProxyListIp(@NotNull String ip) {
        synchronized (connection) {
            try (PreparedStatement ps = connection.prepareStatement("SELECT `ip` FROM `proxylist_cache` WHERE `ip` = ? AND `since` > ?")) {
                ps.setString(1, ip);
                ps.setLong(2, System.currentTimeMillis() - Config.ProxyList.CACHE_DATABASE);
                ResultSet rs = ps.executeQuery();
                return rs.next();
            }
        }
    }

    @SneakyThrows
    public void putProxyListIp(@NotNull String ip) {
        synchronized (connection) {
            try (PreparedStatement ps = connection.prepareStatement("INSERT OR REPLACE INTO `proxylist_cache`(`ip`, `since`) VALUES (?, ?)")) {
                ps.setString(1, ip);
                ps.setLong(2, System.currentTimeMillis());
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
            try (PreparedStatement ps = connection.prepareStatement("DELETE FROM `ipapi_cache` WHERE `ip` = ?")) {
                ps.setString(1, ip);
                return ps.executeUpdate() != 0;
            }
        }
    }

    private class CommitThread extends Thread {
        public CommitThread() {
            super("AntiVPN-CommitThread");
            setDaemon(true);
        }

        @Override
        @SneakyThrows
        public void run() {
            while (running) {
                try {
                    Thread.sleep(COMMIT_INTERVAL);
                    synchronized (connection) {
                        if (!connection.isClosed()) {
                            connection.commit();
                        }
                    }
                } catch (Exception e) {
                    antiVPN.getLogger().log(Level.SEVERE, "Error in commit thread", e);
                }
            }
        }
    }

    private class CacheCleanThread extends Thread {
        public CacheCleanThread() {
            super("AntiVPN - Database Cache Clean Thread");
            setDaemon(true);
        }

        @SuppressWarnings("BusyWait")
        @Override
        public void run() {
            while (true) {
                try {
                    synchronized (connection) {
                        if (connection.isClosed()) break;

                        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM `proxylist_cache` WHERE `since` <= ?")) {
                            ps.setLong(1, System.currentTimeMillis() - Config.ProxyList.CACHE_DATABASE);
                            int deleted = ps.executeUpdate();
                            if (deleted != 0) {
                                antiVPN.getLogger().info("Deleted " + deleted + " expired IPAPI cache entries");
                            }
                        }

                        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM `ipapi_cache` WHERE `since` <= ?")) {
                            ps.setLong(1, System.currentTimeMillis() - Config.IpApi.CACHE_DATABASE);
                            int deleted = ps.executeUpdate();
                            if (deleted != 0) {
                                antiVPN.getLogger().info("Deleted " + deleted + " expired IPAPI cache entries");
                            }
                        }
                    }

                    Thread.sleep(60 * 60 * 1000); // 1 hour
                } catch (InterruptedException e) {
                    return;
                } catch (Exception e) {
                    antiVPN.getLogger().log(Level.SEVERE, "Failed to clean database cache", e);
                }
            }
        }
    }
}
