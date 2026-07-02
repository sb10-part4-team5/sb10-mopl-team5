package com.codeit.team5.mopl.dm.entity;

import com.codeit.team5.mopl.dm.exception.InvalidDirectMessageContentException;
import com.codeit.team5.mopl.global.entity.BaseEntity;
import com.codeit.team5.mopl.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Check;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Check(
        name = "ck_dm_read_state",
        constraints = "(is_read = false AND read_at IS NULL) OR (is_read = true AND read_at IS NOT NULL)"
)
@Table(name = "direct_messages")
public class DirectMessage extends BaseEntity {

    private static final int MAX_CONTENT_LENGTH = 1000;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "is_read", nullable = false)
    private boolean read;

    @Column(name = "read_at")
    private Instant readAt;

    private DirectMessage(Conversation conversation, User sender, User receiver, String content) {
        this.conversation = conversation;
        this.sender = sender;
        this.receiver = receiver;
        this.content = content;
    }

    public static DirectMessage create(Conversation conversation, User sender, String content) {
        if (content == null || content.isBlank() || content.length() > MAX_CONTENT_LENGTH) {
            throw new InvalidDirectMessageContentException();
        }
        User receiver = conversation.getOtherParticipant(sender);
        return new DirectMessage(
                conversation,
                sender,
                receiver,
                content
        );
    }

    public boolean isInConversation(UUID conversationId) {
        return conversation.getId().equals(conversationId);
    }

    public void markAsRead() {
        if (read) {
            return;
        }
        this.read = true;
        this.readAt = Instant.now();
    }
}
