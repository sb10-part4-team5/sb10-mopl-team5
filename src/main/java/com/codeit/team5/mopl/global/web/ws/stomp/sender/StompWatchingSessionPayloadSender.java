package com.codeit.team5.mopl.global.web.ws.stomp.sender;

import com.codeit.team5.mopl.global.web.ws.stomp.constant.StompConstants;
import com.codeit.team5.mopl.watcher.provider.WatchingSessionPayloadSender;
import java.util.UUID;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class StompWatchingSessionPayloadSender implements WatchingSessionPayloadSender {

    private final SimpMessagingTemplate template;

    public StompWatchingSessionPayloadSender(@Lazy SimpMessagingTemplate template) {
        this.template = template;
    }

    @Override
    public void send(UUID targetId, Object payload) {
        String destination = StompConstants.SUB_WATCHING_CONTENT.replace("{id}",
                targetId.toString());
        template.convertAndSend(destination, payload);
    }
}
