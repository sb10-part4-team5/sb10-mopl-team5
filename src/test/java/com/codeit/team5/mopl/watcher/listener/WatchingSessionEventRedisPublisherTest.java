package com.codeit.team5.mopl.watcher.listener;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.codeit.team5.mopl.watcher.constant.WatcherRedisConstants;
import com.codeit.team5.mopl.watcher.constant.WatcherStatus;
import com.codeit.team5.mopl.watcher.dto.payload.WatchingSessionPayload;
import com.codeit.team5.mopl.watcher.event.WatcherJoinedEvent;
import com.codeit.team5.mopl.watcher.service.WatchingSessionQueryService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

@ExtendWith(MockitoExtension.class)
class WatchingSessionEventRedisPublisherTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private WatchingSessionQueryService queryService;

    @InjectMocks
    private WatchingSessionEventRedisPublisher publisher;

    @Test
    @DisplayName("onWatcherJoined - 레디스 발행 성공")
    void onWatcherJoined_Success() throws Exception {
        // given
        UUID contentId = UUID.randomUUID();
        UUID watcherId = UUID.randomUUID();
        WatcherJoinedEvent event = new WatcherJoinedEvent(contentId, watcherId);
        WatchingSessionPayload payload = mock(WatchingSessionPayload.class);
        
        when(queryService.getWatchingSessionPayload(watcherId, WatcherStatus.JOIN)).thenReturn(payload);
        when(objectMapper.writeValueAsString(any())).thenReturn("dummy_json");

        // when
        publisher.onWatcherJoined(event);

        // then
        verify(stringRedisTemplate).convertAndSend(WatcherRedisConstants.WATCHING_SESSION_TOPIC, "dummy_json");
    }

    @Test
    @DisplayName("onWatcherJoined - 페이로드 조회 실패시 발행 안함")
    void onWatcherJoined_QueryServiceException() {
        // given
        UUID contentId = UUID.randomUUID();
        UUID watcherId = UUID.randomUUID();
        WatcherJoinedEvent event = new WatcherJoinedEvent(contentId, watcherId);
        
        when(queryService.getWatchingSessionPayload(watcherId, WatcherStatus.JOIN)).thenThrow(new RuntimeException("Query failure"));

        // when
        publisher.onWatcherJoined(event);

        // then
        verifyNoInteractions(stringRedisTemplate);
    }

    @Test
    @DisplayName("onWatcherJoined - JsonProcessingException 발생시 에러 로깅")
    void onWatcherJoined_JsonProcessingException() throws Exception {
        // given
        UUID contentId = UUID.randomUUID();
        UUID watcherId = UUID.randomUUID();
        WatcherJoinedEvent event = new WatcherJoinedEvent(contentId, watcherId);
        WatchingSessionPayload payload = mock(WatchingSessionPayload.class);
        
        when(queryService.getWatchingSessionPayload(watcherId, WatcherStatus.JOIN)).thenReturn(payload);
        when(objectMapper.writeValueAsString(any())).thenThrow(mock(JsonProcessingException.class));

        // when
        publisher.onWatcherJoined(event);

        // then
        verifyNoInteractions(stringRedisTemplate);
    }
}
