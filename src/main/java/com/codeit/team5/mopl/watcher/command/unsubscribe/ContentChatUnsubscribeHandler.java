package com.codeit.team5.mopl.watcher.command.unsubscribe;

import com.codeit.team5.mopl.global.web.ws.stomp.constant.StompConstants;
import com.codeit.team5.mopl.global.web.ws.stomp.handler.AbstractStompUnsubscribeHandler;
import com.codeit.team5.mopl.global.web.ws.stomp.store.WebSocketSessionStore;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ContentChatUnsubscribeHandler extends AbstractStompUnsubscribeHandler {

    public ContentChatUnsubscribeHandler(
            WebSocketSessionStore sessionStore) {
        super(sessionStore, StompConstants.SUB_WATCHING_CONTENT_CHAT);
    }

    @Override
    protected void doHandle(UUID targetId, String email) {
        // 추가 로직 필요 없음
    }
}
