package com.codeit.team5.mopl.content.finder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codeit.team5.mopl.content.document.ContentDocument;
import com.codeit.team5.mopl.content.dto.request.ContentCursorRequest;
import com.codeit.team5.mopl.content.dto.response.ContentResponse;
import com.codeit.team5.mopl.content.entity.ContentSortByType;
import com.codeit.team5.mopl.content.entity.ContentType;
import com.codeit.team5.mopl.content.mapper.ContentMapper;
import com.codeit.team5.mopl.global.dto.CursorResponse;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Query;

@ExtendWith(MockitoExtension.class)
class ContentSearchFinderTest {

    @Mock
    private ElasticsearchOperations operations;

    @Mock
    private ContentMapper contentMapper;

    @InjectMocks
    private ContentSearchFinder contentSearchFinder;

    @Test
    @DisplayName("다음 페이지가 없으면 조회된 문서를 모두 매핑하고 hasNext=false를 반환한다")
    void search_noNextPage_mapsAllHitsAndReturnsCursorResponse() {
        // given
        ContentCursorRequest request = new ContentCursorRequest(
                null, "영화", null, null, null,
                2, Sort.Direction.DESC, ContentSortByType.WATCHER_COUNT
        );
        ContentDocument document1 = contentDocument("id-1", "영화1");
        ContentDocument document2 = contentDocument("id-2", "영화2");
        ContentResponse response1 = contentResponse("영화1");
        ContentResponse response2 = contentResponse("영화2");

        @SuppressWarnings("unchecked")
        SearchHits<ContentDocument> hits = mock(SearchHits.class);
        when(hits.getSearchHits()).thenReturn(List.of(
                hitOf(document1, 10L, "id-1"),
                hitOf(document2, 5L, "id-2")
        ));
        when(hits.getTotalHits()).thenReturn(2L);
        when(operations.search(any(Query.class), eq(ContentDocument.class))).thenReturn(hits);
        when(contentMapper.toDto(document1)).thenReturn(response1);
        when(contentMapper.toDto(document2)).thenReturn(response2);

        // when
        CursorResponse<ContentResponse> result = contentSearchFinder.search(request);

        // then
        assertThat(result.data()).containsExactly(response1, response2);
        assertThat(result.hasNext()).isFalse();
        assertThat(result.nextCursor()).isNull();
        assertThat(result.nextIdAfter()).isNull();
        assertThat(result.totalCount()).isEqualTo(2L);

        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        verify(operations).search(queryCaptor.capture(), eq(ContentDocument.class));
        assertThat(queryCaptor.getValue().getSearchAfter()).isNull();
    }

    @Test
    @DisplayName("정렬 기준이 createdAt이면 다음 커서를 ISO 문자열로 변환한다")
    void search_hasNextPage_withCreatedAt_convertsEpochMillisToIsoCursor() {
        // given: limit=2인데 3건이 조회되어 hasNext=true, 마지막으로 반환되는 건 2번째 문서다
        ContentCursorRequest request = new ContentCursorRequest(
                null, "영화", null, null, null,
                2, Sort.Direction.DESC, ContentSortByType.CREATED_AT
        );
        ContentDocument document1 = contentDocument("id-1", "영화1");
        ContentDocument document2 = contentDocument("id-2", "영화2");
        ContentDocument document3 = contentDocument("id-3", "영화3");
        Instant lastCreatedAt = Instant.parse("2026-07-01T00:00:00Z");

        @SuppressWarnings("unchecked")
        SearchHits<ContentDocument> hits = mock(SearchHits.class);
        when(hits.getSearchHits()).thenReturn(List.of(
                hitOf(document1, 1_800_000_000_000L, "id-1"),
                hitOf(document2, lastCreatedAt.toEpochMilli(), "id-2"),
                hitOf(document3, 1_700_000_000_000L, "id-3")
        ));
        when(hits.getTotalHits()).thenReturn(3L);
        when(operations.search(any(Query.class), eq(ContentDocument.class))).thenReturn(hits);
        when(contentMapper.toDto(document1)).thenReturn(contentResponse("영화1"));
        when(contentMapper.toDto(document2)).thenReturn(contentResponse("영화2"));

        // when
        CursorResponse<ContentResponse> result = contentSearchFinder.search(request);

        // then
        assertThat(result.hasNext()).isTrue();
        assertThat(result.data()).hasSize(2);
        assertThat(result.nextCursor()).isEqualTo(lastCreatedAt.toString());
        assertThat(result.nextIdAfter()).isEqualTo("id-2");
        verify(contentMapper, never()).toDto(document3);
    }

    @Test
    @DisplayName("정렬 기준이 createdAt이 아니면 정렬값을 그대로 문자열 커서로 사용한다")
    void search_hasNextPage_withWatcherCount_usesRawSortValueAsCursor() {
        // given
        ContentCursorRequest request = new ContentCursorRequest(
                null, "영화", null, null, null,
                1, Sort.Direction.DESC, ContentSortByType.WATCHER_COUNT
        );
        ContentDocument document1 = contentDocument("id-1", "영화1");
        ContentDocument document2 = contentDocument("id-2", "영화2");

        @SuppressWarnings("unchecked")
        SearchHits<ContentDocument> hits = mock(SearchHits.class);
        when(hits.getSearchHits()).thenReturn(List.of(
                hitOf(document1, 100L, "id-1"),
                hitOf(document2, 80L, "id-2")
        ));
        when(hits.getTotalHits()).thenReturn(2L);
        when(operations.search(any(Query.class), eq(ContentDocument.class))).thenReturn(hits);
        when(contentMapper.toDto(document1)).thenReturn(contentResponse("영화1"));

        // when
        CursorResponse<ContentResponse> result = contentSearchFinder.search(request);

        // then
        assertThat(result.hasNext()).isTrue();
        assertThat(result.nextCursor()).isEqualTo("100");
        assertThat(result.nextIdAfter()).isEqualTo("id-1");
    }

    @Test
    @DisplayName("커서가 있으면 search_after 조건으로 다음 페이지를 조회한다")
    void search_buildsSearchAfter_whenCursorProvided() {
        // given
        ContentCursorRequest request = new ContentCursorRequest(
                null, "영화", null, "2026-07-01T00:00:00Z", "id-1",
                2, Sort.Direction.DESC, ContentSortByType.CREATED_AT
        );
        @SuppressWarnings("unchecked")
        SearchHits<ContentDocument> hits = mock(SearchHits.class);
        when(hits.getSearchHits()).thenReturn(List.of());
        when(hits.getTotalHits()).thenReturn(0L);
        when(operations.search(any(Query.class), eq(ContentDocument.class))).thenReturn(hits);

        // when
        contentSearchFinder.search(request);

        // then
        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        verify(operations).search(queryCaptor.capture(), eq(ContentDocument.class));
        Query query = queryCaptor.getValue();
        assertThat(query.getSearchAfter()).containsExactly("2026-07-01T00:00:00Z", "id-1");
        assertThat(query.getMaxResults()).isEqualTo(3);
    }

    private ContentDocument contentDocument(String id, String title) {
        return ContentDocument.builder()
                .id(id)
                .contentId(id)
                .type(ContentType.MOVIE)
                .title(title)
                .build();
    }

    private ContentResponse contentResponse(String title) {
        return new ContentResponse(UUID.randomUUID(), ContentType.MOVIE, title, null, null, List.of(), 0.0, 0, 0);
    }

    private SearchHit<ContentDocument> hitOf(ContentDocument document, Object... sortValues) {
        return new SearchHit<>(null, document.getId(), null, 0f, sortValues,
                Map.of(), Map.of(), null, null, List.of(), document);
    }
}
