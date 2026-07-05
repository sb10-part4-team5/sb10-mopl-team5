package com.codeit.team5.mopl.dm.provider;

import java.util.UUID;

public interface ActiveConversationChecker {

    boolean isViewing(UUID conversationId, UUID userId);
}
