package com.codeit.team5.mopl.config;

import com.codeit.team5.mopl.config.properties.AdminProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AdminProperties.class)
public class AdminAccountConfig {
}
