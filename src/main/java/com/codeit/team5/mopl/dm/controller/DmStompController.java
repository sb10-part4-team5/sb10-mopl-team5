package com.codeit.team5.mopl.dm.controller;

import com.codeit.team5.mopl.dm.dto.request.DirectMessageSendRequest;
import com.codeit.team5.mopl.dm.service.DmService;
import com.codeit.team5.mopl.global.web.ws.stomp.constant.StompConstants;
import java.security.Principal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class DmStompController {

    private final DmService dmService;

    @MessageMapping(StompConstants.PUB_CONVERSATION_DM)
    public void sendDirectMessage(
            Principal principal,
            @DestinationVariable("id") UUID conversationId,
            @Payload DirectMessageSendRequest request) {
        dmService.sendMessage(principal.getName(), conversationId, request.content());
    }
}
