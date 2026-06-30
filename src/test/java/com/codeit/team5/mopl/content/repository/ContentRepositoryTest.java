package com.codeit.team5.mopl.content.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.codeit.team5.mopl.binarycontent.entity.BinaryContent;
import com.codeit.team5.mopl.content.entity.Content;
import com.codeit.team5.mopl.content.entity.ContentSource;
import com.codeit.team5.mopl.content.entity.ContentStats;
import com.codeit.team5.mopl.content.entity.ContentTag;
import com.codeit.team5.mopl.content.entity.ContentType;
import com.codeit.team5.mopl.global.support.base.BaseRepositoryTest;
import com.codeit.team5.mopl.tag.entity.Tag;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ContentRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private ContentRepository contentRepository;

    @BeforeEach
    void setUp() {
        clear();
    }

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

        BinaryContent thumbnail = persistAndFlush(BinaryContent.pending("https://example.com/thumb.jpg"));
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
}
