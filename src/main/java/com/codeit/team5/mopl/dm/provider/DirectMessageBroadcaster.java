package com.codeit.team5.mopl.dm.provider;

import com.codeit.team5.mopl.dm.dto.response.DirectMessageResponse;

public interface DirectMessageBroadcaster {

    void broadcast(DirectMessageResponse message);
}
