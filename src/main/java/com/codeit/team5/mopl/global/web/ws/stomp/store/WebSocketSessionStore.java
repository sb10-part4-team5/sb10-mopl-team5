package com.codeit.team5.mopl.global.web.ws.stomp.store;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class WebSocketSessionStore {

    // userId - (sessionId - destination)
    private final Map<UUID, Map<String, String>> session = new ConcurrentHashMap<>();

    public void connect(UUID userId) {
        session.putIfAbsent(userId, new ConcurrentHashMap<>());
    }

    public void subscribe(UUID userId, String subscriptionId, String destination) {
        session.compute(userId,
                (k, innerMap) -> addSubscription(innerMap, subscriptionId, destination));
    }

    public void unsubscribe(UUID userId, String subscriptionId) {
        session.computeIfPresent(userId, (k, innerMap) -> removeIfEmpty(innerMap, subscriptionId));
    }

    public void disconnect(UUID userId) {
        session.remove(userId);
    }

    public String getDestination(UUID userId, String subscriptionId) {
        return session.getOrDefault(userId, Collections.emptyMap()).getOrDefault(subscriptionId, null);
    }

    // 해당 사용자가 특정 destination을 구독 중인지 (활성 여부 판단)
    public boolean isSubscribed(UUID userId, String destination) {
        return session.getOrDefault(userId, Collections.emptyMap()).containsValue(destination);
    }

    private Map<String, String> addSubscription(Map<String, String> innerMap, String subscriptionId,
            String destination) {
        Map<String, String> map = (innerMap != null) ? innerMap : new ConcurrentHashMap<>();
        map.put(subscriptionId, destination);
        return map;
    }

    private Map<String, String> removeIfEmpty(Map<String, String> innerMap, String subscriptionId) {
        innerMap.remove(subscriptionId);
        return innerMap.isEmpty() ? null : innerMap;
    }

}
