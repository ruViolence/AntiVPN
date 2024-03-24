package ru.violence.antivpn.common.util;

public class FastException extends Exception {

    public FastException() {
        // NOOP
    }

    public FastException(String message) {
        super(message);
    }

    @Override
    public Throwable initCause(Throwable cause) {
        return this;
    }

    @Override
    public Throwable fillInStackTrace() {
        return this;
    }
}
