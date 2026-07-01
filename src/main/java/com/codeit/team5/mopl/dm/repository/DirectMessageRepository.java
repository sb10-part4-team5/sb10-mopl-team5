package com.codeit.team5.mopl.dm.repository;

import com.codeit.team5.mopl.dm.entity.DirectMessage;
import com.codeit.team5.mopl.dm.repository.querydsl.DirectMessageQueryRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DirectMessageRepository extends JpaRepository<DirectMessage, UUID>, DirectMessageQueryRepository {

    // 대화별 메시지 최신순 조회 (페이지네이션)
    List<DirectMessage> findByConversationIdOrderByCreatedAtDesc(UUID conversationId, Pageable pageable);

    // 특정 대화에서 내가 받은 안 읽은 메시지 개수
    long countByConversationIdAndReceiverIdAndReadFalse(UUID conversationId, UUID receiverId);

    // 대화의 가장 최근 메시지
    Optional<DirectMessage> findTopByConversationIdOrderByCreatedAtDesc(UUID conversationId);

    // 기준 시점까지 내가 받은 안 읽은 메시지를 일괄 읽음 처리
    @Modifying(clearAutomatically = true)
    @Query("update DirectMessage dm set dm.read = true, dm.readAt = :readAt "
            + "where dm.conversation.id = :conversationId and dm.receiver.id = :receiverId "
            + "and dm.read = false and dm.createdAt <= :until")
    int markAsReadUntil(@Param("conversationId") UUID conversationId,
            @Param("receiverId") UUID receiverId,
            @Param("until") Instant until,
            @Param("readAt") Instant readAt);
}
