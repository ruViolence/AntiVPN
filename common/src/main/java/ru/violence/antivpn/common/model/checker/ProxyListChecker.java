package ru.violence.antivpn.common.model.checker;

import org.jetbrains.annotations.NotNull;
import ru.violence.antivpn.common.AntiVPN;
import ru.violence.antivpn.common.model.IPChecker;
import ru.violence.antivpn.common.model.exception.ProxyBlockedException;

public class ProxyListChecker implements IPCheck {
    private final @NotNull AntiVPN antiVPN;

    public ProxyListChecker(@NotNull AntiVPN antiVPN) {
        this.antiVPN = antiVPN;
    }

    @Override
    public void checkPlayer(@NotNull IPChecker ipChecker, @NotNull String playerName, @NotNull String playerIp) throws ProxyBlockedException {
        if (antiVPN.getProxyList().check(playerIp)) {
            throw ProxyBlockedException.PROXY_LIST;
        }
    }

    @Override
    public void close() {
        // NOOP
    }
}
