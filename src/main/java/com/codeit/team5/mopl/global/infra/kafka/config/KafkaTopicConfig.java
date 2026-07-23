package com.codeit.team5.mopl.global.infra.kafka.config;

import com.codeit.team5.mopl.global.infra.kafka.topic.KafkaTopics;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;

// SSE 브로드캐스트 토픽을 앱 기동 시 KafkaAdmin이 생성
@Configuration
public class KafkaTopicConfig {

    @Bean
    public KafkaAdmin.NewTopics sseTopics() {
        return new KafkaAdmin.NewTopics(
            TopicBuilder.name(KafkaTopics.NOTIFICATION_CREATED_SSE).partitions(1).replicas(1).build(),
            TopicBuilder.name(KafkaTopics.NOTIFICATION_BATCH_CREATED_SSE).partitions(1).replicas(1).build(),
            TopicBuilder.name(KafkaTopics.DIRECT_MESSAGE_SSE).partitions(1).replicas(1).build()
        );
    }
}
