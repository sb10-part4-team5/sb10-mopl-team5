package com.codeit.team5.mopl.config;

import com.codeit.team5.mopl.auth.filter.JwtAuthenticationFilter;
import com.codeit.team5.mopl.auth.handler.UserAccessDeniedHandler;
import com.codeit.team5.mopl.auth.handler.UserAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final UserAuthenticationEntryPoint userAuthenticationEntryPoint;
    private final UserAccessDeniedHandler userAccessDeniedHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(userAuthenticationEntryPoint)
                        .accessDeniedHandler(userAccessDeniedHandler)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/sign-out").authenticated()
                        .requestMatchers("/api/users").permitAll()
                        .requestMatchers("/api/auth/sign-in").permitAll()

                        // Swagger, actuator 필요하면 추가
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()

                        .anyRequest().permitAll()
                )
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                )
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
