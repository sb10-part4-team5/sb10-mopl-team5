package com.springboot.datagenerator.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Configuration
@EnableConfigurationProperties(GeneratorProperties.class)
@Validated
public class GeneratorConfig {
}
