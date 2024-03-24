package ru.violence.antivpn.common.model;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.violence.antivpn.common.AntiVPN;
import ru.violence.antivpn.common.config.Config;
import ru.violence.antivpn.common.util.Utils;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class ProxyList implements AutoCloseable {
    private final @NotNull AntiVPN antiVPN;
    private final UpdateThread updateThread;
    private boolean isClosed = false;

    // Configurable
    private Cache<String, Boolean> cache;

    public ProxyList(@NotNull AntiVPN antiVPN) {
        this.antiVPN = antiVPN;
        this.updateThread = new UpdateThread();
        this.updateThread.start();
        reload();
    }

    @Override
    public void close() {
        isClosed = true;
        updateThread.interrupt();
    }

    public void reload() {
        synchronized (this) {
            this.cache = CacheBuilder.newBuilder().expireAfterAccess(Config.ProxyList.CACHE_MEMORY, TimeUnit.MINUTES).build();
        }
    }

    public boolean check(@NotNull String ip) {
        return getFromCache(ip);
    }

    @SneakyThrows
    public boolean removeCachedResult(@NotNull String ip) {
        synchronized (this) {
            if (antiVPN.getDatabase().removeFromDatabaseCache(ip)) {
                cache.invalidate(ip);
                return true;
            }
            return false;
        }
    }

    @SneakyThrows
    private @Nullable Boolean getFromMemoryCache(@NotNull String ip) {
        return cache.getIfPresent(ip);
    }

    @SneakyThrows
    private boolean getFromCache(@NotNull String ip) {
        Boolean memoryCache = getFromMemoryCache(ip);
        if (memoryCache != null) return memoryCache;

        synchronized (this) {
            boolean result = antiVPN.getDatabase().isProxyListIp(ip);

            cache.put(ip, result);
            return result;
        }
    }

    private class UpdateThread extends Thread {
        public UpdateThread() {
            super("AntiVPN - ProxyList Update Thread");
        }

        @SuppressWarnings("BusyWait")
        @Override
        public void run() {
            while (true) {
                try {
                    if (isClosed) break;

                    Set<String> ips = new HashSet<>();

                    for (String url : Config.ProxyList.URLS) {
                        try {
                            String raw = Utils.readStringFromUrl(url);

                            for (String line : raw.split("\n+")) {
                                if (line.isEmpty()) continue;
                                String[] split = line.split(":");

                                String ip = split[0]; // Second part is port, we don't need it
                                if (ip.isEmpty()) continue;

                                ips.add(ip);
                            }
                        } catch (IOException e) {
                            antiVPN.getLogger().log(Level.SEVERE, "Failed to load ProxyList from " + url, e);
                        }
                    }

                    for (String ip : ips) {
                        antiVPN.getDatabase().putProxyListIp(ip);
                    }

                    antiVPN.getLogger().info("Updated ProxyList (" + ips.size() + " IPs)");

                    Thread.sleep(Config.ProxyList.UPDATE_DELAY);
                } catch (InterruptedException e) {
                    return;
                } catch (Exception e) {
                    antiVPN.getLogger().log(Level.SEVERE, "Failed to update ProxyList", e);
                }
            }
        }
    }
}
