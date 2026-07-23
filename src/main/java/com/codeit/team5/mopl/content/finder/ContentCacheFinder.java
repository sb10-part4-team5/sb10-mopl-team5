package com.codeit.team5.mopl.content.finder;

import static com.codeit.team5.mopl.global.infra.redis.config.RedisCacheConfig.CONTENT_LIST_CACHE;

import com.codeit.team5.mopl.content.dto.request.ContentCursorRequest;
import com.codeit.team5.mopl.content.dto.response.ContentResponse;
import com.codeit.team5.mopl.content.entity.Content;
import com.codeit.team5.mopl.content.entity.ContentSortByType;
import com.codeit.team5.mopl.content.mapper.ContentMapper;
import com.codeit.team5.mopl.content.repository.ContentRepository;
import com.codeit.team5.mopl.global.dto.CursorResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ContentCacheFinder {

    public static final int FIRST_PAGE_LIMIT = 20;

    private final ContentRepository contentRepository;
    private final ContentMapper contentMapper;

    @Transactional(readOnly = true)
    @Cacheable(value = CONTENT_LIST_CACHE, key = "#sortBy + ':' + #sortDirection", sync = true)
    public CursorResponse<ContentResponse> getFirstPage(ContentSortByType sortBy, Sort.Direction sortDirection) {
        ContentCursorRequest request = new ContentCursorRequest(
                null, null, null, null, null, FIRST_PAGE_LIMIT, sortDirection, sortBy);
        int fetchLimit = FIRST_PAGE_LIMIT + 1;
        List<Content> fetched = contentRepository.findContents(request, fetchLimit);
        boolean hasNext = fetched.size() > FIRST_PAGE_LIMIT;
        List<Content> page = hasNext ? fetched.subList(0, FIRST_PAGE_LIMIT) : fetched;
        long totalCount = contentRepository.countContents(request);
        return contentMapper.toCursor(page, hasNext, totalCount, sortBy, sortDirection);
    }
}
