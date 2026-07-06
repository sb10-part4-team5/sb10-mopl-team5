package com.codeit.team5.mopl.global.web.ws.stomp.store;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class WebSocketSessionStore {

    // email - (sessionId - destination)
    private final Map<String, Map<String, String>> session = new ConcurrentHashMap<>();

    public void connect(String email) {
        session.putIfAbsent(email, new ConcurrentHashMap<>());
    }

    public void subscribe(String email, String subscriptionId, String destination) {
        session.compute(email,
                (k, innerMap) -> addSubscription(innerMap, subscriptionId, destination));
    }

    public void unsubscribe(String email, String subscriptionId) {
        session.computeIfPresent(email, (k, innerMap) -> removeIfEmpty(innerMap, subscriptionId));
    }

    public void disconnect(String email) {
        session.remove(email);
    }

    public String getDestination(String email, String subscriptionId) {
        return session.getOrDefault(email, Collections.emptyMap()).get(subscriptionId);
    }

    // 해당 사용자가 특정 destination을 구독 중인지 (활성 여부 판단)
    public boolean isSubscribed(String email, String destination) {
        return session.getOrDefault(email, Collections.emptyMap()).containsValue(destination);
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
