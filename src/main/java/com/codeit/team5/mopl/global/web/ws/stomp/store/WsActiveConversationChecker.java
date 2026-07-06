package com.codeit.team5.mopl.global.web.ws.stomp.store;

import com.codeit.team5.mopl.dm.provider.ActiveConversationChecker;
import com.codeit.team5.mopl.global.web.ws.stomp.constant.StompConstants;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WsActiveConversationChecker implements ActiveConversationChecker {

    private final WebSocketSessionStore webSocketSessionStore;

    @Override
    public boolean isViewing(UUID conversationId, String email) {
        String destination = StompConstants.SUB_CONVERSATION_DM.replace("{id}", conversationId.toString());
        return webSocketSessionStore.isSubscribed(email, destination);
    }
}
