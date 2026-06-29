package com.codeit.team5.mopl.playlist.entity.event;

import java.util.UUID;

public record PlaylistSubscribedEvent(
    UUID receiverId,
    String subscriberNickname,
    String playlistName
) {

}
