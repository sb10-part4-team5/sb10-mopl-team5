package com.codeit.team5.mopl.subscription.repository;

import com.codeit.team5.mopl.subscription.entity.Subscription;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

    boolean existsBySubscriberEmailAndPlaylistId(String email, UUID playlistId);

    @Modifying
    @Query("delete from Subscription s where s.subscriber.email = :email and s.playlist.id = :playlistId")
    void deleteBySubscriberEmailAndPlaylistIdDirectly(String email, UUID playlistId);
}
