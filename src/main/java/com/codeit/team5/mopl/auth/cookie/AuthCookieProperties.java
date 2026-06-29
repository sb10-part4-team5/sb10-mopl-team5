package com.codeit.team5.mopl.auth.cookie;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "mopl.auth.cookie")
@Validated
public record AuthCookieProperties(

        boolean secure,

        @NotBlank
        @Pattern(regexp = "Lax|Strict|None")
        String sameSite
) {
}
