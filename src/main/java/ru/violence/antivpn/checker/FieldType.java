package ru.violence.antivpn.checker;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public enum FieldType {
    PLAYER_NAME,
    ISP,
    ORG,
    AS,
    ASNAME;

    public static @Nullable FieldType fromString(@NotNull String typeS) {
        for (FieldType type : values()) {
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
