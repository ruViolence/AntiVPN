package ru.violence.antivpn.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import lombok.Getter;
import lombok.SneakyThrows;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.yaml.YAMLConfigurationLoader;
import ru.violence.antivpn.common.checker.CacheCleanerRunnable;
import ru.violence.antivpn.common.checker.IPChecker;
import ru.violence.antivpn.velocity.command.CommandExecutor;
import ru.violence.antivpn.velocity.listener.PreLoginListener;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

@Plugin(id = "antivpn", name = "AntiVPN", version = BuildConstants.VERSION)
public class AntiVPNPlugin {
    private final @Getter ProxyServer proxy;
    private final @Getter Path dataFolder;
    private final @Getter Logger logger;

    private @Getter ConfigurationNode config;
    private @Getter IPChecker ipChecker;

    @Inject
    public AntiVPNPlugin(ProxyServer proxy, @DataDirectory Path dataFolder) {
        this.proxy = proxy;
        this.dataFolder = dataFolder;
        this.logger = Logger.getLogger("AntiVPN");
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        extractDefaultConfig();
        reloadConfig();
        long memCacheLifetime = getConfig().getNode("cache", "memory").getLong();
        long dbCacheLifetime = getConfig().getNode("cache", "database").getLong();
        ipChecker = new IPChecker(dataFolder.toFile(), memCacheLifetime, dbCacheLifetime, getConfig().getNode("cooldown").getLong());
        proxy.getScheduler().buildTask(this, new CacheCleanerRunnable(ipChecker, dbCacheLifetime, getLogger())).repeat(1, TimeUnit.HOURS).schedule();
        proxy.getCommandManager().register(proxy.getCommandManager().metaBuilder("antivpn").build(), new CommandExecutor(this));
        proxy.getEventManager().register(this, new PreLoginListener(this));
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        ipChecker.close();
    }

    @SuppressWarnings("ConstantConditions")
    @SneakyThrows(IOException.class)
    private void extractDefaultConfig() {
        Files.createDirectories(dataFolder);
        Path configPath = dataFolder.resolve("config.yml");
        if (Files.notExists(configPath)) {
            try (InputStream cfgStream = getClass().getClassLoader().getResourceAsStream("config.yml")) {
                Files.copy(cfgStream, configPath);
            }
        }
    }

    @SneakyThrows(IOException.class)
    private void reloadConfig() {
        Files.createDirectories(dataFolder);
        config = YAMLConfigurationLoader.builder().setPath(dataFolder.resolve("config.yml")).build().load();
    }
}
