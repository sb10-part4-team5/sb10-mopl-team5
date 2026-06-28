package com.codeit.team5.mopl.auth.cookie;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mopl.auth.cookie")
public record AuthCookieProperties(
        boolean secure,
        String sameSite
) {
}
