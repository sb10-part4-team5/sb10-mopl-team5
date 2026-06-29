package com.codeit.team5.mopl.notification.event;

import java.util.UUID;

public record FollowingUserWatchingEvent(
    UUID receiverId,
    String userNickname,
    String contentName
) {

}
