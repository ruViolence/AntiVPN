package ru.violence.antivpn.bungee;

import lombok.Getter;
import lombok.SneakyThrows;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.YamlConfiguration;
import ru.violence.antivpn.bungee.command.CommandExecutor;
import ru.violence.antivpn.bungee.listener.PreLoginListener;
import ru.violence.antivpn.common.AntiVPN;
import ru.violence.antivpn.common.config.Config;
import ru.violence.antivpn.common.database.SQLite;
import ru.violence.antivpn.common.model.IPAPI;
import ru.violence.antivpn.common.model.IPChecker;
import ru.violence.antivpn.common.model.ProxyList;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.regex.Pattern;

public class AntiVPNPlugin extends Plugin implements AntiVPN {
    private @Getter SQLite database;
    private @Getter ProxyList proxyList;
    private @Getter IPAPI ipApi;
    private @Getter IPChecker ipChecker;

    @Override
    public void onEnable() {
        extractDefaultConfig();
        reloadConfig();

        database = new SQLite(this);
        proxyList = new ProxyList(this);
        ipApi = new IPAPI(this);
        ipChecker = new IPChecker(this);

        getProxy().getPluginManager().registerCommand(this, new CommandExecutor(this));
        getProxy().getPluginManager().registerListener(this, new PreLoginListener(this));
    }

    @Override
    public void onDisable() {
        if (database != null) database.close();
        if (proxyList != null) proxyList.close();
        if (ipApi != null) ipApi.close();
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

        Config.KICK_REASON = config.getString("kick-reason");

        Config.ProxyList.ENABLED = config.getBoolean("proxy-list.enabled");

        Config.ProxyList.CACHE_DATABASE = config.getLong("proxy-list.cache.database");
        Config.ProxyList.CACHE_MEMORY = config.getLong("proxy-list.cache.memory");

        Config.ProxyList.UPDATE_DELAY = config.getLong("proxy-list.update-delay");

        Config.ProxyList.PATTERN = Pattern.compile(config.getString("proxy-list.pattern"));

        Config.ProxyList.URLS = config.getStringList("proxy-list.urls");

        Config.IpApi.ENABLED = config.getBoolean("ipapi.enabled");

        Config.IpApi.CACHE_DATABASE = config.getLong("ipapi.cache.database");
        Config.IpApi.CACHE_MEMORY = config.getLong("ipapi.cache.memory");

        Config.IpApi.DENY_HOSTING = config.getBoolean("ipapi.deny.hosting");
        Config.IpApi.DENY_PROXY = config.getBoolean("ipapi.deny.proxy");

        Config.IpApi.RESULT_AWAIT = config.getLong("ipapi.result-await");

        Config.IpApi.FORCE_CHECK_ENABLED = config.getBoolean("ipapi.force-check.enabled");
        Config.IpApi.FORCE_CHECK_KICK_REASON = config.getString("ipapi.force-check.kick-reason");

        Config.IpApi.COOLDOWN = config.getLong("ipapi.cooldown");

        Config.IpApi.COUNTRY_BLOCKER_ENABLED = config.getBoolean("ipapi.country-blocker.enabled");
        Config.IpApi.COUNTRY_BLOCKER_KICK_REASON = config.getString("ipapi.country-blocker.kick-reason");
        Config.IpApi.COUNTRY_BLOCKER_WHITELIST = config.getBoolean("ipapi.country-blocker.whitelist");
        Config.IpApi.COUNTRY_BLOCKER_COUNTRIES = new HashSet<>(config.getStringList("ipapi.country-blocker.countries"));

        if (proxyList != null) proxyList.reload();
        if (ipApi != null) ipApi.reload();
        if (ipChecker != null) ipChecker.reload();
    }
}
