package com.codeit.team5.mopl.content.batch.writer;

import static org.assertj.core.api.Assertions.assertThat;
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
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
        given(contentRepository.saveAll(anyList())).willReturn(List.of(content1, content2));
        given(contentStatsRepository.saveAll(anyList())).willReturn(List.of());

        // when
        writer.write(new Chunk<>(List.of(item1, item2)));

        // then
        verify(contentRepository).saveAll(anyList());
        verify(contentStatsRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("청크 내 externalId 중복이 있으면 첫 번째 항목만 저장한다")
    void write_deduplicatesChunkByExternalId() throws Exception {
        // given
        Content content1 = Content.createByExternalSource(ContentType.MOVIE, "영화1", "desc",
                ContentSource.TMDB, "1", null, "{}");
        Content content2 = Content.createByExternalSource(ContentType.MOVIE, "영화1-중복", "desc",
                ContentSource.TMDB, "1", null, "{}");
        ContentWithMetaData item1 = new ContentWithMetaData(content1, null, List.of());
        ContentWithMetaData item2 = new ContentWithMetaData(content2, null, List.of());
        given(contentRepository.saveAll(anyList())).willAnswer(invocation -> invocation.getArgument(0));
        given(contentStatsRepository.saveAll(anyList())).willReturn(List.of());

        // when
        writer.write(new Chunk<>(List.of(item1, item2)));

        // then
        verify(contentRepository).saveAll(List.of(content1));
    }

    @Test
    @DisplayName("썸네일 URL이 있으면 saveAll로 일괄 저장 후 content에 연결한다")
    void write_savesThumbnailsAndAttachesToContent() throws Exception {
        // given
        Content content = Content.createByExternalSource(ContentType.MOVIE, "영화1", "desc",
                ContentSource.TMDB, "1", null, "{}");
        ContentWithMetaData item = new ContentWithMetaData(content, "https://img.example.com/poster.jpg", List.of());
        BinaryContent savedThumbnail = BinaryContent.externalUrl("https://img.example.com/poster.jpg");
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
        BinaryContent savedThumbnail1 = BinaryContent.externalUrl(sharedUrl);
        BinaryContent savedThumbnail2 = BinaryContent.externalUrl(sharedUrl);
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
        given(contentRepository.saveAll(anyList())).willReturn(List.of(content));
        given(contentStatsRepository.saveAll(anyList())).willReturn(List.of());
        given(tagRepository.findByNameIn(List.of("액션"))).willReturn(List.of(existingTag));

        // when
        writer.write(new Chunk<>(List.of(item)));

        // then
        verify(tagRepository, never()).saveAll(anyList());
        assertThat(content.getContentTags())
                .extracting(ContentTag::getTag)
                .containsExactly(existingTag);
    }

    @Test
    @DisplayName("존재하지 않는 태그면 새로 생성하여 저장한다")
    void write_newTag_createsAndSaves() throws Exception {
        // given
        Content content = Content.createByExternalSource(ContentType.MOVIE, "영화1", "desc",
                ContentSource.TMDB, "1", null, "{}");
        ReflectionTestUtils.setField(content, "id", UUID.randomUUID());
        ContentWithMetaData item = new ContentWithMetaData(content, null, List.of("드라마"));
        given(contentRepository.saveAll(anyList())).willReturn(List.of(content));
        given(contentStatsRepository.saveAll(anyList())).willReturn(List.of());
        given(tagRepository.findByNameIn(List.of("드라마"))).willReturn(List.of());
        given(tagRepository.saveAll(anyList())).willAnswer(invocation -> {
            List<Tag> newTags = invocation.getArgument(0);
            newTags.forEach(tag -> ReflectionTestUtils.setField(tag, "id", UUID.randomUUID()));
            return newTags;
        });

        // when
        writer.write(new Chunk<>(List.of(item)));

        // then
        ArgumentCaptor<List<Tag>> captor = ArgumentCaptor.forClass(List.class);
        verify(tagRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).extracting(Tag::getName).containsExactly("드라마");
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
        given(contentRepository.saveAll(anyList())).willReturn(List.of(content));
        given(contentStatsRepository.saveAll(anyList())).willReturn(List.of());
        given(tagRepository.findByNameIn(List.of("액션", "드라마"))).willReturn(List.of(existingTag));
        given(tagRepository.saveAll(anyList())).willAnswer(invocation -> {
            List<Tag> newTags = invocation.getArgument(0);
            newTags.forEach(tag -> ReflectionTestUtils.setField(tag, "id", UUID.randomUUID()));
            return newTags;
        });

        // when
        writer.write(new Chunk<>(List.of(item)));

        // then
        assertThat(content.getContentTags())
                .extracting(ct -> ct.getTag().getName())
                .containsExactlyInAnyOrder("액션", "드라마");
    }
}
