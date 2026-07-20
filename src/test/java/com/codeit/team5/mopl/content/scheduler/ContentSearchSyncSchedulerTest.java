package com.codeit.team5.mopl.content.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.codeit.team5.mopl.content.document.ContentDocument;
import com.codeit.team5.mopl.content.entity.Content;
import com.codeit.team5.mopl.content.mapper.ContentMapper;
import com.codeit.team5.mopl.content.repository.ContentRepository;
import com.codeit.team5.mopl.content.repository.ContentStatsRepository;
import com.codeit.team5.mopl.content.repository.opensearch.ContentDocumentRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ContentSearchSyncSchedulerTest {

    @Mock
    private ContentStatsRepository contentStatsRepository;

    @Mock
    private ContentRepository contentRepository;

    @Mock
    private ContentDocumentRepository contentDocumentRepository;

    @Mock
    private ContentMapper contentMapper;

    @InjectMocks
    private ContentSearchSyncScheduler scheduler;

    @Captor
    private ArgumentCaptor<Instant> sinceCaptor;

    @Test
    @DisplayName("변경된 통계가 없으면 색인 갱신 없이 lastSyncedAt만 전진시킨다")
    void noUpdates_skipsIndexingButAdvancesCursor() {
        // given
        given(contentStatsRepository.findIdsUpdatedAfter(any())).willReturn(List.of());

        // when
        scheduler.syncUpdatedStats();
        scheduler.syncUpdatedStats();

        // then
        verify(contentStatsRepository, times(2)).findIdsUpdatedAfter(sinceCaptor.capture());
        List<Instant> captured = sinceCaptor.getAllValues();
        assertThat(captured.get(0)).isEqualTo(Instant.EPOCH);
        assertThat(captured.get(1)).isAfter(captured.get(0));

        verifyNoInteractions(contentRepository);
        verifyNoInteractions(contentDocumentRepository);
        verifyNoInteractions(contentMapper);
    }

    @Test
    @DisplayName("변경된 통계가 있으면 콘텐츠를 재조회해 문서로 매핑 후 저장한다")
    void withUpdates_reindexesContents() {
        // given
        UUID contentId = UUID.randomUUID();
        Content content = mock(Content.class);
        ContentDocument document = mock(ContentDocument.class);

        given(contentStatsRepository.findIdsUpdatedAfter(any())).willReturn(List.of(contentId));
        given(contentRepository.findAllWithStatsAndTagsByIdIn(List.of(contentId)))
                .willReturn(List.of(content));
        given(contentMapper.toDocument(content)).willReturn(document);

        // when
        scheduler.syncUpdatedStats();

        // then
        verify(contentDocumentRepository).saveAll(List.of(document));
    }

    @Test
    @DisplayName("변경분 ID는 있지만 재조회 결과가 비어있으면 저장을 생략한다")
    void reFetchEmpty_skipsSave() {
        // given
        UUID contentId = UUID.randomUUID();
        given(contentStatsRepository.findIdsUpdatedAfter(any())).willReturn(List.of(contentId));
        given(contentRepository.findAllWithStatsAndTagsByIdIn(List.of(contentId)))
                .willReturn(List.of());

        // when
        scheduler.syncUpdatedStats();

        // then
        verifyNoInteractions(contentDocumentRepository);
        verifyNoInteractions(contentMapper);
    }
}
