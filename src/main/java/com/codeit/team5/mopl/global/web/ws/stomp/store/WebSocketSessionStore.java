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
        session.put(email, new ConcurrentHashMap<>());
    }

    public void subscribe(String email, String subscriptionId, String destination) {
        session.computeIfAbsent(email, e -> new ConcurrentHashMap<>())
                .put(subscriptionId, destination);
    }

    public void unsubscribe(String email, String subscriptionId) {
        session.computeIfPresent(email, (k, innerMap) -> removeIfEmpty(innerMap, subscriptionId));
    }

    public void disconnect(String email) {
        session.remove(email);
    }

    private Map<String, String> removeIfEmpty(Map<String, String> innerMap, String subscriptionId) {
        innerMap.remove(subscriptionId);
        return innerMap.isEmpty() ? null : innerMap;
    }

    public String getDestination(String email, String subscriptionId) {
        return session.getOrDefault(email, Collections.emptyMap()).get(subscriptionId);
    }
}
