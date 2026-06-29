package com.codeit.team5.mopl;

import com.codeit.team5.mopl.auth.cookie.RefreshTokenCookieManager;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class TestGlobalExceptionHandlerConfig {

    @Bean
    public RefreshTokenCookieManager refreshTokenCookieManager() {
        return Mockito.mock(RefreshTokenCookieManager.class);
    }
}
