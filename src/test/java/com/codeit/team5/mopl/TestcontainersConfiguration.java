package com.codeit.team5.mopl;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    public PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>("postgres:17");
    }

    // @Bean
    // @ServiceConnection(name = "redis")
    // public RedisContainer redisContainer() {
    // return new RedisContainer(DockerImageName.parse("redis:7"));
    // }

    // @Bean
    // @ServiceConnection
    // public ConfluentKafkaContainer kafkaContainer() {
    // return new ConfluentKafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.1"));
    // }
}
