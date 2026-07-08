package com.codeit.team5.mopl.global.web.ws.stomp.handler;

import static org.assertj.core.api.Assertions.assertThat;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import com.codeit.team5.mopl.global.exception.BusinessException;

class StompErrorHandlerTest {

    private final StompErrorHandler errorHandler = new StompErrorHandler();

    @Test
    @DisplayName("BusinessException 발생 시 정확한 ERROR STOMP 프레임을 반환한다")
    void handleClientMessageProcessingError_businessException() {
        // given
        StompHeaderAccessor clientAccessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        clientAccessor.setReceipt("test-receipt-123");
        Message<byte[]> clientMessage =
                MessageBuilder.createMessage(new byte[0], clientAccessor.getMessageHeaders());

        BusinessException businessException =
                new TestBusinessException(HttpStatus.UNAUTHORIZED, "권한이 없습니다.");

        Throwable wrappedException = new MessageDeliveryException(clientMessage,
                "Message Delivery Error", businessException);

        // when
        Message<byte[]> resultMessage =
                errorHandler.handleClientMessageProcessingError(clientMessage, wrappedException);

        // then
        StompHeaderAccessor resultAccessor = StompHeaderAccessor.wrap(resultMessage);

        // 1. STOMP 커맨드가 ERROR 인지 확인
        assertThat(resultAccessor.getCommand()).isEqualTo(StompCommand.ERROR);

        // 2. Receipt ID가 원본과 동일하게 복사되었는지 확인
        assertThat(resultAccessor.getReceiptId()).isEqualTo("test-receipt-123");

        // 3. 커스텀 헤더들이 정확하게 세팅되었는지 확인
        assertThat(resultAccessor.getMessage()).isEqualTo(HttpStatus.UNAUTHORIZED + ": 권한이 없습니다.");
        assertThat(resultAccessor.getFirstNativeHeader("exception-type"))
                .isEqualTo("TestBusinessException");

        // 4. Payload(본문)에 예외 메시지가 잘 들어갔는지 확인
        String payloadString = new String(resultMessage.getPayload(), StandardCharsets.UTF_8);
        assertThat(payloadString).isEqualTo("권한이 없습니다.");
    }

    // 테스트를 위한 임의의 BusinessException 구현체
    private static class TestBusinessException extends BusinessException {
        public TestBusinessException(HttpStatus status, String message) {
            super(status, message);
        }
    }
}
