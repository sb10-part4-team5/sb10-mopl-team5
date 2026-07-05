package com.codeit.team5.mopl.global.support.security;

import com.codeit.team5.mopl.auth.security.details.AuthUser;
import com.codeit.team5.mopl.auth.security.details.MoplUserDetails;
import java.util.UUID;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

/**
 * 통합 테스트에서 {@code SecurityMockMvcRequestPostProcessors.authentication(...)}에 주입할
 * {@link Authentication}을 만들기 위한 공용 헬퍼. 여러 컨트롤러 통합 테스트에서 반복되던
 * ADMIN/USER 인증 객체 생성 로직을 한 곳으로 모은다.
 */
public final class IntegrationTestSecuritySupport {

    private IntegrationTestSecuritySupport() {
    }

    public static Authentication adminAuthentication() {
        return authenticationOf("admin@mopl.com", "ADMIN");
    }

    public static Authentication userAuthentication() {
        return authenticationOf("user@mopl.com", "USER");
    }

    public static Authentication authenticationOf(String email, String role) {
        return authenticationOf(UUID.randomUUID(), email, role);
    }

    public static Authentication authenticationOf(UUID userId, String email, String role) {
        MoplUserDetails details = new MoplUserDetails(new AuthUser(userId, email, role, false), "password");
        return new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities());
    }
}
