package com.codeit.team5.mopl.global.web.ws.stomp.listener;

import com.codeit.team5.mopl.global.web.ws.stomp.store.WebSocketSessionStore;
import com.codeit.team5.mopl.watcher.exception.WatchingSessionNotFoundException;
import com.codeit.team5.mopl.watcher.service.WatchingSessionService;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Slf4j
@Component
@RequiredArgsConstructor
public class StompDisconnectEventHandler {

    private final WebSocketSessionStore sessionStore;
    private final WatchingSessionService watchingSessionService;

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal user = accessor.getUser();
        if (user == null) {
            return;
        }
        String email = user.getName();
        sessionStore.disconnect(email);
        try {
            watchingSessionService.delete(email);
            log.info("WatchingSession cleaned up for disconnected user: {}", email);
        } catch (WatchingSessionNotFoundException e) {
            log.debug("No active WatchingSession to delete for user: {}", email);
        }
    }
}
