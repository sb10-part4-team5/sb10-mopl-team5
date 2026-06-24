package com.codeit.team5.mopl.binarycontent.storage.s3;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "storage.s3")
public record S3StorageProperties(
        String accessKey,
        String privateKey,
        String bucketName,
        String region
) {

}
