package com.codeit.team5.mopl.notification.event;

import java.util.UUID;

public record UserFollowedEvent(
    UUID receiverId,
    String userName
) {

}
