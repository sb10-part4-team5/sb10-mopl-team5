package com.codeit.team5.mopl.config.properties;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "mopl.admin")
public record AdminProperties (

        @NotBlank
        String email,

        @NotBlank
        String password,

        @NotBlank
        String name
) {

}
