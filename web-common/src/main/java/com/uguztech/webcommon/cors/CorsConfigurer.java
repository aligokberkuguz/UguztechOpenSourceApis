package com.uguztech.webcommon.cors;

import io.javalin.config.JavalinConfig;

import java.util.List;

public final class CorsConfigurer {
    public CorsConfigurer() {
    }

    public static void configure(JavalinConfig config, List<String> allowedOrigins) {
        config.bundledPlugins.enableCors(cors -> {
            cors.addRule(rule -> {
                if (allowedOrigins.contains("*")) {
                    rule.anyHost();
                } else {
                    allowedOrigins.forEach(rule::allowHost);
                }
            });
        });
    }
}
