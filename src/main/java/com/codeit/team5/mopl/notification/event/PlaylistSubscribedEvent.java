package com.codeit.team5.mopl.notification.event;

import java.util.UUID;

public record PlaylistSubscribedEvent(
    UUID receiverId,
    String subscriberNickname,
    String playlistName
) {

}
