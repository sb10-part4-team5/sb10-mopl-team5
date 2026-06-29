package com.codeit.team5.mopl.config;

import com.codeit.team5.mopl.content.client.sportsdb.SportsDbProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableConfigurationProperties(SportsDbProperties.class)
public class SportsDbWebClientConfig {

    @Bean
    public WebClient sportsDbWebClient(SportsDbProperties properties) {
        return WebClient.builder()
                .baseUrl(properties.baseUrl() + "/" + properties.apiKey())
                .defaultHeader(HttpHeaders.ACCEPT, "application/json")
                .build();
    }
}
