package com.codeit.team5.mopl.global.infra.redis.config;

import java.util.concurrent.Executor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

import com.codeit.team5.mopl.dm.constant.DmRedisConstants;
import com.codeit.team5.mopl.dm.infra.DmRedisMessageSubscriber;
import com.codeit.team5.mopl.watcher.constant.WatcherRedisConstants;
import com.codeit.team5.mopl.watcher.infra.RedisMessageSubscriber;

@Configuration
public class RedisMessageListenerConfig {

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            MessageListenerAdapter listenerAdapter,
            MessageListenerAdapter dmListenerAdapter,
            @Qualifier("redisMessageWorker") Executor worker) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(listenerAdapter, new ChannelTopic(WatcherRedisConstants.WATCHING_SESSION_TOPIC));
        container.addMessageListener(dmListenerAdapter, new ChannelTopic(DmRedisConstants.DM_BROADCAST_TOPIC));
        container.setTaskExecutor(worker);
        return container;
    }

    @Bean
    public MessageListenerAdapter listenerAdapter(RedisMessageSubscriber subscriber) {
        return new MessageListenerAdapter(subscriber, "onMessage");
    }

    @Bean
    public MessageListenerAdapter dmListenerAdapter(DmRedisMessageSubscriber subscriber) {
        return new MessageListenerAdapter(subscriber, "onMessage");
    }
}
