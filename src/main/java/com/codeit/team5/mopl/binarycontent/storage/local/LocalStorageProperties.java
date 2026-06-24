package com.codeit.team5.mopl.binarycontent.storage.local;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "storage.local")
public record LocalStorageProperties(
        String baseUrl,
        String uploadDir
) {

}
