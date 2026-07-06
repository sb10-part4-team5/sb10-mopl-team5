package com.codeit.team5.mopl.binarycontent.storage.s3;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mopl.storage.s3")
public record S3StorageProperties(
        String accessKey,
        String secretKey,
        String bucketName,
        String region,
        String cdnBaseUrl,
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
        if (apiCallAttemptTimeout.compareTo(apiCallTimeout) > 0) {
            throw new IllegalArgumentException(
                    "apiCallAttemptTimeout(" + apiCallAttemptTimeout
                            + ")은 apiCallTimeout(" + apiCallTimeout + ")보다 클 수 없습니다.");
        }
    }
}
