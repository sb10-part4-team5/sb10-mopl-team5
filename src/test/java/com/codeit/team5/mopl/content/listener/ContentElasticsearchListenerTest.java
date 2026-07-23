package com.codeit.team5.mopl.content.listener;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.codeit.team5.mopl.content.document.ContentDocument;
import com.codeit.team5.mopl.content.entity.Content;
import com.codeit.team5.mopl.content.entity.ContentType;
import com.codeit.team5.mopl.content.event.ContentDeletedEvent;
import com.codeit.team5.mopl.content.event.ContentUpsertedEvent;
import com.codeit.team5.mopl.content.mapper.ContentMapper;
import com.codeit.team5.mopl.content.repository.ContentRepository;
import com.codeit.team5.mopl.content.repository.opensearch.ContentDocumentRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ContentElasticsearchListenerTest {

    @Mock
    private ContentRepository contentRepository;

    @Mock
    private ContentDocumentRepository contentDocumentRepository;

    @Mock
    private ContentMapper contentMapper;

    @InjectMocks
    private ContentElasticsearchListener listener;

    @Test
    @DisplayName("업서트 이벤트를 받으면 대상 콘텐츠를 조회해 문서로 변환 후 색인한다")
    void handle_upsertedEvent_savesDocuments() {
        // given
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        ContentUpsertedEvent event = new ContentUpsertedEvent(List.of(id1, id2));

        Content content1 = Content.createByAdmin(ContentType.MOVIE, "영화1", null);
        Content content2 = Content.createByAdmin(ContentType.MOVIE, "영화2", null);
        List<Content> contents = List.of(content1, content2);

        ContentDocument document1 = ContentDocument.builder().id(id1.toString()).build();
        ContentDocument document2 = ContentDocument.builder().id(id2.toString()).build();

        when(contentRepository.findAllWithStatsAndTagsByIdIn(event.contentIds())).thenReturn(contents);
        when(contentMapper.toDocument(content1)).thenReturn(document1);
        when(contentMapper.toDocument(content2)).thenReturn(document2);

        // when
        listener.handle(event);

        // then
        verify(contentDocumentRepository).saveAll(List.of(document1, document2));
    }

    @Test
    @DisplayName("색인 대상 콘텐츠가 없으면 저장을 시도하지 않고 건너뛴다")
    void handle_upsertedEvent_skipsWhenNoContents() {
        // given
        ContentUpsertedEvent event = new ContentUpsertedEvent(List.of(UUID.randomUUID()));
        when(contentRepository.findAllWithStatsAndTagsByIdIn(event.contentIds())).thenReturn(List.of());

        // when
        listener.handle(event);

        // then
        verifyNoInteractions(contentDocumentRepository);
    }

    @Test
    @DisplayName("contentIds가 비어있으면 조회 자체를 시도하지 않고 건너뛴다")
    void handle_upsertedEvent_skipsWhenContentIdsEmpty() {
        // given
        ContentUpsertedEvent event = new ContentUpsertedEvent(List.of());

        // when
        listener.handle(event);

        // then
        verifyNoInteractions(contentRepository, contentDocumentRepository);
    }

    @Test
    @DisplayName("contentIds가 null이면 조회 자체를 시도하지 않고 건너뛴다")
    void handle_upsertedEvent_skipsWhenContentIdsNull() {
        // given
        ContentUpsertedEvent event = new ContentUpsertedEvent(null);

        // when
        listener.handle(event);

        // then
        verifyNoInteractions(contentRepository, contentDocumentRepository);
    }

    @Test
    @DisplayName("삭제 이벤트를 받으면 해당 문서를 색인에서 제거한다")
    void handle_deletedEvent_deletesDocument() {
        // given
        UUID contentId = UUID.randomUUID();
        ContentDeletedEvent event = new ContentDeletedEvent(contentId);

        // when
        listener.handle(event);

        // then
        verify(contentDocumentRepository).deleteById(ContentDocument.toDocumentId(contentId));
    }
}
