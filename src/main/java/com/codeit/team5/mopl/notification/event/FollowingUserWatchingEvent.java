package com.codeit.team5.mopl.notification.event;

import com.codeit.team5.mopl.notification.exception.InvalidContentException;
import com.codeit.team5.mopl.notification.exception.InvalidNicknameException;
import com.codeit.team5.mopl.notification.exception.InvalidReceiverIdException;
import java.util.UUID;
import org.springframework.util.StringUtils;

public record FollowingUserWatchingEvent(
    UUID receiverId,
    String userNickname,
    String contentName
) {
    public FollowingUserWatchingEvent {
        if (receiverId == null){
            throw new InvalidReceiverIdException();
        }
        if (!StringUtils.hasText(userNickname)) {
            throw new InvalidNicknameException();
        }
        if (!StringUtils.hasText(contentName)){
            throw new InvalidContentException();
        }
    }

}
