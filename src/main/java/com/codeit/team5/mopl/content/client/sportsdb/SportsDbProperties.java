package com.codeit.team5.mopl.content.client.sportsdb;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "sports-db")
public record SportsDbProperties(

        @NotBlank
        String apiKey,

        @NotBlank
        String baseUrl
) {
}
