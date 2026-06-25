package com.codeit.team5.mopl.binarycontent.storage.s3;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mopl.storage.s3")
public record S3StorageProperties(
        String accessKey,
        String secretKey,
        String bucketName,
        String region
) {

}
