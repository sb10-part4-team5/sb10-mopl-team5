package com.codeit.team5.mopl.dm.repository;

import com.codeit.team5.mopl.dm.entity.Conversation;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    // participant1 < participant2 로 정렬된 두 참여자로 대화 조회 (중복 생성 방지)
    Optional<Conversation> findByParticipant1IdAndParticipant2Id(UUID participant1Id, UUID participant2Id);

    // 내가 참여한 대화 목록
    @Query("select c from Conversation c "
            + "where c.participant1.id = :userId or c.participant2.id = :userId")
    List<Conversation> findAllByParticipantId(@Param("userId") UUID userId);
}
