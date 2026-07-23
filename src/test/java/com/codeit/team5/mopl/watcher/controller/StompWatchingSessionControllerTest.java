package com.codeit.team5.mopl.watcher.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.codeit.team5.mopl.auth.security.details.MoplPrincipal;
import com.codeit.team5.mopl.watcher.constant.WatcherStatus;
import com.codeit.team5.mopl.watcher.dto.payload.WatchingSessionPayload;
import com.codeit.team5.mopl.watcher.service.WatchingSessionQueryService;

@ExtendWith(MockitoExtension.class)
class StompWatchingSessionControllerTest {

    @Mock
    private WatchingSessionQueryService service;

    @InjectMocks
    private StompWatchingSessionController controller;

    @Test
    @DisplayName("STOMP 구독 매핑 - 구독 시점에 Payload를 조회하여 반환한다")
    void subscribe_ShouldReturnPayload_WhenValidRequestReceived() {
        // Given
        UUID contentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        MoplPrincipal principal = mock(MoplPrincipal.class);
        given(principal.getId()).willReturn(userId);
        WatchingSessionPayload mockPayload = mock(WatchingSessionPayload.class);
        given(service.getWatchingSessionPayload(contentId, userId, WatcherStatus.JOIN))
                .willReturn(mockPayload);

        // When
        WatchingSessionPayload result = controller.subscribe(contentId, principal);

        // Then
        verify(service).getWatchingSessionPayload(contentId, userId, WatcherStatus.JOIN);
        assertThat(result).isEqualTo(mockPayload);
    }
}
