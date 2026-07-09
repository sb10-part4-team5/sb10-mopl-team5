package com.codeit.team5.mopl.global.web.ws.stomp.handler;

import java.nio.charset.StandardCharsets;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.StompSubProtocolErrorHandler;
import com.codeit.team5.mopl.global.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class StompErrorHandler extends StompSubProtocolErrorHandler {

    @Override
    public Message<byte[]> handleClientMessageProcessingError(Message<byte[]> clientMessage,
            Throwable ex) {
        for (Throwable t = ex; t != null; t = t.getCause()) {
            if (t instanceof BusinessException exception) {
                return createErrorMessage(exception, extractReceiptId(clientMessage));
            }
        }

        return super.handleClientMessageProcessingError(clientMessage, ex);
    }

    private Message<byte[]> createErrorMessage(BusinessException ex, String receiptId) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.ERROR);
        accessor.setMessage(ex.getStatus() + ": " + ex.getMessage());
        accessor.addNativeHeader("exception-type", ex.getExceptionType());
        if (receiptId != null) {
            accessor.setReceiptId(receiptId);
        }

        byte[] payload = ex.getMessage().getBytes(StandardCharsets.UTF_8);
        log.warn("STOMP error: {}", ex.toString());
        return MessageBuilder.createMessage(payload, accessor.getMessageHeaders());
    }

    /**
     * 메시지에서 리셉트 아이디를 안전하게 추출합니다.
     */
    private String extractReceiptId(Message<byte[]> clientMessage) {
        StompHeaderAccessor accessor =
                StompHeaderAccessor.getAccessor(clientMessage, StompHeaderAccessor.class);
        return (accessor == null) ? null : accessor.getReceipt();
    }
}

