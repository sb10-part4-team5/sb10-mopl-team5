package com.codeit.team5.mopl.auth.event;

import static org.mockito.Mockito.verify;

import com.codeit.team5.mopl.auth.service.MailService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
}
