package ru.violence.antivpn.common.config;

import lombok.experimental.UtilityClass;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@UtilityClass
public class Config {
    public static String KICK_REASON;

    public static class ProxyList {
        public static boolean ENABLED;

        public static long CACHE_DATABASE;
        public static long CACHE_MEMORY;

        public static long UPDATE_DELAY;

        public static Pattern PATTERN;

        public static List<String> URLS;
    }

    public static class IpApi {
        public static boolean ENABLED;

        public static long CACHE_DATABASE;
        public static long CACHE_MEMORY;

        public static boolean DENY_HOSTING;
        public static boolean DENY_PROXY;

        public static Set<String> BYPASS_COUNTRIES_HOSTING;
        public static Set<String> BYPASS_COUNTRIES_PROXY;

        public static long RESULT_AWAIT;

        public static boolean FORCE_CHECK_ENABLED;
        public static String FORCE_CHECK_KICK_REASON;

        public static long COOLDOWN;

        public static boolean COUNTRY_BLOCKER_ENABLED;
        public static String COUNTRY_BLOCKER_KICK_REASON;
        public static boolean COUNTRY_BLOCKER_WHITELIST;
        public static Set<String> COUNTRY_BLOCKER_COUNTRIES;
    }
}
