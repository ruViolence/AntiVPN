package ru.violence.antivpn.common.model.exception;

import ru.violence.antivpn.common.util.FastException;

public class CheckErrorException extends FastException {
    public static final CheckErrorException INSTANCE = new CheckErrorException();

    private CheckErrorException() {
        // NOOP
    }
}
