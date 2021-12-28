package ru.violence.antivpn;

import lombok.Getter;
import lombok.SneakyThrows;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.YamlConfiguration;
import ru.violence.antivpn.checker.IPChecker;
import ru.violence.antivpn.command.CommandExecutor;
import ru.violence.antivpn.listener.PreLoginListener;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

public class AntiVPNPlugin extends Plugin {
    private @Getter Configuration config;
    private @Getter IPChecker ipChecker;

    @SneakyThrows
    @Override
    public void onEnable() {
        extractDefaultConfig();
        reloadConfig();
        ipChecker = new IPChecker(this);
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
    @SneakyThrows
    private void reloadConfig() {
        getDataFolder().mkdirs();
        config = YamlConfiguration.getProvider(YamlConfiguration.class).load(new File(getDataFolder(), "config.yml"));
    }
}
