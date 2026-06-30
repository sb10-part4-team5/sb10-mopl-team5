package com.codeit.team5.mopl.dm.repository;

import com.codeit.team5.mopl.dm.entity.DirectMessage;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DirectMessageRepository extends JpaRepository<DirectMessage, UUID> {

    // 대화별 메시지 최신순 조회 (페이지네이션)
    List<DirectMessage> findByConversationIdOrderByCreatedAtDesc(UUID conversationId, Pageable pageable);

    // 특정 대화에서 내가 받은 안 읽은 메시지 개수
    long countByConversationIdAndReceiverIdAndReadFalse(UUID conversationId, UUID receiverId);

    // 대화의 가장 최근 메시지
    Optional<DirectMessage> findTopByConversationIdOrderByCreatedAtDesc(UUID conversationId);
}
