package ru.violence.antivpn.util;

public class FastException extends Exception {

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
