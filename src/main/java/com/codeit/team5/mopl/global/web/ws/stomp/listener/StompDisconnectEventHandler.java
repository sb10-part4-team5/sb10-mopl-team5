package com.codeit.team5.mopl.global.web.ws.stomp.listener;

import java.security.Principal;
import java.util.UUID;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import com.codeit.team5.mopl.global.exception.BusinessException;
import com.codeit.team5.mopl.global.web.ws.stomp.store.WebSocketSessionStore;
import com.codeit.team5.mopl.watcher.service.WatchingSessionCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class StompDisconnectEventHandler {

    private final WebSocketSessionStore sessionStore;
    private final WatchingSessionCommandService watchingSessionService;

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor =
                StompHeaderAccessor.getAccessor(event.getMessage(), StompHeaderAccessor.class);
        Principal user = accessor.getUser();
        if (user == null) {
            return;
        }
        UUID userId = UUID.fromString(user.getName());
        try {
            watchingSessionService.left(userId);
            log.info("WatchingSession cleaned up for disconnected user: {}", userId);
        } catch (BusinessException e) {
            log.error("No active WatchingSession to delete for user: {} | {}", userId,
                    e.toString());
        } catch (Exception e) {
            log.error(e.toString());
        } finally {
            sessionStore.disconnect(userId);
        }
    }
}
