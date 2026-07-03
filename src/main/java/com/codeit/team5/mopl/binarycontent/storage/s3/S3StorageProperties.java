package com.codeit.team5.mopl.binarycontent.storage.s3;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mopl.storage.s3")
public record S3StorageProperties(
        String accessKey,
        String secretKey,
        String bucketName,
        String region,
        Duration apiCallTimeout,
        Duration apiCallAttemptTimeout
) {

    public S3StorageProperties {
        if (apiCallTimeout == null) {
            apiCallTimeout = Duration.ofSeconds(10);
        }
        if (apiCallAttemptTimeout == null) {
            apiCallAttemptTimeout = Duration.ofSeconds(5);
        }
    }
}
