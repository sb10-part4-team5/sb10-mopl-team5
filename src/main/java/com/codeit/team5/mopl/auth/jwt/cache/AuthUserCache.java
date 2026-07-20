package com.codeit.team5.mopl.auth.jwt.cache;

import com.codeit.team5.mopl.auth.security.details.AuthUser;
import java.util.Optional;
import java.util.UUID;

public interface AuthUserCache {

    Optional<AuthUser> get(UUID userId);

    void put(AuthUser authUser);

    void evict(UUID userId);
}
