package com.codeit.team5.mopl.auth.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.verify;

import com.codeit.team5.mopl.auth.service.MailService;
import java.lang.reflect.Method;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@ExtendWith(MockitoExtension.class)
class TemporaryPasswordEventListenerTest {

    @Mock
    private MailService mailService;

    @InjectMocks
    private TemporaryPasswordEventListener temporaryPasswordEventListener;

    @Test
    @DisplayName("임시 비밀번호 발급 이벤트를 수신하면 임시 비밀번호 이메일을 전송한다")
    void handleTemporaryPasswordIssued_success() {
        // Given
        TemporaryPasswordIssuedEvent event =
                new TemporaryPasswordIssuedEvent("user@example.com", "Temp1234");

        // When
        temporaryPasswordEventListener.handleTemporaryPasswordIssued(event);

        // Then
        verify(mailService).sendTemporaryPassword("user@example.com", "Temp1234");
    }

    @Test
    @DisplayName("임시 비밀번호 발급 이벤트 핸들러는 커밋 후 비동기로 재시도한다")
    void handleTemporaryPasswordIssued_annotations_success() throws NoSuchMethodException {
        // Given
        Method method = TemporaryPasswordEventListener.class.getMethod(
                "handleTemporaryPasswordIssued",
                TemporaryPasswordIssuedEvent.class
        );

        // When
        Async async = method.getAnnotation(Async.class);
        Retryable retryable = method.getAnnotation(Retryable.class);
        TransactionalEventListener listener = method.getAnnotation(TransactionalEventListener.class);

        // Then
        assertThat(async).isNotNull();
        assertThat(retryable).isNotNull();
        assertThat(retryable.maxAttempts()).isEqualTo(3);
        assertThat(listener).isNotNull();
        assertThat(listener.phase()).isEqualTo(TransactionPhase.AFTER_COMMIT);
    }

    @Test
    @DisplayName("임시 비밀번호 메일 전송 재시도 실패는 예외를 전파하지 않는다")
    void recover_success() {
        // Given
        MailSendException exception = new MailSendException("mail failed");

        // When & Then
        assertThatCode(() -> temporaryPasswordEventListener.recover(exception))
                .doesNotThrowAnyException();
    }
}
