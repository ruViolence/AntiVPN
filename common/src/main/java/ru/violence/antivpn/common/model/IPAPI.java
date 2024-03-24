package ru.violence.antivpn.common.model;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Sets;
import lombok.Data;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.violence.antivpn.common.AntiVPN;
import ru.violence.antivpn.common.config.Config;
import ru.violence.antivpn.common.util.FastException;
import ru.violence.antivpn.common.util.Utils;

import java.io.IOException;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class IPAPI implements AutoCloseable {
    private static final String API_URL_PREFIX = "http://ip-api.com/json/"; // HTTPS is not supported on free plan
    private static final String API_URL_SUFFIX = "?fields=21122562";

    private final @NotNull AntiVPN antiVPN;
    private final Set<QueueEntry> queue = Sets.newConcurrentHashSet();
    private final QueueThread queueThread;
    private long cooldownTime = 0;
    private boolean isClosed = false;

    // Configurable
    private Cache<String, CheckResult> cache;

    public IPAPI(@NotNull AntiVPN antiVPN) {
        this.antiVPN = antiVPN;
        this.queueThread = new QueueThread();
        this.queueThread.start();
        reload();
    }

    @Override
    public void close() {
        isClosed = true;
        queueThread.interrupt();
    }

    public void reload() {
        synchronized (this) {
            this.cache = CacheBuilder.newBuilder().expireAfterAccess(Config.IpApi.CACHE_MEMORY, TimeUnit.MINUTES).build();
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
    private @Nullable CheckResult getFromMemoryCache(@NotNull String ip) {
        return cache.getIfPresent(ip);
    }

    @SneakyThrows
    private @Nullable CheckResult getFromCache(@NotNull String ip) {
        CheckResult memoryCache = getFromMemoryCache(ip);
        if (memoryCache != null) return memoryCache;

        synchronized (this) {
            CheckResult result = antiVPN.getDatabase().getIPAPIResult(ip);
            if (result == null) return null;

            cache.put(ip, result);
            return result;
        }
    }

    private boolean isCooledDown() {
        return System.currentTimeMillis() < cooldownTime;
    }

    private void setCooldown() {
        this.cooldownTime = System.currentTimeMillis() + Config.IpApi.COOLDOWN;
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

    private class QueueThread extends Thread {
        public QueueThread() {
            super("AntiVPN - IPAPI Queue Thread");
        }

        @SuppressWarnings("BusyWait")
        @Override
        public void run() {
            task:
            while (true) {
                try {
                    Thread.sleep(10);
                    if (isClosed) break;

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

                            antiVPN.getDatabase().putIPAPIResult(ip, jsonString);
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
                    antiVPN.getLogger().log(Level.SEVERE, "Failed to query IPAPI", e);
                }
            }
        }
    }
}
