package com.codeit.team5.mopl.dm.repository;

import com.codeit.team5.mopl.dm.entity.Conversation;
import com.codeit.team5.mopl.dm.repository.querydsl.ConversationQueryRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationRepository extends JpaRepository<Conversation, UUID>, ConversationQueryRepository {

    @Override
    @EntityGraph(attributePaths = {"participant1.profileImage", "participant2.profileImage"})
    Optional<Conversation> findById(UUID id);

    // participant1 < participant2 로 정렬된 두 참여자로 대화 조회 (중복 생성 방지)
    @EntityGraph(attributePaths = {"participant1.profileImage", "participant2.profileImage"})
    Optional<Conversation> findByParticipant1IdAndParticipant2Id(UUID participant1Id, UUID participant2Id);
}
