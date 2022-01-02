package ru.violence.antivpn.checker;

import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.violence.antivpn.AntiVPNPlugin;
import ru.violence.antivpn.checker.exception.TimedOutException;
import ru.violence.antivpn.util.Utils;
import ru.violence.coreapi.common.util.StringReplacer;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

@SuppressWarnings({"SqlResolve", "SqlNoDataSourceInspection", "HttpUrlsUsage"})
public class IPChecker implements AutoCloseable {
    private static final String API_URL = "http://ip-api.com/json/{query}?fields=21188127";
    private static final long TIMEOUT = 15 * 1000; // 15 Seconds
    private final AntiVPNPlugin plugin;
    private final Connection connection;
    private long timeoutTime = 0;

    public IPChecker(AntiVPNPlugin plugin) throws Exception {
        Class.forName("org.sqlite.JDBC").newInstance();
        this.plugin = plugin;
        this.connection = DriverManager.getConnection("jdbc:sqlite://" + plugin.getDataFolder().getAbsolutePath() + "/data.db");
        createTables();
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

    public @NotNull CheckResult check(@NotNull String ip) throws Exception {
        if (isTimedOut()) throw TimedOutException.INSTANCE;

        String cached = getFromCache(ip);
        if (cached != null) return new CheckResult(cached);

        try {
            String jsonString = Utils.readStringFromUrl(StringReplacer.replace(API_URL, "{query}", ip));
            CheckResult result = new CheckResult(jsonString);

            if (!result.getStatus().equals("success")) {
                throw new Exception("Unsuccessful IP query \"" + ip + "\": " + jsonString);
            }

            putToCache(ip, jsonString);
            return result;
        } catch (IOException e) {
            setTimeout();
            throw e;
        }
    }

    private boolean isTimedOut() {
        return System.currentTimeMillis() < timeoutTime;
    }

    private void setTimeout() {
        this.timeoutTime = System.currentTimeMillis() + TIMEOUT;
    }

    @SneakyThrows
    private @Nullable String getFromCache(@NotNull String ip) {
        synchronized (connection) {
            try (PreparedStatement ps = connection.prepareStatement("SELECT `result` FROM `check_cache` WHERE `ip` = ? AND `since` > ?")) {
                ps.setString(1, ip);
                ps.setLong(2, System.currentTimeMillis() - plugin.getConfig().getLong("cache-lifetime"));
                ResultSet rs = ps.executeQuery();
                if (!rs.next()) return null;
                return rs.getString(1);
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
}
