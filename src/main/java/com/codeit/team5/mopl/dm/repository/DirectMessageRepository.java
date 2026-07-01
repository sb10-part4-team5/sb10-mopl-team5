package com.codeit.team5.mopl.dm.repository;

import com.codeit.team5.mopl.dm.entity.DirectMessage;
import com.codeit.team5.mopl.dm.repository.querydsl.DirectMessageQueryRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DirectMessageRepository extends JpaRepository<DirectMessage, UUID>, DirectMessageQueryRepository {

    // 특정 대화에서 내가 받은 안 읽은 메시지 존재 여부
    boolean existsByConversationIdAndReceiverIdAndReadFalse(UUID conversationId, UUID receiverId);

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
