package com.codeit.team5.mopl;

import static org.mockito.Mockito.mock;

import com.codeit.team5.mopl.auth.security.details.oauth.MoplOAuth2UserService;
import com.codeit.team5.mopl.auth.security.handler.signin.OAuth2SignInFailureHandler;
import com.codeit.team5.mopl.auth.security.handler.signin.OAuth2SignInSuccessHandler;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class ControllerSliceSecurityMockConfig {

    @Bean
    MoplOAuth2UserService moplOAuth2UserService() {
        return mock(MoplOAuth2UserService.class);
    }

    @Bean
    OAuth2SignInSuccessHandler oAuth2SignInSuccessHandler() {
        return mock(OAuth2SignInSuccessHandler.class);
    }

    @Bean
    OAuth2SignInFailureHandler oAuth2SignInFailureHandler() {
        return mock(OAuth2SignInFailureHandler.class);
    }
}
