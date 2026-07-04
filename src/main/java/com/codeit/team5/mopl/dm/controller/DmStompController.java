package com.codeit.team5.mopl.dm.controller;

import com.codeit.team5.mopl.dm.dto.request.DirectMessageSendRequest;
import com.codeit.team5.mopl.dm.service.DirectMessageService;
import com.codeit.team5.mopl.global.web.ws.stomp.constant.StompConstants;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
@Slf4j
public class DmStompController {

    private final DirectMessageService directMessageService;

    @MessageMapping(StompConstants.PUB_CONVERSATION_DM)
    public void sendDirectMessage(
            Principal principal,
            @DestinationVariable("id") UUID conversationId,
            @Valid @Payload DirectMessageSendRequest request) {
        log.info("DM STOMP send: conversationId={}, sender={}", conversationId, principal.getName());
        directMessageService.sendMessage(principal.getName(), conversationId, request.content());
    }

    @MessageExceptionHandler(MethodArgumentNotValidException.class)
    public void handleValidationException(MethodArgumentNotValidException e, Principal principal) {
        String sender = principal != null ? principal.getName() : "unknown";
        log.warn("DM STOMP validation failed: sender={}, reason={}", sender, e.getMessage());
    }
}
