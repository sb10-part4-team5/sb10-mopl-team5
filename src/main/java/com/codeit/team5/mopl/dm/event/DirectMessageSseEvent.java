package com.codeit.team5.mopl.dm.event;

import com.codeit.team5.mopl.dm.dto.response.DirectMessageResponse;
import com.codeit.team5.mopl.global.infra.kafka.topic.KafkaTopics;
import org.springframework.modulith.events.Externalized;

@Externalized(KafkaTopics.DIRECT_MESSAGE_SSE
    + "::#{#this.message().receiver().id()}"
)
public record DirectMessageSseEvent(
        DirectMessageResponse message
) {
}
