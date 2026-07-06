package com.codeit.team5.mopl.config;

import com.codeit.team5.mopl.auth.support.AuthCookieProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AuthCookieProperties.class)
public class AuthCookiePropertiesConfig {

}
