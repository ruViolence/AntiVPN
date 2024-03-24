package ru.violence.antivpn.common;

import org.jetbrains.annotations.NotNull;
import ru.violence.antivpn.common.database.SQLite;
import ru.violence.antivpn.common.model.IPAPI;
import ru.violence.antivpn.common.model.IPChecker;
import ru.violence.antivpn.common.model.ProxyList;

import java.io.File;
import java.util.logging.Logger;

public interface AntiVPN {
    @NotNull File getDataFolder();

    @NotNull Logger getLogger();

    @NotNull SQLite getDatabase();

    @NotNull ProxyList getProxyList();

    @NotNull IPAPI getIpApi();

    @NotNull IPChecker getIpChecker();
}
