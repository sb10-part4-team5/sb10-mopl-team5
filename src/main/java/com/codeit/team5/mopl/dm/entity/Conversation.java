package com.codeit.team5.mopl.dm.entity;

import com.codeit.team5.mopl.dm.exception.NotConversationParticipantException;
import com.codeit.team5.mopl.dm.exception.SelfConversationException;
import com.codeit.team5.mopl.dm.util.UuidUtils;
import com.codeit.team5.mopl.global.entity.BaseEntity;
import com.codeit.team5.mopl.user.entity.User;
import java.util.UUID;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.Immutable;

@Entity
@Getter
@Immutable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Check(name = "ck_conv_order", constraints = "participant1 < participant2")
@Table(
        name = "conversations",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_conversations_participant1_participant2",
                columnNames = {"participant1", "participant2"}
        )
)
public class Conversation extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "participant1", nullable = false)
    private User participant1;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "participant2", nullable = false)
    private User participant2;

    private Conversation(User participant1, User participant2) {
        this.participant1 = participant1;
        this.participant2 = participant2;
    }

    public static Conversation create(User userA, User userB) {
        if (userA.equals(userB)) {
            throw new SelfConversationException(userA.getId());
        }
        if (UuidUtils.compareUnsigned(userA.getId(), userB.getId()) < 0) {
            return new Conversation(userA, userB);
        }
        return new Conversation(userB, userA);
    }

    public User getOtherParticipant(User user) {
        if (participant1.equals(user)) {
            return participant2;
        }
        if (participant2.equals(user)) {
            return participant1;
        }
        throw new NotConversationParticipantException(user.getId());
    }

    public void validateParticipant(UUID userId) {
        if (!participant1.getId().equals(userId) && !participant2.getId().equals(userId)) {
            throw new NotConversationParticipantException(userId);
        }
    }
}
