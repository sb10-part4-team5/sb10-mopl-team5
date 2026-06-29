package com.codeit.team5.mopl.notification.event;

import java.util.UUID;

public record RoleChangedEvent(
    UUID receiverId,
    String roleBefore,
    String roleAfter
) {

}
