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
import java.util.concurrent.TimeUnit;

public class AntiVPNPlugin extends Plugin {
    private @Getter Configuration config;
    private @Getter IPChecker ipChecker;

    @Override
    public void onEnable() {
        extractDefaultConfig();
        reloadConfig();
        long memCacheLifetime = getConfig().getLong("cache.memory");
        long dbCacheLifetime = getConfig().getLong("cache.database");
        ipChecker = new IPChecker(getDataFolder(), memCacheLifetime, dbCacheLifetime, getConfig().getLong("cooldown"));
        ProxyServer.getInstance().getScheduler().schedule(this, new CacheCleanerRunnable(ipChecker, dbCacheLifetime, getLogger()), 0, 1, TimeUnit.HOURS);
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
    private void reloadConfig() {
        getDataFolder().mkdirs();
        config = YamlConfiguration.getProvider(YamlConfiguration.class).load(new File(getDataFolder(), "config.yml"));
    }
}
