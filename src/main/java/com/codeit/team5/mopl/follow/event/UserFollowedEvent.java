package com.codeit.team5.mopl.follow.event;

import java.util.UUID;

public record UserFollowedEvent(
    UUID receiverId,
    String userName
) {

}
