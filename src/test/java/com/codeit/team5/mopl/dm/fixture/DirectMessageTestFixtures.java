package com.codeit.team5.mopl.dm.fixture;

import com.codeit.team5.mopl.dm.dto.response.DirectMessageResponse;
import com.codeit.team5.mopl.user.dto.response.UserSummaryResponse;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DirectMessageTestFixtures {

    public static DirectMessageResponse dmMessage(UUID receiverId) {
        return dmMessage(receiverId, "다린", "안녕하세요");
    }

    public static DirectMessageResponse dmMessage(UUID receiverId, String senderName, String content) {
        UserSummaryResponse sender = new UserSummaryResponse(UUID.randomUUID(), senderName, null);
        UserSummaryResponse receiver = new UserSummaryResponse(receiverId, "받는이", null);
        return new DirectMessageResponse(
                UUID.randomUUID(), UUID.randomUUID(), sender, receiver, content, Instant.now());
    }
}
