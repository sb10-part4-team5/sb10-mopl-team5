package com.codeit.team5.mopl.user.event;

import java.util.UUID;

public record UserLockedEvent(UUID id, boolean locked) {

}
