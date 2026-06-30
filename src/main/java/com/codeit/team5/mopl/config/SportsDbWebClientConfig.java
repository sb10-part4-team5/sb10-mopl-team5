package com.codeit.team5.mopl.config;

import com.codeit.team5.mopl.content.client.sportsdb.SportsDbProperties;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Configuration
@EnableConfigurationProperties(SportsDbProperties.class)
public class SportsDbWebClientConfig {

    private static final int CONNECT_TIMEOUT_MS = 3_000;
    private static final Duration RESPONSE_TIMEOUT = Duration.ofSeconds(10);
    private static final int READ_TIMEOUT_SEC = 10;

    @Bean
    public WebClient sportsDbWebClient(SportsDbProperties properties) {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, CONNECT_TIMEOUT_MS)
                .responseTimeout(RESPONSE_TIMEOUT)
                .doOnConnected(conn -> conn.addHandlerLast(
                        new ReadTimeoutHandler(READ_TIMEOUT_SEC, TimeUnit.SECONDS)));

        return WebClient.builder()
                .baseUrl(properties.baseUrl() + "/" + properties.apiKey())
                .defaultHeader(HttpHeaders.ACCEPT, "application/json")
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}
