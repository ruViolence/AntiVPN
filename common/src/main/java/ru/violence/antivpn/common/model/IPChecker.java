package ru.violence.antivpn.common.model;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import ru.violence.antivpn.common.AntiVPN;
import ru.violence.antivpn.common.config.Config;
import ru.violence.antivpn.common.model.checker.IPAPIChecker;
import ru.violence.antivpn.common.model.checker.IPCheck;
import ru.violence.antivpn.common.model.checker.PlayerNameBypassChecker;
import ru.violence.antivpn.common.model.checker.ProxyListChecker;
import ru.violence.antivpn.common.model.exception.BypassedException;
import ru.violence.antivpn.common.model.exception.CheckErrorException;
import ru.violence.antivpn.common.model.exception.CountryBlockedException;
import ru.violence.antivpn.common.model.exception.HostingBlockedException;
import ru.violence.antivpn.common.model.exception.ManuallyBlockedException;
import ru.violence.antivpn.common.model.exception.ProxyBlockedException;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class IPChecker {
    private final @NotNull AntiVPN antiVPN;
    private final @NotNull List<IPCheck> ipCheckers = new ArrayList<>();

    public IPChecker(@NotNull AntiVPN antiVPN) {
        this.antiVPN = antiVPN;
        reload();
    }

    public void reload() {
        synchronized (this) {
            ipCheckers.removeIf(ipCheck -> {
                try {
                    ipCheck.close();
                } catch (Exception e) {
                    antiVPN.getLogger().log(Level.SEVERE, "Failed to close IP checker", e);
                }
                return true;
            });

            addChecker(new PlayerNameBypassChecker(antiVPN));

            if (Config.ProxyList.ENABLED) {
                addChecker(new ProxyListChecker(antiVPN));
            }

            if (Config.IpApi.ENABLED) {
                addChecker(new IPAPIChecker(antiVPN));
            }
        }
    }

    public void checkPlayer(@NotNull String playerName, @NotNull String playerIp) throws CheckErrorException, BypassedException, CountryBlockedException, ProxyBlockedException, HostingBlockedException, ManuallyBlockedException {
        synchronized (this) {
            for (IPCheck ipChecker : ipCheckers) {
                ipChecker.checkPlayer(this, playerName, playerIp);
            }
        }
    }

    @Contract(value = "-> new", pure = true)
    @NotNull List<IPCheck> getCheckers() {
        synchronized (this) {
            return new ArrayList<>(ipCheckers);
        }
    }

    void addChecker(@NotNull IPCheck checker) {
        synchronized (this) {
            if (ipCheckers.contains(checker)) return; // Prevent duplicates
            ipCheckers.add(checker);
        }
    }

    void removeChecker(@NotNull IPCheck checker) {
        synchronized (this) {
            ipCheckers.remove(checker);
        }
    }
}
