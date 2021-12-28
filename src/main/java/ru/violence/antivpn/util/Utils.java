package ru.violence.antivpn.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;

@UtilityClass
public class Utils {
    public @NotNull String joinArgs(String @NotNull [] args, @Range(from = 0, to = Integer.MAX_VALUE) int startFrom) {
        StringBuilder sb = new StringBuilder();
        for (int i = startFrom; i < args.length; i++) {
            if (sb.length() != 0) sb.append(' ');
            sb.append(args[i]);
        }
        return sb.toString();
    }

    public @NotNull JsonObject parseJson(@NotNull String jsonText) {
        return new JsonParser().parse(jsonText).getAsJsonObject();
    }

    public @NotNull String readStringFromUrl(@NotNull String url) throws IOException {
        try (InputStream is = new URL(url).openStream()) {
            InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
            return readAll(isr);
        }
    }

    private @NotNull String readAll(@NotNull Reader rd) throws IOException {
        StringBuilder sb = new StringBuilder();
        int chr;
        while ((chr = rd.read()) != -1) {
            sb.append((char) chr);
        }
        return sb.toString();
    }
}
