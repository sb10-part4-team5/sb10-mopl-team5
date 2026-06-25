package com.codeit.team5.mopl.watcher.listener;

import com.codeit.team5.mopl.global.web.ws.stomp.event.WatchingContentJoinedEvent;
import com.codeit.team5.mopl.global.web.ws.stomp.event.WatchingContentLeftEvent;
import com.codeit.team5.mopl.watcher.constant.WatcherStatus;
import com.codeit.team5.mopl.watcher.dto.payload.WatchingSessionPayload;
import com.codeit.team5.mopl.watcher.dto.response.WatchingSessionResponse;
import com.codeit.team5.mopl.watcher.provider.WatchingSessionPayloadSender;
import com.codeit.team5.mopl.watcher.service.WatchingSessionService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WatchingContentEventListener {

    private final WatchingSessionService service;
    private final WatchingSessionPayloadSender payloadSender;

    @EventListener
    public void handleJoinedEvent(WatchingContentJoinedEvent event) {
        UUID contentId = event.contentId();
        UUID watcherId = event.watcherId();
        WatchingSessionResponse response = service.create(contentId, watcherId);
        Long view = service.getCurrentWatchingContentView(contentId);
        payloadSender.send(contentId,
                new WatchingSessionPayload(WatcherStatus.JOIN, response, view));
    }

    @EventListener
    public void handleLeftEvent(WatchingContentLeftEvent event) {
        UUID watcherId = event.watcherId();
        UUID contentId = event.contentId();
        WatchingSessionResponse response = service.findSessionByWatchId(watcherId);
        service.delete(watcherId);
        Long view = service.getCurrentWatchingContentView(contentId);
        payloadSender.send(contentId,
                new WatchingSessionPayload(WatcherStatus.LEAVE, response, view));
    }
}
