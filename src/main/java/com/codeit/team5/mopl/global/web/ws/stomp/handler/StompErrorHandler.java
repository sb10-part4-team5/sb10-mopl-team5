package com.codeit.team5.mopl.global.web.ws.stomp.handler;

import java.nio.charset.StandardCharsets;
import org.springframework.core.NestedExceptionUtils;
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
        Throwable rootCause = NestedExceptionUtils.getMostSpecificCause(ex);
        StompHeaderAccessor clientHeaderAccessor = StompHeaderAccessor.wrap(clientMessage);
        String receiptId = clientHeaderAccessor.getReceipt();
        if (rootCause instanceof BusinessException exception) {
            return createErrorMessage(exception, receiptId);
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
