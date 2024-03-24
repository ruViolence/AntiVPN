package ru.violence.antivpn.common.model.exception;

import ru.violence.antivpn.common.util.FastException;

public class BypassedException extends FastException {
    public static final BypassedException INSTANCE = new BypassedException();

    private BypassedException() {
        // NOOP
    }
}
