package ru.violence.antivpn.common.model.checker;

import org.jetbrains.annotations.NotNull;
import ru.violence.antivpn.common.AntiVPN;
import ru.violence.antivpn.common.model.FieldType;
import ru.violence.antivpn.common.model.IPChecker;
import ru.violence.antivpn.common.model.exception.BypassedException;

public class PlayerNameBypassChecker implements IPCheck {
    private final @NotNull AntiVPN antiVPN;

    public PlayerNameBypassChecker(@NotNull AntiVPN antiVPN) {
        this.antiVPN = antiVPN;
    }

    @Override
    public void checkPlayer(@NotNull IPChecker ipChecker, @NotNull String playerName, @NotNull String playerIp) throws BypassedException {
        if (antiVPN.getDatabase().isBypassed(FieldType.PLAYER_NAME, playerName)) {
            throw BypassedException.INSTANCE;
        }
    }

    @Override
    public void close() {
        // NOOP
    }
}
