package com.codeit.team5.mopl.dm.handler;

import com.codeit.team5.mopl.global.web.ws.stomp.constant.StompConstants;
import com.codeit.team5.mopl.global.web.ws.stomp.handler.AbstractStompUnsubscribeHandler;
import com.codeit.team5.mopl.global.web.ws.stomp.store.WebSocketSessionStore;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class DmUnsubscribeHandler extends AbstractStompUnsubscribeHandler {

    public DmUnsubscribeHandler(WebSocketSessionStore sessionStore) {
        super(sessionStore, StompConstants.SUB_CONVERSATION_DM);
    }

    @Override
    protected void doHandle(UUID conversationId, String email) {
        // 세션 스토어에서 구독 제거는 부모 클래스(unsubscribeSession)에서 처리
    }
}
