package com.codeit.team5.mopl.global.web.ws.stomp.store;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class WebSocketSessionStore {

    // userId - (subscriptionId - StompDestination)
    private final Map<UUID, Map<String, StompDestination>> session = new ConcurrentHashMap<>();

    public void connect(UUID userId) {
        session.putIfAbsent(userId, new ConcurrentHashMap<>());
    }

    public void subscribe(UUID userId, String subscriptionId, StompDestination stompDestination) {
        session.compute(userId,
            (k, innerMap) -> addSubscription(innerMap, subscriptionId, stompDestination));
    }

    public void unsubscribe(UUID userId, String subscriptionId) {
        session.computeIfPresent(userId, (k, innerMap) -> removeIfEmpty(innerMap, subscriptionId));
    }

    public void disconnect(UUID userId) {
        session.remove(userId);
    }

    public Optional<StompDestination> getDestination(UUID userId,
        String subscriptionId) {
        return Optional.ofNullable(session.getOrDefault(userId, Collections.emptyMap())
            .getOrDefault(subscriptionId, null));
    }

    // 해당 사용자가 특정 destination을 구독 중인지 (활성 여부 판단)
    public boolean isSubscribed(UUID userId, String destination) {
        Map<String, StompDestination> userDestinations =
            session.getOrDefault(userId, Collections.emptyMap());
        return userDestinations.values().stream()
            .anyMatch(d -> d.destinationPattern().equals(destination));
    }

    public Collection<StompDestination> getAllDestination(UUID userId) {
        return session.getOrDefault(userId, Collections.emptyMap()).values();
    }

    private Map<String, StompDestination> addSubscription(
        Map<String, StompDestination> innerMap, String subscriptionId,
        StompDestination stompDestination) {

        Map<String, StompDestination> map =
            (innerMap != null) ? innerMap : new ConcurrentHashMap<>();
        map.put(subscriptionId, stompDestination);
        return map;
    }

    private Map<String, StompDestination> removeIfEmpty(
        Map<String, StompDestination> innerMap, String subscriptionId) {
        innerMap.remove(subscriptionId);
        return innerMap.isEmpty() ? null : innerMap;
    }

    public record StompDestination(String destinationPattern, UUID targetId) implements Serializable {

        public String getPattern() {
            return destinationPattern.replace("{id}", "*");
        }

        public String destinationPattern() {
            return this.destinationPattern.replace("{id}", this.targetId.toString());
        }
    }
}
