package com.codeit.team5.mopl.content.store;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codeit.team5.mopl.content.dto.request.ContentCursorRequest;
import com.codeit.team5.mopl.content.dto.response.ContentResponse;
import com.codeit.team5.mopl.content.entity.Content;
import com.codeit.team5.mopl.content.entity.ContentSortByType;
import com.codeit.team5.mopl.content.entity.ContentType;
import com.codeit.team5.mopl.content.mapper.ContentMapper;
import com.codeit.team5.mopl.content.repository.ContentRepository;
import com.codeit.team5.mopl.global.dto.CursorResponse;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

/**
 * {@code @Cacheable}은 Spring AOP 프록시가 있어야 동작하므로,
 * 여기서는 캐시 미스 시 어떤 쿼리를 던지고 어떻게 매핑하는지(순수 로직)만 검증한다.
 * 실제 캐싱 동작 자체는 Redis가 붙는 통합 테스트에서 확인해야 한다.
 */
@ExtendWith(MockitoExtension.class)
class ContentCacheStoreTest {

    @Mock
    private ContentRepository contentRepository;

    @Mock
    private ContentMapper contentMapper;

    @InjectMocks
    private ContentCacheStore contentCacheStore;

    @Test
    @DisplayName("첫 페이지 조회 시 필터 없이 limit=20 고정 조건으로 DB를 조회한다")
    void getFirstPage_buildsDefaultRequest() {
        // given
        List<Content> fetched = List.of(
                Content.createByAdmin(ContentType.MOVIE, "영화1", null),
                Content.createByAdmin(ContentType.MOVIE, "영화2", null)
        );
        CursorResponse<ContentResponse> expected = new CursorResponse<>(
                List.of(), null, null, false, 2L, "watcherCount", "DESCENDING"
        );

        when(contentRepository.findContents(any(ContentCursorRequest.class), eq(21))).thenReturn(fetched);
        when(contentRepository.countContents(any(ContentCursorRequest.class))).thenReturn(2L);
        when(contentMapper.toCursor(eq(fetched), eq(false), eq(2L),
                eq(ContentSortByType.WATCHER_COUNT), eq(Sort.Direction.DESC)))
                .thenReturn(expected);

        // when
        CursorResponse<ContentResponse> result =
                contentCacheStore.getFirstPage(ContentSortByType.WATCHER_COUNT, Sort.Direction.DESC);

        // then
        assertThat(result).isSameAs(expected);

        ArgumentCaptor<ContentCursorRequest> requestCaptor = ArgumentCaptor.forClass(ContentCursorRequest.class);
        verify(contentRepository).findContents(requestCaptor.capture(), eq(21));
        ContentCursorRequest usedRequest = requestCaptor.getValue();
        assertThat(usedRequest.typeEqual()).isNull();
        assertThat(usedRequest.keywordLike()).isNull();
        assertThat(usedRequest.tagsIn()).isNull();
        assertThat(usedRequest.cursor()).isNull();
        assertThat(usedRequest.idAfter()).isNull();
        assertThat(usedRequest.limit()).isEqualTo(ContentCacheStore.FIRST_PAGE_LIMIT);
        assertThat(usedRequest.sortBy()).isEqualTo(ContentSortByType.WATCHER_COUNT);
        assertThat(usedRequest.sortDirection()).isEqualTo(Sort.Direction.DESC);
    }
}
