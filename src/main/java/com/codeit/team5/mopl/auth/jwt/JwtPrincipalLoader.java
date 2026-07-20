package com.codeit.team5.mopl.auth.jwt;

import com.codeit.team5.mopl.auth.security.details.MoplUserDetails;
import com.codeit.team5.mopl.auth.security.details.MoplUserDetailsService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwtPrincipalLoader {

    private static final String AUTH_USER_LOOKUP_TIMER =
            "auth.user.lookup";

    private final MoplUserDetailsService userDetailsService;
    private final MeterRegistry meterRegistry;

    public MoplUserDetails loadByUserId(UUID userId) {
        Timer.Sample sample = Timer.start(meterRegistry);
        String result = "success";

        try {
            return (MoplUserDetails) userDetailsService.loadUserById(userId);
        } catch (RuntimeException e) {
            result = "failure";
            throw e;
        } finally {
            sample.stop(
                    meterRegistry.timer(
                            AUTH_USER_LOOKUP_TIMER,
                            "source", "db",
                            "result", result
                    )
            );
        }
    }
}
