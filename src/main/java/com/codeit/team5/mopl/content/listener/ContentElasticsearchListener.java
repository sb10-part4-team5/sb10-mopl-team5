package com.codeit.team5.mopl.content.listener;

import com.codeit.team5.mopl.content.document.ContentDocument;
import com.codeit.team5.mopl.content.entity.Content;
import com.codeit.team5.mopl.content.event.ContentDeletedEvent;
import com.codeit.team5.mopl.content.event.ContentUpsertedEvent;
import com.codeit.team5.mopl.content.mapper.ContentMapper;
import com.codeit.team5.mopl.content.repository.ContentRepository;
import com.codeit.team5.mopl.content.repository.opensearch.ContentDocumentRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class ContentElasticsearchListener {

    private final ContentRepository contentRepository;
    private final ContentDocumentRepository contentDocumentRepository;
    private final ContentMapper contentMapper;

    @Async("outboxEventWorker")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(ContentUpsertedEvent event) {
        if (event.contentIds() == null || event.contentIds().isEmpty()) {
            return;
        }
        List<Content> contents = contentRepository.findAllWithStatsAndTagsByIdIn(event.contentIds());
        List<ContentDocument> documents = contents.stream()
                .map(contentMapper::toDocument)
                .toList();
        if (documents.isEmpty()) {
            log.debug("색인 대상 콘텐츠가 없어 건너뜁니다. contentIds={}", event.contentIds());
            return;
        }
        contentDocumentRepository.saveAll(documents);
    }

    @Async("outboxEventWorker")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(ContentDeletedEvent event) {
        contentDocumentRepository.deleteById(ContentDocument.toDocumentId(event.contentId()));
    }

}
