package com.codeit.team5.mopl.auth.repository;

import com.codeit.team5.mopl.auth.entity.SocialAccount;
import com.codeit.team5.mopl.auth.entity.SocialProvider;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SocialAccountRepository extends JpaRepository<SocialAccount, UUID> {

    Optional<SocialAccount> findByProviderAndProviderUserId(
            SocialProvider provider,
            String providerUserId
    );

    boolean existsByProviderAndProviderUserId(
            SocialProvider provider,
            String providerUserId
    );
}
