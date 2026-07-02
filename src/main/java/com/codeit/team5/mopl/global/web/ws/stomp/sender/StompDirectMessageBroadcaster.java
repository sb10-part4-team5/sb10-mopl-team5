package com.codeit.team5.mopl.global.web.ws.stomp.sender;

import com.codeit.team5.mopl.dm.dto.response.DirectMessageResponse;
import com.codeit.team5.mopl.dm.provider.DirectMessageBroadcaster;
import com.codeit.team5.mopl.global.web.ws.stomp.constant.StompConstants;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class StompDirectMessageBroadcaster implements DirectMessageBroadcaster {

    private final SimpMessagingTemplate messagingTemplate;

    public StompDirectMessageBroadcaster(@Lazy SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public void broadcast(DirectMessageResponse message) {
        String destination = StompConstants.conversationDmDestination(message.conversationId());
        messagingTemplate.convertAndSend(destination, message);
    }
}
