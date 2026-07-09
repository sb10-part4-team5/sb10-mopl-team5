package com.codeit.team5.mopl.watcher.controller;

import java.util.UUID;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import com.codeit.team5.mopl.auth.security.details.MoplPrincipal;
import com.codeit.team5.mopl.global.web.ws.stomp.constant.StompConstants;
import com.codeit.team5.mopl.watcher.constant.WatcherStatus;
import com.codeit.team5.mopl.watcher.dto.payload.WatchingSessionPayload;
import com.codeit.team5.mopl.watcher.provider.WatchingSessionPayloadSender;
import com.codeit.team5.mopl.watcher.service.WatchingSessionQueryService;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class StompWatchingSessionController {

    private final WatchingSessionQueryService service;
    private final WatchingSessionPayloadSender payloadSender;

    @SubscribeMapping(StompConstants.WATCHING_CONTENT)
    public void subscribe(@DestinationVariable(value = "id") UUID contentId,
            @AuthenticationPrincipal MoplPrincipal principal) {
        WatchingSessionPayload payload =
                service.getWatchingSessionPayload(principal.getId(), WatcherStatus.JOIN);
        payloadSender.send(contentId, payload);
    }
}
