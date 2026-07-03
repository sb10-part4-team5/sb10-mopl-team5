package com.codeit.team5.mopl.config;

import com.codeit.team5.mopl.binarycontent.storage.local.LocalStorageProperties;
import com.codeit.team5.mopl.binarycontent.storage.s3.S3StorageProperties;
import java.util.Objects;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
@EnableConfigurationProperties({LocalStorageProperties.class, S3StorageProperties.class})
public class StorageConfig {

    // mopl.storage.type=s3 일 때만 S3Client 빈 생성
    @Bean
    @ConditionalOnProperty(name = "mopl.storage.type", havingValue = "s3")
    public S3Client s3Client(S3StorageProperties props) {
        String region = Objects.requireNonNull(props.region(), "mopl.storage.s3.region 설정이 필요합니다");

        String accessKey = props.accessKey();
        String secretKey = props.secretKey();

        boolean hasAccessKey = accessKey != null && !accessKey.isBlank();
        boolean hasSecretKey = secretKey != null && !secretKey.isBlank();
        if (hasAccessKey != hasSecretKey) {
            throw new IllegalStateException("accessKey와 secretKey는 함께 설정해야 합니다");
        }

        AwsCredentialsProvider credentialsProvider = hasAccessKey
                ? StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey))
                : DefaultCredentialsProvider.builder().build(); // prod: ECS task role 자격증명

        return S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(credentialsProvider)
                .overrideConfiguration(ClientOverrideConfiguration.builder()
                        .apiCallTimeout(props.apiCallTimeout())
                        .apiCallAttemptTimeout(props.apiCallAttemptTimeout())
                        .build())
                .build();
    }
}
