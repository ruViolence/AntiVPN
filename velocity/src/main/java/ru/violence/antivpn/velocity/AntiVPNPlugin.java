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
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;
import ru.violence.antivpn.common.AntiVPN;
import ru.violence.antivpn.common.config.Config;
import ru.violence.antivpn.common.database.SQLite;
import ru.violence.antivpn.common.model.IPAPI;
import ru.violence.antivpn.common.model.IPChecker;
import ru.violence.antivpn.common.model.ProxyList;
import ru.violence.antivpn.velocity.command.CommandExecutor;
import ru.violence.antivpn.velocity.listener.PreLoginListener;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.logging.Logger;
import java.util.regex.Pattern;

@Plugin(id = "antivpn", name = "AntiVPN", version = BuildConstants.VERSION)
public class AntiVPNPlugin implements AntiVPN {
    private final @Getter ProxyServer proxy;
    private final Path dataFolder;
    private final @Getter Logger logger;

    private @Getter SQLite database;
    private @Getter ProxyList proxyList;
    private @Getter IPAPI ipApi;
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

        database = new SQLite(this);
        proxyList = new ProxyList(this);
        ipApi = new IPAPI(this);
        ipChecker = new IPChecker(this);

        proxy.getCommandManager().register("antivpn", new CommandExecutor(this));
        proxy.getEventManager().register(this, new PreLoginListener(this));
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        if (database != null) database.close();
        if (proxyList != null) proxyList.close();
        if (ipApi != null) ipApi.close();
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

        Config.KICK_REASON = config.node("kick-reason").getString();

        Config.ProxyList.ENABLED = config.node("proxy-list", "enabled").getBoolean();

        Config.ProxyList.CACHE_DATABASE = config.node("proxy-list", "cache", "database").getLong();
        Config.ProxyList.CACHE_MEMORY = config.node("proxy-list", "cache", "memory").getLong();

        Config.ProxyList.UPDATE_DELAY = config.node("proxy-list", "update-delay").getLong();

        Config.ProxyList.PATTERN = Pattern.compile(config.node("proxy-list", "pattern").getString());

        Config.ProxyList.URLS = config.node("proxy-list", "urls").getList(TypeToken.get(String.class));

        Config.IpApi.ENABLED = config.node("ipapi", "enabled").getBoolean();

        Config.IpApi.CACHE_DATABASE = config.node("ipapi", "cache", "database").getLong();
        Config.IpApi.CACHE_MEMORY = config.node("ipapi", "cache", "memory").getLong();

        Config.IpApi.DENY_HOSTING = config.node("ipapi", "deny", "hosting").getBoolean();
        Config.IpApi.DENY_PROXY = config.node("ipapi", "deny", "proxy").getBoolean();

        Config.IpApi.BYPASS_COUNTRIES_HOSTING = new HashSet<>(config.node("ipapi", "bypass-countries", "hosting").getList(TypeToken.get(String.class)));
        Config.IpApi.BYPASS_COUNTRIES_PROXY = new HashSet<>(config.node("ipapi", "bypass-countries", "proxy").getList(TypeToken.get(String.class)));

        Config.IpApi.RESULT_AWAIT = config.node("ipapi", "result-await").getLong();

        Config.IpApi.FORCE_CHECK_ENABLED = config.node("ipapi", "force-check", "enabled").getBoolean();
        Config.IpApi.FORCE_CHECK_KICK_REASON = config.node("ipapi", "force-check", "kick-reason").getString();

        Config.IpApi.COOLDOWN = config.node("ipapi", "cooldown").getLong();

        Config.IpApi.COUNTRY_BLOCKER_ENABLED = config.node("ipapi", "country-blocker", "enabled").getBoolean();
        Config.IpApi.COUNTRY_BLOCKER_KICK_REASON = config.node("ipapi", "country-blocker", "kick-reason").getString();
        Config.IpApi.COUNTRY_BLOCKER_WHITELIST = config.node("ipapi", "country-blocker", "whitelist").getBoolean();
        Config.IpApi.COUNTRY_BLOCKER_COUNTRIES = new HashSet<>(config.node("ipapi", "country-blocker", "countries").getList(TypeToken.get(String.class)));

        if (proxyList != null) proxyList.reload();
        if (ipApi != null) ipApi.reload();
        if (ipChecker != null) ipChecker.reload();
    }

    @Override
    public @NotNull File getDataFolder() {
        return dataFolder.toFile();
    }
}
