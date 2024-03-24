package ru.violence.antivpn.common.model.exception;

import org.jetbrains.annotations.NotNull;
import ru.violence.antivpn.common.model.CheckResult;
import ru.violence.antivpn.common.util.FastException;

public class ManuallyBlockedException extends FastException {
    private final @NotNull CheckResult checkResult;

    public ManuallyBlockedException(@NotNull CheckResult checkResult) {
        this.checkResult = checkResult;
    }

    public @NotNull CheckResult getCheckResult() {
        return checkResult;
    }
}
