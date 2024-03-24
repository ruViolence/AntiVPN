package ru.violence.antivpn.common.model.exception;

import org.jetbrains.annotations.Nullable;
import ru.violence.antivpn.common.model.CheckResult;
import ru.violence.antivpn.common.util.FastException;

public class ProxyBlockedException extends FastException {
    public static final ProxyBlockedException PROXY_LIST = new ProxyBlockedException(null);

    private final @Nullable CheckResult checkResult;

    public ProxyBlockedException(@Nullable CheckResult checkResult) {
        this.checkResult = checkResult;
    }

    public @Nullable CheckResult getCheckResult() {
        return checkResult;
    }
}
