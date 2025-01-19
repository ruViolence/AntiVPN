package ru.violence.antivpn.common.model.checker;

import org.jetbrains.annotations.NotNull;
import ru.violence.antivpn.common.AntiVPN;
import ru.violence.antivpn.common.config.Config;
import ru.violence.antivpn.common.database.SQLite;
import ru.violence.antivpn.common.model.CheckResult;
import ru.violence.antivpn.common.model.FieldType;
import ru.violence.antivpn.common.model.IPChecker;
import ru.violence.antivpn.common.model.exception.BypassedException;
import ru.violence.antivpn.common.model.exception.CheckErrorException;
import ru.violence.antivpn.common.model.exception.CountryBlockedException;
import ru.violence.antivpn.common.model.exception.HostingBlockedException;
import ru.violence.antivpn.common.model.exception.ManuallyBlockedException;
import ru.violence.antivpn.common.model.exception.ProxyBlockedException;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;

public class IPAPIChecker implements IPCheck {
    private final @NotNull AntiVPN antiVPN;

    public IPAPIChecker(@NotNull AntiVPN antiVPN) {
        this.antiVPN = antiVPN;
    }

    @Override
    public void checkPlayer(@NotNull IPChecker ipChecker, @NotNull String playerName, @NotNull String playerIp) throws CheckErrorException, BypassedException, CountryBlockedException, ProxyBlockedException, HostingBlockedException, ManuallyBlockedException {
        SQLite database = antiVPN.getDatabase();

        CheckResult result;

        try {
            result = antiVPN.getIpApi().check(playerIp).get(Config.IpApi.RESULT_AWAIT, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException e) {
            antiVPN.getLogger().log(Level.SEVERE, "Failed to check IP: " + playerIp, e);
            throw CheckErrorException.INSTANCE;
        } catch (TimeoutException e) {
            throw CheckErrorException.INSTANCE;
        }

        if (Config.IpApi.COUNTRY_BLOCKER_ENABLED) {
            boolean contains = Config.IpApi.COUNTRY_BLOCKER_COUNTRIES.contains(result.getCountryCode());
            boolean isBlockedCountry = Config.IpApi.COUNTRY_BLOCKER_WHITELIST != contains;

            if (isBlockedCountry) {
                throw new CountryBlockedException(result);
            }
        }

        if (database.isBypassed(FieldType.ISP, result.getIsp()) ||
            database.isBypassed(FieldType.ORG, result.getOrg()) ||
            database.isBypassed(FieldType.AS, result.getAs()) ||
            database.isBypassed(FieldType.ASNAME, result.getAsname())) {
            throw BypassedException.INSTANCE;
        }

        if (database.isBlocked(FieldType.PLAYER_NAME, playerName) ||
            database.isBlocked(FieldType.ISP, result.getIsp()) ||
            database.isBlocked(FieldType.ORG, result.getOrg()) ||
            database.isBlocked(FieldType.AS, result.getAs()) ||
            database.isBlocked(FieldType.ASNAME, result.getAsname())) {
            throw new ManuallyBlockedException(result);
        }

        if (Config.IpApi.DENY_HOSTING && result.isHosting()) {
            if (Config.IpApi.BYPASS_COUNTRIES_HOSTING.contains(result.getCountryCode())) return;
            throw new HostingBlockedException(result);
        }

        if (Config.IpApi.DENY_PROXY && result.isProxy()) {
            if (Config.IpApi.BYPASS_COUNTRIES_PROXY.contains(result.getCountryCode())) return;
            throw new ProxyBlockedException(result);
        }
    }

    @Override
    public void close() {
        // NOOP
    }
}
