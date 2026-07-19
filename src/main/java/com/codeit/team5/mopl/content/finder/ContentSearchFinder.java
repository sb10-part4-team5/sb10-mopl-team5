package com.codeit.team5.mopl.content.finder;

import com.codeit.team5.mopl.content.document.ContentDocument;
import com.codeit.team5.mopl.content.dto.request.ContentCursorRequest;
import com.codeit.team5.mopl.content.dto.response.ContentResponse;
import com.codeit.team5.mopl.content.entity.ContentSortByType;
import com.codeit.team5.mopl.content.mapper.ContentMapper;
import com.codeit.team5.mopl.global.dto.CursorResponse;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.opensearch.data.client.orhlc.NativeSearchQueryBuilder;
import org.opensearch.index.query.BoolQueryBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.search.sort.SortBuilders;
import org.opensearch.search.sort.SortOrder;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 키워드 검색을 OpenSearch에서 조회한다. 매칭·필터·정렬·커서를 전부 처리하고
 * {@link ContentResponse}로 바로 반환한다.
 */
@Component
@RequiredArgsConstructor
public class ContentSearchFinder {

    private static final String TIEBREAKER_FIELD = "contentId";

    private final ElasticsearchOperations operations;
    private final ContentMapper contentMapper;

    public CursorResponse<ContentResponse> search(ContentCursorRequest request) {
        SortOrder order = request.sortDirection() == Direction.ASC ? SortOrder.ASC : SortOrder.DESC;
        String sortField = toDocumentField(request.sortBy());

        NativeSearchQueryBuilder builder = new NativeSearchQueryBuilder()
                .withQuery(buildQuery(request))
                .withSorts(
                        SortBuilders.fieldSort(sortField).order(order),
                        SortBuilders.fieldSort(TIEBREAKER_FIELD).order(order))
                .withMaxResults(request.limit() + 1);

        List<Object> searchAfter = toSearchAfter(request);
        if (searchAfter != null) {
            builder.withSearchAfter(searchAfter);
        }

        SearchHits<ContentDocument> hits = operations.search(builder.build(), ContentDocument.class);

        List<SearchHit<ContentDocument>> hitList = hits.getSearchHits();
        boolean hasNext = hitList.size() > request.limit();
        List<SearchHit<ContentDocument>> page = hasNext ? hitList.subList(0, request.limit()) : hitList;

        List<ContentResponse> data = page.stream()
                .map(hit -> contentMapper.toDto(hit.getContent()))
                .toList();

        String nextCursor = null;
        String nextIdAfter = null;
        if (hasNext && !page.isEmpty()) {
            List<Object> sortValues = page.get(page.size() - 1).getSortValues();
            nextCursor = toCursorValue(request.sortBy(), sortValues.get(0));
            nextIdAfter = String.valueOf(sortValues.get(1));
        }

        String direction = request.sortDirection() == Direction.ASC ? "ASCENDING" : "DESCENDING";
        return new CursorResponse<>(data, nextCursor, nextIdAfter, hasNext, hits.getTotalHits(),
                request.sortBy().getValue(), direction);
    }

    private BoolQueryBuilder buildQuery(ContentCursorRequest request) {
        BoolQueryBuilder query = QueryBuilders.boolQuery();

        if (StringUtils.hasText(request.keywordLike())) {
            query.must(QueryBuilders.multiMatchQuery(request.keywordLike(), "title", "description"));
        } else {
            query.must(QueryBuilders.matchAllQuery());
        }
        if (request.typeEqual() != null) {
            query.filter(QueryBuilders.termQuery("type", request.typeEqual().name()));
        }
        if (request.tagsIn() != null && !request.tagsIn().isEmpty()) {
            query.filter(QueryBuilders.termsQuery("tags", normalizeTags(request.tagsIn())));
        }
        return query;
    }

    private List<String> normalizeTags(List<String> tags) {
        return tags.stream()
                .map(String::trim)
                .filter(tag -> !tag.isEmpty())
                .map(tag -> tag.toLowerCase(Locale.ROOT))
                .toList();
    }

    private List<Object> toSearchAfter(ContentCursorRequest request) {
        if (request.cursor() == null || request.idAfter() == null) {
            return null;
        }
        return List.of(request.cursor(), request.idAfter());
    }

    private String toDocumentField(ContentSortByType sortBy) {
        return switch (sortBy) {
            case CREATED_AT -> "createdAt";
            case WATCHER_COUNT -> "watcherCount";
            case RATE -> "averageRating";
        };
    }

    // ES는 date 정렬값을 epoch millis로 반환하므로, RDB 경로의 커서 포맷(ISO)에 맞춰 변환한다.
    private String toCursorValue(ContentSortByType sortBy, Object sortValue) {
        if (sortBy == ContentSortByType.CREATED_AT) {
            return Instant.ofEpochMilli(((Number) sortValue).longValue()).toString();
        }
        return String.valueOf(sortValue);
    }
}
