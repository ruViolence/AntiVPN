package ru.violence.antivpn.common.checker;

import com.google.gson.JsonObject;
import lombok.Data;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import ru.violence.antivpn.common.util.Utils;

@Data
public class CheckResult {
    private final @Getter JsonObject json;

    public CheckResult(String json) {
        this.json = Utils.parseJson(json);
    }

    public @NotNull String getStatus() {
        return json.get("status").getAsString();
    }

    public @NotNull String getCountryCode() {
        return json.get("countryCode").getAsString();
    }

    public @NotNull String getIsp() {
        return json.get("isp").getAsString();
    }

    public @NotNull String getOrg() {
        return json.get("org").getAsString();
    }

    public @NotNull String getAs() {
        return json.get("as").getAsString();
    }

    public @NotNull String getAsname() {
        return json.get("asname").getAsString();
    }

    public boolean isProxy() {
        return json.get("proxy").getAsBoolean();
    }

    public boolean isHosting() {
        return json.get("hosting").getAsBoolean();
    }
}
