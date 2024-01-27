package ru.violence.antivpn.bungee;

import lombok.Getter;
import lombok.SneakyThrows;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.YamlConfiguration;
import ru.violence.antivpn.bungee.command.CommandExecutor;
import ru.violence.antivpn.bungee.listener.PreLoginListener;
import ru.violence.antivpn.common.checker.CacheCleanerRunnable;
import ru.violence.antivpn.common.checker.IPChecker;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class AntiVPNPlugin extends Plugin {
    private @Getter IPChecker ipChecker;

    // Config values
    private @Getter String kickReason;
    private @Getter long cacheDbLifetime;
    private @Getter long cacheMemoryLifetime;
    private @Getter boolean denyHosting;
    private @Getter long resultAwait;
    private @Getter boolean forceCheckEnabled;
    private @Getter String forceCheckKickReason;
    private @Getter long cooldown;
    private @Getter boolean countryBlockerEnabled;
    private @Getter String countryBlockerKickReason;
    private @Getter boolean countryBlockerWhitelist;
    private @Getter List<String> countryBlockerCountries;

    @Override
    public void onEnable() {
        extractDefaultConfig();
        reloadConfig();
        ipChecker = new IPChecker(getDataFolder(), cacheMemoryLifetime, cacheDbLifetime, cooldown);
        ProxyServer.getInstance().getScheduler().schedule(this, new CacheCleanerRunnable(ipChecker, getLogger(), this::getCacheDbLifetime), 0, 1, TimeUnit.HOURS);
        getProxy().getPluginManager().registerCommand(this, new CommandExecutor(this));
        getProxy().getPluginManager().registerListener(this, new PreLoginListener(this));
    }

    @Override
    public void onDisable() {
        ipChecker.close();
    }

    @SuppressWarnings({"ResultOfMethodCallIgnored", "ConstantConditions"})
    @SneakyThrows(IOException.class)
    private void extractDefaultConfig() {
        getDataFolder().mkdirs();
        File configFile = new File(getDataFolder(), "config.yml");
        if (configFile.exists()) return;
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("config.yml")) {
            Files.copy(is, configFile.toPath());
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @SneakyThrows(IOException.class)
    public void reloadConfig() {
        getDataFolder().mkdirs();
        Configuration config = YamlConfiguration.getProvider(YamlConfiguration.class).load(new File(getDataFolder(), "config.yml"));

        kickReason = config.getString("kick-reason");
        cacheMemoryLifetime = config.getLong("cache.memory");
        cacheDbLifetime = config.getLong("cache.database");
        denyHosting = config.getBoolean("deny-hosting");
        resultAwait = config.getLong("result-await");
        forceCheckEnabled = config.getBoolean("force-check.enabled");
        forceCheckKickReason = config.getString("force-check.kick-reason");
        cooldown = config.getLong("cooldown");
        countryBlockerEnabled = config.getBoolean("country-blocker.enabled");
        countryBlockerKickReason = config.getString("country-blocker.kick-reason");
        countryBlockerWhitelist = config.getBoolean("country-blocker.whitelist");
        countryBlockerCountries = config.getStringList("country-blocker.countries");

        if (ipChecker != null) ipChecker.reload(cacheMemoryLifetime, cacheDbLifetime, cooldown);
    }
}
