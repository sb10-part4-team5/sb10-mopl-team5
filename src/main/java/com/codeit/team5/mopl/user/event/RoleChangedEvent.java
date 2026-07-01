package com.codeit.team5.mopl.user.event;

import java.util.UUID;

public record RoleChangedEvent(
    UUID receiverId,
    String roleBefore,
    String roleAfter
) {

}
