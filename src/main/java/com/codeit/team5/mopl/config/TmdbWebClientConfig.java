package com.codeit.team5.mopl.config;

import com.codeit.team5.mopl.content.client.tmdb.TmdbProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableConfigurationProperties(TmdbProperties.class)
public class TmdbWebClientConfig {

    @Bean
    public WebClient tmdbWebClient(TmdbProperties properties) {
        return WebClient.builder()
                .baseUrl(properties.baseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.accessToken())
                .defaultHeader(HttpHeaders.ACCEPT, "application/json")
                .build();
    }
}
