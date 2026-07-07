package com.codeit.team5.mopl.global.web.ws.stomp.handler;

import com.codeit.team5.mopl.global.exception.BusinessException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.StompSubProtocolErrorHandler;

@Slf4j
@Component
public class StompErrorHandler extends StompSubProtocolErrorHandler {

    @Override
    public Message<byte[]> handleClientMessageProcessingError(Message<byte[]> clientMessage,
        Throwable ex) {
        Throwable cause = ex;
        while (cause != null) {
            if (cause instanceof BusinessException exception) {
                StompHeaderAccessor clientHeaderAccessor = StompHeaderAccessor.getAccessor(
                    clientMessage, StompHeaderAccessor.class);
                String receiptId = Objects.requireNonNull(clientHeaderAccessor).getReceipt();
                return createErrorMessage(exception, receiptId);
            }
            cause = cause.getCause();
        }
        return super.handleClientMessageProcessingError(clientMessage, ex);
    }

    private Message<byte[]> createErrorMessage(BusinessException ex, String receiptId) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.ERROR);
        accessor.setMessage(ex.getStatus().toString());
        accessor.addNativeHeader("exception-type", ex.getExceptionType());
        if (receiptId != null) {
            accessor.setReceiptId(receiptId);
        }
        byte[] payload = ex.getMessage().getBytes(StandardCharsets.UTF_8);
        accessor.setLeaveMutable(true);
        log.error(ex.toString());
        return MessageBuilder.createMessage(payload, accessor.getMessageHeaders());
    }
}
