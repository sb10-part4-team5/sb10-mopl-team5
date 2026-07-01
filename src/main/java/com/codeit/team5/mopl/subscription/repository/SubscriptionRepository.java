package com.codeit.team5.mopl.subscription.repository;

import com.codeit.team5.mopl.subscription.entity.Subscription;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

}
