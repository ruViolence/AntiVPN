package ru.violence.antivpn.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import io.leangen.geantyref.TypeToken;
import lombok.Getter;
import lombok.SneakyThrows;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;
import ru.violence.antivpn.common.checker.CacheCleanerRunnable;
import ru.violence.antivpn.common.checker.IPChecker;
import ru.violence.antivpn.velocity.command.CommandExecutor;
import ru.violence.antivpn.velocity.listener.PreLoginListener;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

@Plugin(id = "antivpn", name = "AntiVPN", version = BuildConstants.VERSION)
public class AntiVPNPlugin {
    private final @Getter ProxyServer proxy;
    private final @Getter Path dataFolder;
    private final @Getter Logger logger;

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
        ipChecker = new IPChecker(dataFolder.toFile(), cacheMemoryLifetime, cacheDbLifetime, cooldown);
        proxy.getScheduler().buildTask(this, new CacheCleanerRunnable(ipChecker, getLogger(), this::getCacheDbLifetime)).repeat(1, TimeUnit.HOURS).schedule();
        proxy.getCommandManager().register("antivpn", new CommandExecutor(this));
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
    public void reloadConfig() {
        Files.createDirectories(dataFolder);
        CommentedConfigurationNode config = YamlConfigurationLoader.builder().path(dataFolder.resolve("config.yml")).build().load();

        kickReason = config.node("kick-reason").getString();
        cacheMemoryLifetime = config.node("cache", "memory").getLong();
        cacheDbLifetime = config.node("cache", "database").getLong();
        denyHosting = config.node("deny-hosting").getBoolean();
        resultAwait = config.node("result-await").getLong();
        forceCheckEnabled = config.node("force-check", "enabled").getBoolean();
        forceCheckKickReason = config.node("force-check", "kick-reason").getString();
        cooldown = config.node("cooldown").getLong();
        countryBlockerEnabled = config.node("country-blocker", "enabled").getBoolean();
        countryBlockerKickReason = config.node("country-blocker", "kick-reason").getString();
        countryBlockerWhitelist = config.node("country-blocker", "whitelist").getBoolean();
        countryBlockerCountries = config.node("country-blocker", "countries").getList(TypeToken.get(String.class));

        if (ipChecker != null) ipChecker.reload(cacheMemoryLifetime, cacheDbLifetime, cooldown);
    }
}
