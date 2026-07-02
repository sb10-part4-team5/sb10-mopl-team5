package com.codeit.team5.mopl.auth.security.provider;

import com.codeit.team5.mopl.auth.exception.InvalidCredentialsException;
import com.codeit.team5.mopl.auth.security.details.MoplUserDetails;
import com.codeit.team5.mopl.auth.security.details.MoplUserDetailsService;
import com.codeit.team5.mopl.auth.service.TemporaryPasswordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MoplAuthenticationProvider implements AuthenticationProvider {

    private final MoplUserDetailsService userDetailsService;
    private final TemporaryPasswordService temporaryPasswordService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Authentication authenticate(Authentication authentication)
            throws AuthenticationException {
        String email = authentication.getName();
        Object credentials = authentication.getCredentials();
        if (credentials == null) {
            throw new BadCredentialsException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }
        String rawPassword = credentials.toString();

        MoplUserDetails userDetails;

        try {
            userDetails = (MoplUserDetails) userDetailsService.loadUserByUsername(email);
        } catch (InvalidCredentialsException e) {
            throw new BadCredentialsException("이메일 또는 비밀번호가 올바르지 않습니다.", e);
        }

        if (!userDetails.isAccountNonLocked()) {
            throw new LockedException("잠긴 계정입니다.");
        }

        boolean matchesPassword =
                passwordEncoder.matches(rawPassword, userDetails.getPassword());

        boolean matchesTemporaryPassword =
                !matchesPassword
                        && temporaryPasswordService.matches(userDetails.getId(), rawPassword);

        if (!matchesPassword && !matchesTemporaryPassword) {
            throw new BadCredentialsException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        // 임시 비밀번호 인증 성공 시 재사용 방지를 위해 즉시 폐기
        if (matchesTemporaryPassword) {
            temporaryPasswordService.deleteByUserId(userDetails.getId());
        }

        return new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
