package com.uguztech.webcommon.config;

import java.util.Arrays;
import java.util.List;

public final class EnvConfig {

    private EnvConfig() {}

    public static String getOrDefault(String key, String defaultValue) {
        return System.getenv().getOrDefault(key, defaultValue);
    }

    public static List<String> getListOrDefault(String key, String defaultValue) {
        String value = getOrDefault(key, defaultValue);
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .toList();
    }
}
