package com.codeit.team5.mopl.config;

import com.codeit.team5.mopl.auth.jwt.JwtAuthenticationFilter;
import com.codeit.team5.mopl.auth.jwt.JwtAuthenticationService;
import com.codeit.team5.mopl.auth.security.details.oauth.MoplOAuth2UserService;
import com.codeit.team5.mopl.auth.security.handler.signin.OAuth2SignInSuccessHandler;
import com.codeit.team5.mopl.auth.security.handler.signin.SignInFailureHandler;
import com.codeit.team5.mopl.auth.security.handler.signin.SignInSuccessHandler;
import com.codeit.team5.mopl.auth.security.handler.SpaCsrfTokenRequestHandler;
import com.codeit.team5.mopl.auth.security.handler.UserAccessDeniedHandler;
import com.codeit.team5.mopl.auth.security.handler.UserAuthenticationEntryPoint;
import com.codeit.team5.mopl.auth.security.handler.signin.OAuth2SignInFailureHandler;
import com.codeit.team5.mopl.auth.security.handler.signout.SignOutHandler;
import com.codeit.team5.mopl.auth.security.provider.MoplAuthenticationProvider;
import com.codeit.team5.mopl.auth.support.HttpCookieOAuth2AuthorizationRequestRepository;
import jakarta.servlet.DispatcherType;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationService jwtAuthenticationService;
    private final MoplOAuth2UserService moplOAuth2UserService;
    private final HttpCookieOAuth2AuthorizationRequestRepository httpCookieOAuth2AuthorizationRequestRepository;
    private final UserAuthenticationEntryPoint userAuthenticationEntryPoint;
    private final UserAccessDeniedHandler userAccessDeniedHandler;
    private final MoplAuthenticationProvider moplAuthenticationProvider;
    private final OAuth2SignInSuccessHandler oauth2SignInSuccessHandler;
    private final OAuth2SignInFailureHandler oauth2SignInFailureHandler;
    private final SignInSuccessHandler signInSuccessHandler;
    private final SignInFailureHandler signInFailureHandler;
    private final SignOutHandler signOutHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        PathPatternRequestMatcher.Builder paths =
                PathPatternRequestMatcher.withDefaults();

        return http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler())
                        .ignoringRequestMatchers(
                                paths.matcher(HttpMethod.POST, "/api/auth/refresh")
                        )
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authenticationProvider(moplAuthenticationProvider)
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(userAuthenticationEntryPoint)
                        .accessDeniedHandler(userAccessDeniedHandler)
                )
                .authorizeHttpRequests(auth -> auth
                        // SSE ASYNC 재디스패치 시 JWT 필터가 건너뛰어 AuthorizationFilter에서 Access Denied가 발생하므로 ASYNC 타입은 전체 허용
                        .dispatcherTypeMatchers(DispatcherType.ASYNC).permitAll()

                        .requestMatchers("/api/auth/sign-out").permitAll()

                        .requestMatchers(
                                "/oauth2/**",
                                "/login/oauth2/**"
                        ).permitAll()

                        .requestMatchers(HttpMethod.POST, "/api/users").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/users").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/users/*/role").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/users/*/locked").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/users/*").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/api/users/*").authenticated()

                        .requestMatchers(HttpMethod.GET, "/api/contents", "/api/contents/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/contents").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/contents/*").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/contents/*").hasRole("ADMIN")
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        .requestMatchers("/api/users/**").authenticated()
                        .requestMatchers("/api/follows/**").authenticated()
                        .requestMatchers("/api/conversations/**").authenticated()
                        .requestMatchers("/api/notifications/**").authenticated()
                        .requestMatchers("/api/sse/**").authenticated()
                        .requestMatchers("/api/reviews/**").authenticated()
                        .requestMatchers("/api/playlists/**").authenticated()

                        .requestMatchers(HttpMethod.POST, "/api/auth/reset-password").permitAll()
                        .requestMatchers("/api/auth/sign-in").permitAll()
                        .requestMatchers("/api/auth/csrf-token").permitAll()
                        .requestMatchers("/api/auth/refresh").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()

                        .anyRequest().permitAll()
                )
                .formLogin(login -> login
                        .loginProcessingUrl("/api/auth/sign-in")
                        .successHandler(signInSuccessHandler)
                        .failureHandler(signInFailureHandler)
                )
                .oauth2Login(oauth -> oauth
                        .authorizationEndpoint(authorization ->
                                authorization.authorizationRequestRepository(
                                        httpCookieOAuth2AuthorizationRequestRepository
                                )
                        )
                        .userInfoEndpoint(userInfo ->
                                userInfo.userService(moplOAuth2UserService)
                        )
                        .successHandler(oauth2SignInSuccessHandler)
                        .failureHandler(oauth2SignInFailureHandler)
                )
                .addFilterBefore(
                        jwtAuthenticationFilter(),
                        UsernamePasswordAuthenticationFilter.class
                )
                .logout(logout -> logout
                        .logoutUrl("/api/auth/sign-out")
                        .addLogoutHandler(signOutHandler)
                        .logoutSuccessHandler(
                                new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT)
                        )
                )
                .build();
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtAuthenticationService);
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authenticationConfiguration
    ) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }
}
