package com.codeit.team5.mopl.content.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.codeit.team5.mopl.content.document.ContentDocument;
import com.codeit.team5.mopl.content.entity.Content;
import com.codeit.team5.mopl.content.mapper.ContentMapper;
import com.codeit.team5.mopl.content.repository.ContentRepository;
import com.codeit.team5.mopl.content.repository.ContentStatsRepository;
import com.codeit.team5.mopl.content.repository.SyncCursorRepository;
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

    private static final String CURSOR_NAME = "contentSearchSync";

    @Mock
    private ContentStatsRepository contentStatsRepository;

    @Mock
    private ContentRepository contentRepository;

    @Mock
    private ContentDocumentRepository contentDocumentRepository;

    @Mock
    private ContentMapper contentMapper;

    @Mock
    private SyncCursorRepository syncCursorRepository;

    @InjectMocks
    private ContentSearchSyncScheduler scheduler;

    @Captor
    private ArgumentCaptor<Instant> syncedAtCaptor;

    @Test
    @DisplayName("DB에 저장된 커서를 그대로 조회 기준으로 사용한다")
    void usesPersistedCursorAsQueryBound() {
        // given
        Instant persistedCursor = Instant.parse("2020-01-01T00:00:00Z");
        given(syncCursorRepository.findSyncedAt(CURSOR_NAME)).willReturn(persistedCursor);
        given(contentStatsRepository.findIdsUpdatedAfter(any())).willReturn(List.of());

        // when
        scheduler.syncUpdatedStats();

        // then
        verify(contentStatsRepository).findIdsUpdatedAfter(eq(persistedCursor));
    }

    @Test
    @DisplayName("변경된 통계가 없으면 색인 갱신 없이 커서만 갱신한다")
    void noUpdates_skipsIndexingButAdvancesCursor() {
        // given
        given(syncCursorRepository.findSyncedAt(CURSOR_NAME)).willReturn(Instant.EPOCH);
        given(contentStatsRepository.findIdsUpdatedAfter(any())).willReturn(List.of());

        // when
        scheduler.syncUpdatedStats();

        // then
        verify(syncCursorRepository).updateSyncedAt(eq(CURSOR_NAME), syncedAtCaptor.capture());
        assertThat(syncedAtCaptor.getValue()).isAfter(Instant.EPOCH);

        verifyNoInteractions(contentRepository);
        verifyNoInteractions(contentDocumentRepository);
        verifyNoInteractions(contentMapper);
    }

    @Test
    @DisplayName("변경된 통계가 있으면 콘텐츠를 재조회해 문서로 매핑 후 저장하고 커서를 갱신한다")
    void withUpdates_reindexesContentsAndAdvancesCursor() {
        // given
        UUID contentId = UUID.randomUUID();
        Content content = mock(Content.class);
        ContentDocument document = mock(ContentDocument.class);

        given(syncCursorRepository.findSyncedAt(CURSOR_NAME)).willReturn(Instant.EPOCH);
        given(contentStatsRepository.findIdsUpdatedAfter(any())).willReturn(List.of(contentId));
        given(contentRepository.findAllWithStatsAndTagsByIdIn(List.of(contentId)))
                .willReturn(List.of(content));
        given(contentMapper.toDocument(content)).willReturn(document);

        // when
        scheduler.syncUpdatedStats();

        // then
        verify(contentDocumentRepository).saveAll(List.of(document));
        verify(syncCursorRepository).updateSyncedAt(eq(CURSOR_NAME), any());
    }

    @Test
    @DisplayName("변경분 ID는 있지만 재조회 결과가 비어있으면 저장은 생략하고 커서는 갱신한다")
    void reFetchEmpty_skipsSaveButAdvancesCursor() {
        // given
        UUID contentId = UUID.randomUUID();
        given(syncCursorRepository.findSyncedAt(CURSOR_NAME)).willReturn(Instant.EPOCH);
        given(contentStatsRepository.findIdsUpdatedAfter(any())).willReturn(List.of(contentId));
        given(contentRepository.findAllWithStatsAndTagsByIdIn(List.of(contentId)))
                .willReturn(List.of());

        // when
        scheduler.syncUpdatedStats();

        // then
        verifyNoInteractions(contentDocumentRepository);
        verifyNoInteractions(contentMapper);
        verify(syncCursorRepository).updateSyncedAt(eq(CURSOR_NAME), any());
    }
}
