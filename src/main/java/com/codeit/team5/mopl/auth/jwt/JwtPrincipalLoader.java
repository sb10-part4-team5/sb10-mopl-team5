package com.codeit.team5.mopl.auth.jwt;

import com.codeit.team5.mopl.auth.jwt.cache.AuthUserCacheStore;
import com.codeit.team5.mopl.auth.security.details.AuthUser;
import com.codeit.team5.mopl.auth.security.details.MoplUserDetails;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwtPrincipalLoader {

    private final AuthUserCacheStore authUserCacheStore;

    public MoplUserDetails loadByUserId(UUID userId) {
        AuthUser authUser = authUserCacheStore.getByUserId(userId);

        return MoplUserDetails.forJwt(authUser);
    }
}
