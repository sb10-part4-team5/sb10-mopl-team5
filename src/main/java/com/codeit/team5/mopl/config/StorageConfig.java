package com.codeit.team5.mopl.config;

import com.codeit.team5.mopl.binarycontent.storage.local.LocalStorageProperties;
import com.codeit.team5.mopl.binarycontent.storage.s3.S3StorageProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({LocalStorageProperties.class, S3StorageProperties.class})
public class StorageConfig {

}
