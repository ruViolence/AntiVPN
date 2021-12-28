package ru.violence.antivpn.checker;

import com.google.gson.JsonObject;
import lombok.Data;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import ru.violence.antivpn.util.Utils;

@Data
public class CheckResult {
    private final @Getter JsonObject json;

    public CheckResult(String json) {
        this.json = Utils.parseJson(json);
    }

    public @NotNull String getStatus() {
        return json.get("status").getAsString();
    }

    public @NotNull String getCountry() {
        return json.get("country").getAsString();
    }

    public @NotNull String getCountryCode() {
        return json.get("countryCode").getAsString();
    }

    public @NotNull String getRegion() {
        return json.get("region").getAsString();
    }

    public @NotNull String getRegionName() {
        return json.get("regionName").getAsString();
    }

    public @NotNull String getCity() {
        return json.get("city").getAsString();
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

    public boolean isMobile() {
        return json.get("mobile").getAsBoolean();
    }

    public boolean isProxy() {
        return json.get("proxy").getAsBoolean();
    }

    public boolean isHosting() {
        return json.get("hosting").getAsBoolean();
    }
}
