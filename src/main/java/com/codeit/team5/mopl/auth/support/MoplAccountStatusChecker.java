package com.codeit.team5.mopl.auth.support;

import com.codeit.team5.mopl.auth.security.details.AuthUser;
import com.codeit.team5.mopl.auth.security.details.MoplUserDetails;
import org.springframework.security.authentication.LockedException;
import org.springframework.stereotype.Component;

@Component
public class MoplAccountStatusChecker {

    public void check(AuthUser authUser) {
        if (authUser.locked()) {
            throw new LockedException("잠긴 계정입니다.");
        }
    }

    public void check(MoplUserDetails userDetails) {
        if (!userDetails.isAccountNonLocked()) {
            throw new LockedException("잠긴 계정입니다.");
        }
    }
}
