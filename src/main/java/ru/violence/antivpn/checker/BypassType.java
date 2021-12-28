package ru.violence.antivpn.checker;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public enum BypassType {
    PLAYER_NAME,
    ISP,
    ORG,
    AS,
    ASNAME;

    public static @Nullable BypassType fromString(@NotNull String typeS) {
        for (BypassType type : values()) {
            if (type.toKey().equals(typeS)) {
                return type;
            }
        }
        return null;
    }

    public @NotNull String toKey() {
        return name().toLowerCase(Locale.ROOT);
    }
}
