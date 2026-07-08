package com.springboot.datagenerator.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(GeneratorProperties.class)
public class GeneratorConfig {
}
