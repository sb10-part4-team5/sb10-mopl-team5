package com.codeit.team5.mopl.content.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.codeit.team5.mopl.binarycontent.entity.BinaryContent;
import com.codeit.team5.mopl.content.dto.request.ContentCursorRequest;
import com.codeit.team5.mopl.content.entity.Content;
import com.codeit.team5.mopl.content.entity.ContentSortByType;
import com.codeit.team5.mopl.content.entity.ContentSource;
import com.codeit.team5.mopl.content.entity.ContentStats;
import com.codeit.team5.mopl.content.entity.ContentTag;
import com.codeit.team5.mopl.content.entity.ContentType;
import com.codeit.team5.mopl.global.support.base.BaseRepositoryTest;
import com.codeit.team5.mopl.tag.entity.Tag;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

class ContentRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private ContentRepository contentRepository;

    @BeforeEach
    void setUp() {
        clear();
    }

    // --- findWithStatsAndTagsById (EntityGraph) ---

    @Test
    @DisplayName("stats, contentTags, tag를 1번 쿼리로 fetch join 조회_성공")
    void findWithStatsAndTagsById_성공() {
        // given
        Content content = createContent();
        ContentStats stats = persistAndFlush(ContentStats.create(content));
        content.attachStats(stats);

        Tag tag1 = persistAndFlush(Tag.create("액션"));
        Tag tag2 = persistAndFlush(Tag.create("드라마"));
        content.addTag(ContentTag.create(content, tag1));
        content.addTag(ContentTag.create(content, tag2));
        flush();
        clear();

        // when
        Optional<Content> result = contentRepository.findWithStatsAndTagsById(content.getId());

        // then
        assertThat(result).isPresent();
        Content found = result.get();
        assertThat(found.getStats()).isNotNull();
        assertThat(found.getContentTags()).hasSize(2);
        assertThat(found.getContentTags())
                .extracting(ct -> ct.getTag().getName())
                .containsExactlyInAnyOrder("액션", "드라마");
        ensureQueryCount(1);
    }

    @Test
    @DisplayName("썸네일이 있을 때 썸네일도 1번 쿼리로 함께 조회_성공")
    void findWithStatsAndTagsById_썸네일포함_성공() {
        // given
        Content content = createContent();
        ContentStats stats = persistAndFlush(ContentStats.create(content));
        content.attachStats(stats);

        BinaryContent thumbnail = persistAndFlush(BinaryContent.completed("https://example.com/thumb.jpg"));
        content.attachThumbnail(thumbnail);
        flush();
        clear();

        // when
        Optional<Content> result = contentRepository.findWithStatsAndTagsById(content.getId());

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getThumbnail()).isNotNull();
        assertThat(result.get().getThumbnail().getUrl()).isEqualTo("https://example.com/thumb.jpg");
        ensureQueryCount(1);
    }

    @Test
    @DisplayName("태그가 없는 콘텐츠도 정상 조회_성공")
    void findWithStatsAndTagsById_태그없음_성공() {
        // given
        Content content = createContent();
        ContentStats stats = persistAndFlush(ContentStats.create(content));
        content.attachStats(stats);
        flush();
        clear();

        // when
        Optional<Content> result = contentRepository.findWithStatsAndTagsById(content.getId());

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getContentTags()).isEmpty();
        ensureQueryCount(1);
    }

    @Test
    @DisplayName("존재하지 않는 ID로 조회하면 빈 Optional 반환")
    void findWithStatsAndTagsById_NotFound() {
        // given
        UUID randomId = UUID.randomUUID();

        // when
        Optional<Content> result = contentRepository.findWithStatsAndTagsById(randomId);

        // then
        assertThat(result).isEmpty();
        ensureQueryCount(1);
    }

    // --- findContents / countContents (QueryDSL: 필터·정렬·커서) ---

    @Test
    @DisplayName("typeEqual 필터는 해당 타입의 콘텐츠만 조회한다")
    void findContents_filterByType() {
        // given
        persistContent(ContentType.MOVIE, "영화1", null);
        persistContent(ContentType.MOVIE, "영화2", null);
        persistContent(ContentType.TV_SERIES, "드라마1", null);
        clear();

        // when
        List<Content> result = contentRepository.findContents(
                request(ContentType.MOVIE, null, null, null, null, Sort.Direction.DESC, ContentSortByType.CREATED_AT), 10);

        // then
        assertThat(result).hasSize(2)
                .extracting(Content::getType)
                .containsOnly(ContentType.MOVIE);
    }

    @Test
    @DisplayName("keywordLike 필터는 제목 또는 설명에 키워드가 포함된 콘텐츠를 조회한다")
    void findContents_filterByKeyword() {
        // given
        persistContent(ContentType.MOVIE, "어벤져스", "히어로 영화");
        persistContent(ContentType.MOVIE, "인터스텔라", "우주를 다룬 어벤져스급 대작"); // 설명에 키워드
        persistContent(ContentType.MOVIE, "기생충", "가족 드라마");
        clear();

        // when
        List<Content> result = contentRepository.findContents(
                request(null, "어벤져스", null, null, null, Sort.Direction.DESC, ContentSortByType.CREATED_AT), 10);

        // then - 제목 매칭 1건 + 설명 매칭 1건
        assertThat(result).hasSize(2)
                .extracting(Content::getTitle)
                .containsExactlyInAnyOrder("어벤져스", "인터스텔라");
    }

    @Test
    @DisplayName("tagsIn 필터는 해당 태그를 가진 콘텐츠만 조회한다")
    void findContents_filterByTagsIn() {
        // given
        Tag action = persistAndFlush(Tag.create("액션"));
        Tag romance = persistAndFlush(Tag.create("로맨스"));
        Content movie1 = persistContent(ContentType.MOVIE, "액션영화", null);
        Content movie2 = persistContent(ContentType.MOVIE, "로맨스영화", null);
        attachTag(movie1, action);
        attachTag(movie2, romance);
        clear();

        // when
        List<Content> result = contentRepository.findContents(
                request(null, null, List.of("액션"), null, null, Sort.Direction.DESC, ContentSortByType.CREATED_AT), 10);

        // then
        assertThat(result).hasSize(1)
                .extracting(Content::getTitle)
                .containsExactly("액션영화");
    }

    @Test
    @DisplayName("tagsIn 필터는 대소문자·공백을 정규화하여 매칭한다")
    void findContents_filterByTagsIn_normalized() {
        // given
        Tag sf = persistAndFlush(Tag.create("sf"));
        Content movie = persistContent(ContentType.MOVIE, "SF영화", null);
        attachTag(movie, sf);
        clear();

        // when - 대문자·공백이 포함된 요청도 정규화되어 매칭되어야 한다
        List<Content> result = contentRepository.findContents(
                request(null, null, List.of("  SF  "), null, null, Sort.Direction.DESC, ContentSortByType.CREATED_AT), 10);

        // then
        assertThat(result).hasSize(1)
                .extracting(Content::getTitle)
                .containsExactly("SF영화");
    }

    @Test
    @DisplayName("여러 필터를 함께 적용하면 모든 조건을 만족하는 콘텐츠만 조회한다")
    void findContents_combinedFilters() {
        // given
        persistContent(ContentType.MOVIE, "어벤져스", null);
        persistContent(ContentType.TV_SERIES, "어벤져스 시리즈", null); // 키워드는 맞지만 타입 불일치
        persistContent(ContentType.MOVIE, "기생충", null);            // 타입은 맞지만 키워드 불일치
        clear();

        // when
        List<Content> result = contentRepository.findContents(
                request(ContentType.MOVIE, "어벤져스", null, null, null, Sort.Direction.DESC, ContentSortByType.CREATED_AT), 10);

        // then
        assertThat(result).hasSize(1)
                .extracting(Content::getTitle)
                .containsExactly("어벤져스");
    }

    @Test
    @DisplayName("countContents는 필터 조건에 맞는 콘텐츠 개수를 반환한다")
    void countContents_withFilter() {
        // given
        persistContent(ContentType.MOVIE, "영화1", null);
        persistContent(ContentType.MOVIE, "영화2", null);
        persistContent(ContentType.TV_SERIES, "드라마1", null);
        clear();

        // when
        long movieCount = contentRepository.countContents(
                request(ContentType.MOVIE, null, null, null, null, Sort.Direction.DESC, ContentSortByType.CREATED_AT));
        long totalCount = contentRepository.countContents(
                request(null, null, null, null, null, Sort.Direction.DESC, ContentSortByType.CREATED_AT));

        // then
        assertThat(movieCount).isEqualTo(2);
        assertThat(totalCount).isEqualTo(3);
    }

    @Test
    @DisplayName("createdAt DESC 커서로 콘텐츠를 중복·누락 없이 이어서 조회한다")
    void findContents_createdAtDescCursor() {
        // given
        persistContent(ContentType.MOVIE, "콘텐츠A", null);
        persistContent(ContentType.MOVIE, "콘텐츠B", null);
        persistContent(ContentType.MOVIE, "콘텐츠C", null);
        clear();

        ContentCursorRequest firstRequest =
                request(null, null, null, null, null, Sort.Direction.DESC, ContentSortByType.CREATED_AT);

        // when
        List<Content> expectedOrder = contentRepository.findContents(firstRequest, 10);
        List<Content> firstPage = contentRepository.findContents(firstRequest, 1);
        Content last = firstPage.get(0);
        ContentCursorRequest secondRequest = request(
                null, null, null,
                last.getCreatedAt().toString(), last.getId().toString(),
                Sort.Direction.DESC, ContentSortByType.CREATED_AT);
        List<Content> secondPage = contentRepository.findContents(secondRequest, 10);

        // then
        assertPagination(expectedOrder, firstPage, secondPage, 3);
    }

    @Test
    @DisplayName("createdAt ASC 정렬은 DESC 정렬의 정확한 역순으로 조회한다")
    void findContents_createdAtAsc() {
        // given
        persistContent(ContentType.MOVIE, "콘텐츠A", null);
        persistContent(ContentType.MOVIE, "콘텐츠B", null);
        persistContent(ContentType.MOVIE, "콘텐츠C", null);
        clear();

        // when
        List<Content> desc = contentRepository.findContents(
                request(null, null, null, null, null, Sort.Direction.DESC, ContentSortByType.CREATED_AT), 10);
        List<Content> asc = contentRepository.findContents(
                request(null, null, null, null, null, Sort.Direction.ASC, ContentSortByType.CREATED_AT), 10);

        // then - (createdAt, id) 동일 정렬 기준에서 방향만 반대이므로 ASC는 DESC의 정확한 역순이다
        List<UUID> reversedDescIds = new ArrayList<>(desc.stream().map(Content::getId).toList());
        Collections.reverse(reversedDescIds);
        assertThat(asc)
                .extracting(Content::getId)
                .containsExactlyElementsOf(reversedDescIds);
    }

    @Test
    @DisplayName("watcherCount DESC 커서로 콘텐츠를 중복·누락 없이 이어서 조회한다")
    void findContents_watcherCountDescCursor() {
        // given
        persistContentWithWatcher("시청자10", 10L);
        persistContentWithWatcher("시청자30", 30L);
        persistContentWithWatcher("시청자20", 20L);
        clear();

        ContentCursorRequest firstRequest =
                request(null, null, null, null, null, Sort.Direction.DESC, ContentSortByType.WATCHER_COUNT);

        // when
        List<Content> expectedOrder = contentRepository.findContents(firstRequest, 10);
        List<Content> firstPage = contentRepository.findContents(firstRequest, 1);
        Content last = firstPage.get(0);
        ContentCursorRequest secondRequest = request(
                null, null, null,
                String.valueOf(last.getStats().getWatcherCount()), last.getId().toString(),
                Sort.Direction.DESC, ContentSortByType.WATCHER_COUNT);
        List<Content> secondPage = contentRepository.findContents(secondRequest, 10);

        // then - watcherCount 내림차순(30, 20, 10)
        assertThat(expectedOrder)
                .extracting(c -> c.getStats().getWatcherCount())
                .containsExactly(30L, 20L, 10L);
        assertPagination(expectedOrder, firstPage, secondPage, 3);
    }

    @Test
    @DisplayName("watcherCount가 동률인 경우 id를 2차 정렬 키로 삼아 중복·누락 없이 이어서 조회한다")
    void findContents_watcherCountDescCursor_withTie() {
        // given - 두 콘텐츠가 동일한 watcherCount(20)를 가진다
        persistContentWithWatcher("동률A", 20L);
        persistContentWithWatcher("동률B", 20L);
        persistContentWithWatcher("단독", 10L);
        clear();

        ContentCursorRequest firstRequest =
                request(null, null, null, null, null, Sort.Direction.DESC, ContentSortByType.WATCHER_COUNT);

        // when - 1페이지 마지막 행이 동률 값(20)을 커서로 넘긴다
        List<Content> expectedOrder = contentRepository.findContents(firstRequest, 10);
        List<Content> firstPage = contentRepository.findContents(firstRequest, 1);
        Content last = firstPage.get(0);
        ContentCursorRequest secondRequest = request(
                null, null, null,
                String.valueOf(last.getStats().getWatcherCount()), last.getId().toString(),
                Sort.Direction.DESC, ContentSortByType.WATCHER_COUNT);
        List<Content> secondPage = contentRepository.findContents(secondRequest, 10);

        // then - watcherCount가 같은 나머지 한 행이 누락·중복 없이 다음 페이지에 나와야 한다
        assertThat(expectedOrder)
                .extracting(c -> c.getStats().getWatcherCount())
                .containsExactly(20L, 20L, 10L);
        assertPagination(expectedOrder, firstPage, secondPage, 3);
    }

    @Test
    @DisplayName("rate(평균 평점) DESC 커서로 콘텐츠를 중복·누락 없이 이어서 조회한다")
    void findContents_rateDescCursor() {
        // given - 평균 평점: 2.0, 4.5, 3.0
        persistContentWithRating("평점2.0", 4.0, 2);
        persistContentWithRating("평점4.5", 9.0, 2);
        persistContentWithRating("평점3.0", 9.0, 3);
        clear();

        ContentCursorRequest firstRequest =
                request(null, null, null, null, null, Sort.Direction.DESC, ContentSortByType.RATE);

        // when
        List<Content> expectedOrder = contentRepository.findContents(firstRequest, 10);
        List<Content> firstPage = contentRepository.findContents(firstRequest, 1);
        Content last = firstPage.get(0);
        ContentCursorRequest secondRequest = request(
                null, null, null,
                String.valueOf(last.getStats().getAverageRating()), last.getId().toString(),
                Sort.Direction.DESC, ContentSortByType.RATE);
        List<Content> secondPage = contentRepository.findContents(secondRequest, 10);

        // then - 평균 평점 내림차순(4.5, 3.0, 2.0)
        assertThat(expectedOrder)
                .extracting(c -> c.getStats().getAverageRating())
                .containsExactly(4.5, 3.0, 2.0);
        assertPagination(expectedOrder, firstPage, secondPage, 3);
    }

    @Test
    @DisplayName("rate가 동률인 경우 id를 2차 정렬 키로 삼아 중복·누락 없이 이어서 조회한다")
    void findContents_rateDescCursor_withTie() {
        // given - 최상위 두 콘텐츠가 동일한 평균 평점(4.0)을 가진다 (ratingSum/reviewCount 조합은 다름)
        // 1페이지(size=1)의 커서가 동률 값이 되어야 tie-break 분기를 실제로 검증할 수 있다
        persistContentWithRating("동률A", 8.0, 2);
        persistContentWithRating("동률B", 12.0, 3);
        persistContentWithRating("단독", 4.0, 2);
        clear();

        ContentCursorRequest firstRequest =
                request(null, null, null, null, null, Sort.Direction.DESC, ContentSortByType.RATE);

        // when - 1페이지 마지막 행이 동률 값(4.0)을 커서로 넘긴다
        List<Content> expectedOrder = contentRepository.findContents(firstRequest, 10);
        List<Content> firstPage = contentRepository.findContents(firstRequest, 1);
        Content last = firstPage.get(0);
        ContentCursorRequest secondRequest = request(
                null, null, null,
                String.valueOf(last.getStats().getAverageRating()), last.getId().toString(),
                Sort.Direction.DESC, ContentSortByType.RATE);
        List<Content> secondPage = contentRepository.findContents(secondRequest, 10);

        // then - 평균 평점이 같은 나머지 한 행이 누락·중복 없이 다음 페이지에 나와야 한다
        assertThat(expectedOrder)
                .extracting(c -> c.getStats().getAverageRating())
                .containsExactly(4.0, 4.0, 2.0);
        assertPagination(expectedOrder, firstPage, secondPage, 3);
    }

    // --- 헬퍼 ---

    private Content createContent() {
        Content content = Content.createByExternalSource(
                ContentType.MOVIE,
                "테스트 콘텐츠",
                null,
                ContentSource.TMDB,
                "ext-id-" + UUID.randomUUID(),
                null,
                null
        );
        return persistAndFlush(content);
    }

    private Content persistContent(ContentType type, String title, String description) {
        Content content = persistAndFlush(Content.createByAdmin(type, title, description));
        ContentStats stats = persistAndFlush(ContentStats.create(content));
        content.attachStats(stats);
        return content;
    }

    private Content persistContentWithWatcher(String title, long watcherCount) {
        Content content = persistAndFlush(Content.createByAdmin(ContentType.MOVIE, title, null));
        ContentStats stats = ContentStats.create(content);
        ReflectionTestUtils.setField(stats, "watcherCount", watcherCount);
        persistAndFlush(stats);
        content.attachStats(stats);
        return content;
    }

    private Content persistContentWithRating(String title, double ratingSum, int reviewCount) {
        Content content = persistAndFlush(Content.createByAdmin(ContentType.MOVIE, title, null));
        ContentStats stats = ContentStats.create(content);
        stats.updateRating(ratingSum, reviewCount);
        persistAndFlush(stats);
        content.attachStats(stats);
        return content;
    }

    private void attachTag(Content content, Tag tag) {
        ContentTag contentTag = ContentTag.create(content, tag);
        content.addTag(contentTag);
        persistAndFlush(contentTag);
    }

    private ContentCursorRequest request(
            ContentType typeEqual, String keywordLike, List<String> tagsIn,
            String cursor, String idAfter, Sort.Direction direction, ContentSortByType sortBy) {
        return new ContentCursorRequest(typeEqual, keywordLike, tagsIn, cursor, idAfter, 10, direction, sortBy);
    }

    /**
     * 1페이지(size=1) + 2페이지(커서)를 합치면 전체 순서와 정확히 일치하고,
     * 두 페이지 간 중복이 없어야 함을 검증한다.
     */
    private void assertPagination(List<Content> expectedOrder, List<Content> firstPage,
            List<Content> secondPage, int totalSize) {
        assertThat(expectedOrder).hasSize(totalSize);

        List<Content> combined = new ArrayList<>(firstPage);
        combined.addAll(secondPage);
        assertThat(combined)
                .extracting(Content::getId)
                .containsExactlyElementsOf(expectedOrder.stream().map(Content::getId).toList());
        assertThat(firstPage)
                .extracting(Content::getId)
                .doesNotContainAnyElementsOf(secondPage.stream().map(Content::getId).toList());
        assertThat(firstPage).containsExactly(expectedOrder.get(0));
        assertThat(secondPage.get(0)).isEqualTo(expectedOrder.get(1));
    }
}
