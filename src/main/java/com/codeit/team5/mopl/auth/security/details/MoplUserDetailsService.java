package com.codeit.team5.mopl.auth.security.details;

import com.codeit.team5.mopl.auth.exception.InvalidCredentialsException;
import com.codeit.team5.mopl.auth.support.EmailNormalizer;
import com.codeit.team5.mopl.auth.support.MoplAccountStatusChecker;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

// 일반 로그인 & JWT용 어댑터 = MoplUserDetails 반환
@Service
@RequiredArgsConstructor
public class MoplUserDetailsService implements UserDetailsService {

    private final MoplPrincipalService moplPrincipalService;
    private final MoplAccountStatusChecker moplAccountStatusChecker;

    // form 로그인용 email 조회 기준 메서드
    @Override
    public UserDetails loadUserByUsername(String email) throws InvalidCredentialsException {
        String normalizeEmail = EmailNormalizer.normalize(email);
        PasswordAuthUser passwordAuthUser = moplPrincipalService.loadAuthUserWithPasswordByEmail(normalizeEmail);
        moplAccountStatusChecker.check(passwordAuthUser.authUser());

        return new MoplUserDetails(passwordAuthUser.authUser(), passwordAuthUser.password());
    }

    // JWT용 (식별자가 id값이라서)
    public UserDetails loadUserById(UUID userId) throws InvalidCredentialsException {
        PasswordAuthUser passwordAuthUser = moplPrincipalService.loadAuthUserWithPasswordById(userId);
        moplAccountStatusChecker.check(passwordAuthUser.authUser());

        return new MoplUserDetails(passwordAuthUser.authUser(), passwordAuthUser.password());
    }
}
