package com.codeit.team5.mopl.content.batch.writer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.codeit.team5.mopl.binarycontent.entity.BinaryContent;
import com.codeit.team5.mopl.binarycontent.repository.BinaryContentRepository;
import com.codeit.team5.mopl.content.batch.dto.ContentWithMetaData;
import com.codeit.team5.mopl.content.entity.Content;
import com.codeit.team5.mopl.content.entity.ContentSource;
import com.codeit.team5.mopl.content.entity.ContentTag;
import com.codeit.team5.mopl.content.entity.ContentType;
import com.codeit.team5.mopl.content.repository.ContentRepository;
import com.codeit.team5.mopl.content.repository.ContentStatsRepository;
import com.codeit.team5.mopl.tag.entity.Tag;
import com.codeit.team5.mopl.tag.repository.TagRepository;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.item.Chunk;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ContentItemWriterTest {

    @Mock
    private ContentRepository contentRepository;
    @Mock
    private ContentStatsRepository contentStatsRepository;
    @Mock
    private BinaryContentRepository binaryContentRepository;
    @Mock
    private TagRepository tagRepository;

    private ContentItemWriter writer;

    @BeforeEach
    void setUp() {
        writer = new ContentItemWriter(contentRepository, contentStatsRepository,
                binaryContentRepository, tagRepository);
    }

    @Test
    @DisplayName("청크 내 콘텐츠를 saveAll로 일괄 저장한다")
    void write_savesAllContents() throws Exception {
        // given
        Content content1 = Content.createByExternalSource(ContentType.MOVIE, "영화1", "desc",
                ContentSource.TMDB, "1", null, "{}");
        Content content2 = Content.createByExternalSource(ContentType.MOVIE, "영화2", "desc",
                ContentSource.TMDB, "2", null, "{}");
        ContentWithMetaData item1 = new ContentWithMetaData(content1, null, List.of());
        ContentWithMetaData item2 = new ContentWithMetaData(content2, null, List.of());
        given(contentRepository.findExternalIdsBySourceAndExternalIdIn(any(), anyList())).willReturn(Set.of());
        given(contentRepository.saveAll(anyList())).willReturn(List.of(content1, content2));
        given(contentStatsRepository.saveAll(anyList())).willReturn(List.of());

        // when
        writer.write(new Chunk<>(List.of(item1, item2)));

        // then
        verify(contentRepository).saveAll(anyList());
        verify(contentStatsRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("DB에 이미 존재하는 externalId는 저장하지 않는다")
    void write_deduplicatesAgainstDb() throws Exception {
        // given
        Content content1 = Content.createByExternalSource(ContentType.MOVIE, "영화1", "desc",
                ContentSource.TMDB, "1", null, "{}");
        Content content2 = Content.createByExternalSource(ContentType.MOVIE, "영화2", "desc",
                ContentSource.TMDB, "2", null, "{}");
        ContentWithMetaData item1 = new ContentWithMetaData(content1, null, List.of());
        ContentWithMetaData item2 = new ContentWithMetaData(content2, null, List.of());
        given(contentRepository.findExternalIdsBySourceAndExternalIdIn(any(), anyList()))
                .willReturn(Set.of("1")); // content1은 이미 존재
        given(contentRepository.saveAll(anyList())).willAnswer(invocation -> invocation.getArgument(0));
        given(contentStatsRepository.saveAll(anyList())).willReturn(List.of());

        // when
        writer.write(new Chunk<>(List.of(item1, item2)));

        // then
        verify(contentRepository).saveAll(List.of(content2));
    }

    @Test
    @DisplayName("같은 청크에 동일 externalId가 두 건 있으면 먼저 온 항목만 저장한다")
    void write_deduplicatesWithinChunkByExternalId() throws Exception {
        // given
        Content content1 = Content.createByExternalSource(ContentType.MOVIE, "영화1", "desc",
                ContentSource.TMDB, "1", null, "{}");
        Content content1Dup = Content.createByExternalSource(ContentType.MOVIE, "영화1-중복", "desc",
                ContentSource.TMDB, "1", null, "{}");
        ContentWithMetaData item1 = new ContentWithMetaData(content1, null, List.of());
        ContentWithMetaData item1Dup = new ContentWithMetaData(content1Dup, null, List.of());
        given(contentRepository.findExternalIdsBySourceAndExternalIdIn(any(), anyList()))
                .willReturn(Set.of()); // DB에는 없음
        given(contentRepository.saveAll(anyList())).willAnswer(invocation -> invocation.getArgument(0));
        given(contentStatsRepository.saveAll(anyList())).willReturn(List.of());

        // when
        writer.write(new Chunk<>(List.of(item1, item1Dup)));

        // then — 먼저 온 content1만 저장 (유니크 제약 위반 방지)
        verify(contentRepository).saveAll(List.of(content1));
    }

    @Test
    @DisplayName("빈 청크가 전달되면 예외 없이 즉시 반환한다")
    void write_emptyChunk_doesNothing() throws Exception {
        // when
        writer.write(new Chunk<>(List.of()));

        // then
        verify(contentRepository, never()).findExternalIdsBySourceAndExternalIdIn(any(), anyList());
        verify(contentRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("청크 내 모든 항목이 DB에 이미 존재하면 저장을 생략한다")
    void write_allExistingInDb_skipsAllSaves() throws Exception {
        // given
        Content content1 = Content.createByExternalSource(ContentType.MOVIE, "영화1", "desc",
                ContentSource.TMDB, "1", null, "{}");
        Content content2 = Content.createByExternalSource(ContentType.MOVIE, "영화2", "desc",
                ContentSource.TMDB, "2", null, "{}");
        ContentWithMetaData item1 = new ContentWithMetaData(content1, null, List.of());
        ContentWithMetaData item2 = new ContentWithMetaData(content2, null, List.of());
        given(contentRepository.findExternalIdsBySourceAndExternalIdIn(any(), anyList()))
                .willReturn(Set.of("1", "2"));

        // when
        writer.write(new Chunk<>(List.of(item1, item2)));

        // then
        verify(contentRepository, never()).saveAll(anyList());
        verify(contentStatsRepository, never()).saveAll(anyList());
        verify(binaryContentRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("썸네일 URL이 있으면 saveAll로 일괄 저장 후 content에 연결한다")
    void write_savesThumbnailsAndAttachesToContent() throws Exception {
        // given
        Content content = Content.createByExternalSource(ContentType.MOVIE, "영화1", "desc",
                ContentSource.TMDB, "1", null, "{}");
        ContentWithMetaData item = new ContentWithMetaData(content, "https://img.example.com/poster.jpg", List.of());
        BinaryContent savedThumbnail = BinaryContent.completed("https://img.example.com/poster.jpg");
        given(contentRepository.findExternalIdsBySourceAndExternalIdIn(any(), anyList())).willReturn(Set.of());
        given(contentRepository.saveAll(anyList())).willReturn(List.of(content));
        given(contentStatsRepository.saveAll(anyList())).willReturn(List.of());
        given(binaryContentRepository.saveAll(anyList())).willReturn(List.of(savedThumbnail));

        // when
        writer.write(new Chunk<>(List.of(item)));

        // then
        verify(binaryContentRepository).saveAll(anyList());
        assertThat(content.getThumbnail()).isEqualTo(savedThumbnail);
    }

    @Test
    @DisplayName("썸네일 URL이 없으면 binaryContentRepository를 호출하지 않는다")
    void write_skipsThumbnailWhenUrlIsNull() throws Exception {
        // given
        Content content = Content.createByExternalSource(ContentType.MOVIE, "영화1", "desc",
                ContentSource.TMDB, "1", null, "{}");
        ContentWithMetaData item = new ContentWithMetaData(content, null, List.of());
        given(contentRepository.findExternalIdsBySourceAndExternalIdIn(any(), anyList())).willReturn(Set.of());
        given(contentRepository.saveAll(anyList())).willReturn(List.of(content));
        given(contentStatsRepository.saveAll(anyList())).willReturn(List.of());

        // when
        writer.write(new Chunk<>(List.of(item)));

        // then
        verify(binaryContentRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("서로 다른 콘텐츠가 같은 thumbnailUrl을 가지면 각각 별도의 BinaryContent가 연결된다")
    void write_sameThumbnailUrl_attachesSeparateBinaryContentToEach() throws Exception {
        // given
        Content content1 = Content.createByExternalSource(ContentType.MOVIE, "영화1", "desc",
                ContentSource.TMDB, "1", null, "{}");
        Content content2 = Content.createByExternalSource(ContentType.MOVIE, "영화2", "desc",
                ContentSource.TMDB, "2", null, "{}");
        String sharedUrl = "https://img.example.com/default.jpg";
        ContentWithMetaData item1 = new ContentWithMetaData(content1, sharedUrl, List.of());
        ContentWithMetaData item2 = new ContentWithMetaData(content2, sharedUrl, List.of());
        BinaryContent savedThumbnail1 = BinaryContent.completed(sharedUrl);
        BinaryContent savedThumbnail2 = BinaryContent.completed(sharedUrl);
        given(contentRepository.findExternalIdsBySourceAndExternalIdIn(any(), anyList())).willReturn(Set.of());
        given(contentRepository.saveAll(anyList())).willReturn(List.of(content1, content2));
        given(contentStatsRepository.saveAll(anyList())).willReturn(List.of());
        given(binaryContentRepository.saveAll(anyList())).willReturn(List.of(savedThumbnail1, savedThumbnail2));

        // when
        writer.write(new Chunk<>(List.of(item1, item2)));

        // then
        assertThat(content1.getThumbnail()).isEqualTo(savedThumbnail1);
        assertThat(content2.getThumbnail()).isEqualTo(savedThumbnail2);
        assertThat(content1.getThumbnail()).isNotSameAs(content2.getThumbnail());
    }

    @Test
    @DisplayName("이미 존재하는 태그면 재사용하고 새로 생성하지 않는다")
    void write_existingTag_reusesWithoutCreatingNew() throws Exception {
        // given
        Content content = Content.createByExternalSource(ContentType.MOVIE, "영화1", "desc",
                ContentSource.TMDB, "1", null, "{}");
        ReflectionTestUtils.setField(content, "id", UUID.randomUUID());
        ContentWithMetaData item = new ContentWithMetaData(content, null, List.of("액션"));
        Tag existingTag = Tag.create("액션");
        ReflectionTestUtils.setField(existingTag, "id", UUID.randomUUID());
        given(contentRepository.findExternalIdsBySourceAndExternalIdIn(any(), anyList())).willReturn(Set.of());
        given(contentRepository.saveAll(anyList())).willReturn(List.of(content));
        given(contentStatsRepository.saveAll(anyList())).willReturn(List.of());
        given(tagRepository.findOrCreateAllByName(List.of("액션"))).willReturn(Map.of("액션", existingTag));

        // when
        writer.write(new Chunk<>(List.of(item)));

        // then
        assertThat(content.getContentTags())
                .extracting(ContentTag::getTag)
                .containsExactly(existingTag);
    }

    @Test
    @DisplayName("존재하지 않는 태그면 리포지토리가 새로 생성해 돌려준 태그를 연결한다")
    void write_newTag_createsAndSaves() throws Exception {
        // given
        Content content = Content.createByExternalSource(ContentType.MOVIE, "영화1", "desc",
                ContentSource.TMDB, "1", null, "{}");
        ReflectionTestUtils.setField(content, "id", UUID.randomUUID());
        ContentWithMetaData item = new ContentWithMetaData(content, null, List.of("드라마"));
        Tag newDramaTag = Tag.create("드라마");
        ReflectionTestUtils.setField(newDramaTag, "id", UUID.randomUUID());
        given(contentRepository.findExternalIdsBySourceAndExternalIdIn(any(), anyList())).willReturn(Set.of());
        given(contentRepository.saveAll(anyList())).willReturn(List.of(content));
        given(contentStatsRepository.saveAll(anyList())).willReturn(List.of());
        given(tagRepository.findOrCreateAllByName(List.of("드라마"))).willReturn(Map.of("드라마", newDramaTag));

        // when
        writer.write(new Chunk<>(List.of(item)));

        // then
        assertThat(content.getContentTags())
                .extracting(ct -> ct.getTag().getName())
                .containsExactly("드라마");
    }

    @Test
    @DisplayName("기존 태그와 신규 태그가 섞여 있어도 모두 ContentTag로 연결된다")
    void write_mixedExistingAndNewTags_allAttachedAsContentTag() throws Exception {
        // given
        Content content = Content.createByExternalSource(ContentType.MOVIE, "영화1", "desc",
                ContentSource.TMDB, "1", null, "{}");
        ReflectionTestUtils.setField(content, "id", UUID.randomUUID());
        ContentWithMetaData item = new ContentWithMetaData(content, null, List.of("액션", "드라마"));
        Tag existingTag = Tag.create("액션");
        ReflectionTestUtils.setField(existingTag, "id", UUID.randomUUID());
        Tag newDramaTag = Tag.create("드라마");
        ReflectionTestUtils.setField(newDramaTag, "id", UUID.randomUUID());
        given(contentRepository.findExternalIdsBySourceAndExternalIdIn(any(), anyList())).willReturn(Set.of());
        given(contentRepository.saveAll(anyList())).willReturn(List.of(content));
        given(contentStatsRepository.saveAll(anyList())).willReturn(List.of());
        given(tagRepository.findOrCreateAllByName(List.of("액션", "드라마")))
                .willReturn(Map.of("액션", existingTag, "드라마", newDramaTag));

        // when
        writer.write(new Chunk<>(List.of(item)));

        // then
        assertThat(content.getContentTags())
                .extracting(ct -> ct.getTag().getName())
                .containsExactlyInAnyOrder("액션", "드라마");
    }

    @Test
    @DisplayName("서로 다른 콘텐츠가 같은 신규 태그를 참조하면 한 번만 조회/생성 요청되어 공유된다")
    void write_duplicateNewTagAcrossItems_createsOnlyOnce() throws Exception {
        // given
        Content content1 = Content.createByExternalSource(ContentType.MOVIE, "영화1", "desc",
                ContentSource.TMDB, "1", null, "{}");
        Content content2 = Content.createByExternalSource(ContentType.MOVIE, "영화2", "desc",
                ContentSource.TMDB, "2", null, "{}");
        ContentWithMetaData item1 = new ContentWithMetaData(content1, null, List.of("판타지"));
        ContentWithMetaData item2 = new ContentWithMetaData(content2, null, List.of("판타지"));
        Tag newFantasyTag = Tag.create("판타지");
        ReflectionTestUtils.setField(newFantasyTag, "id", UUID.randomUUID());
        given(contentRepository.findExternalIdsBySourceAndExternalIdIn(any(), anyList())).willReturn(Set.of());
        given(contentRepository.saveAll(anyList())).willReturn(List.of(content1, content2));
        given(contentStatsRepository.saveAll(anyList())).willReturn(List.of());
        given(tagRepository.findOrCreateAllByName(List.of("판타지"))).willReturn(Map.of("판타지", newFantasyTag));

        // when
        writer.write(new Chunk<>(List.of(item1, item2)));

        // then
        verify(tagRepository).findOrCreateAllByName(List.of("판타지"));
        assertThat(content1.getContentTags()).extracting(ct -> ct.getTag().getName()).containsExactly("판타지");
        assertThat(content2.getContentTags()).extracting(ct -> ct.getTag().getName()).containsExactly("판타지");
    }

    @Test
    @DisplayName("모든 콘텐츠에 태그가 없으면 태그 저장소를 조회하지 않는다")
    void write_noTagNames_skipsTagRepository() throws Exception {
        // given
        Content content = Content.createByExternalSource(ContentType.MOVIE, "영화1", "desc",
                ContentSource.TMDB, "1", null, "{}");
        ContentWithMetaData item = new ContentWithMetaData(content, null, List.of());
        given(contentRepository.findExternalIdsBySourceAndExternalIdIn(any(), anyList())).willReturn(Set.of());
        given(contentRepository.saveAll(anyList())).willReturn(List.of(content));
        given(contentStatsRepository.saveAll(anyList())).willReturn(List.of());

        // when
        writer.write(new Chunk<>(List.of(item)));

        // then
        verify(tagRepository, never()).findOrCreateAllByName(anyList());
    }

    @Test
    @DisplayName("공백뿐인 태그 이름은 무시되어 연결되지 않는다")
    void write_blankTagNames_ignored() throws Exception {
        // given
        Content content = Content.createByExternalSource(ContentType.MOVIE, "영화1", "desc",
                ContentSource.TMDB, "1", null, "{}");
        ContentWithMetaData item = new ContentWithMetaData(content, null, List.of("   ", ""));
        given(contentRepository.findExternalIdsBySourceAndExternalIdIn(any(), anyList())).willReturn(Set.of());
        given(contentRepository.saveAll(anyList())).willReturn(List.of(content));
        given(contentStatsRepository.saveAll(anyList())).willReturn(List.of());

        // when
        writer.write(new Chunk<>(List.of(item)));

        // then
        verify(tagRepository, never()).findOrCreateAllByName(anyList());
        assertThat(content.getContentTags()).isEmpty();
    }

    @Test
    @DisplayName("태그 이름은 앞뒤 공백 제거 및 소문자로 정규화되어 조회된다")
    void write_tagNamesNormalizedBeforeLookup() throws Exception {
        // given
        Content content = Content.createByExternalSource(ContentType.MOVIE, "영화1", "desc",
                ContentSource.TMDB, "1", null, "{}");
        ContentWithMetaData item = new ContentWithMetaData(content, null, List.of("  Action  "));
        Tag newActionTag = Tag.create("action");
        ReflectionTestUtils.setField(newActionTag, "id", UUID.randomUUID());
        given(contentRepository.findExternalIdsBySourceAndExternalIdIn(any(), anyList())).willReturn(Set.of());
        given(contentRepository.saveAll(anyList())).willReturn(List.of(content));
        given(contentStatsRepository.saveAll(anyList())).willReturn(List.of());
        given(tagRepository.findOrCreateAllByName(List.of("action"))).willReturn(Map.of("action", newActionTag));

        // when
        writer.write(new Chunk<>(List.of(item)));

        // then
        verify(tagRepository).findOrCreateAllByName(List.of("action"));
        assertThat(content.getContentTags()).extracting(ct -> ct.getTag().getName()).containsExactly("action");
    }
}
