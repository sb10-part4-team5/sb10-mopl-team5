package com.codeit.team5.mopl.content.batch.writer;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.codeit.team5.mopl.binarycontent.repository.BinaryContentRepository;
import com.codeit.team5.mopl.content.batch.dto.ContentWithMetaData;
import com.codeit.team5.mopl.content.entity.Content;
import com.codeit.team5.mopl.content.entity.ContentSource;
import com.codeit.team5.mopl.content.entity.ContentType;
import com.codeit.team5.mopl.content.repository.ContentRepository;
import com.codeit.team5.mopl.content.repository.ContentStatsRepository;
import com.codeit.team5.mopl.tag.repository.TagRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.item.Chunk;

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
        Content content1 = Content.createByExternalSource(ContentType.MOVIE, "영화1", "desc",
                ContentSource.TMDB, "1", null, "{}");
        Content content2 = Content.createByExternalSource(ContentType.MOVIE, "영화2", "desc",
                ContentSource.TMDB, "2", null, "{}");
        ContentWithMetaData item1 = new ContentWithMetaData(content1, null, List.of());
        ContentWithMetaData item2 = new ContentWithMetaData(content2, null, List.of());

        given(contentRepository.saveAll(anyList())).willReturn(List.of(content1, content2));
        given(contentStatsRepository.saveAll(anyList())).willReturn(List.of());

        writer.write(new Chunk<>(List.of(item1, item2)));

        verify(contentRepository).saveAll(anyList());
        verify(contentStatsRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("청크 내 externalId 중복이 있으면 첫 번째 항목만 저장한다")
    void write_deduplicatesChunkByExternalId() throws Exception {
        Content content1 = Content.createByExternalSource(ContentType.MOVIE, "영화1", "desc",
                ContentSource.TMDB, "1", null, "{}");
        Content content2 = Content.createByExternalSource(ContentType.MOVIE, "영화1-중복", "desc",
                ContentSource.TMDB, "1", null, "{}");
        ContentWithMetaData item1 = new ContentWithMetaData(content1, null, List.of());
        ContentWithMetaData item2 = new ContentWithMetaData(content2, null, List.of());

        given(contentRepository.saveAll(anyList())).willAnswer(invocation -> invocation.getArgument(0));
        given(contentStatsRepository.saveAll(anyList())).willReturn(List.of());

        writer.write(new Chunk<>(List.of(item1, item2)));

        verify(contentRepository).saveAll(List.of(content1));
    }
}
