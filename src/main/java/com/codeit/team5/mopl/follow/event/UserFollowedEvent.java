package com.codeit.team5.mopl.follow.event;

import com.codeit.team5.mopl.notification.exception.InvalidNicknameException;
import com.codeit.team5.mopl.notification.exception.InvalidReceiverIdException;
import java.util.UUID;
import org.springframework.util.StringUtils;

public record UserFollowedEvent(
    UUID receiverId,
    String userName
) {
    public UserFollowedEvent {
        if (receiverId == null) {
            throw new InvalidReceiverIdException();
        }
        if (!StringUtils.hasText(userName)) {
            throw new InvalidNicknameException();
        }
    }
}
