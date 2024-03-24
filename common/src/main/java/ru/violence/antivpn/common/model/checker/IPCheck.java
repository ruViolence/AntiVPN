package ru.violence.antivpn.common.model.checker;

import org.jetbrains.annotations.NotNull;
import ru.violence.antivpn.common.model.IPChecker;
import ru.violence.antivpn.common.model.exception.BypassedException;
import ru.violence.antivpn.common.model.exception.CheckErrorException;
import ru.violence.antivpn.common.model.exception.CountryBlockedException;
import ru.violence.antivpn.common.model.exception.HostingBlockedException;
import ru.violence.antivpn.common.model.exception.ManuallyBlockedException;
import ru.violence.antivpn.common.model.exception.ProxyBlockedException;

public interface IPCheck extends AutoCloseable {
    void checkPlayer(@NotNull IPChecker ipChecker, @NotNull String playerName, @NotNull String playerIp) throws CheckErrorException, BypassedException, CountryBlockedException, ProxyBlockedException, HostingBlockedException, ManuallyBlockedException;
}
