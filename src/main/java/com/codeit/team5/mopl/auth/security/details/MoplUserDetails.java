package com.codeit.team5.mopl.auth.security.details;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Getter
@RequiredArgsConstructor
public class MoplUserDetails implements UserDetails, MoplPrincipal {

    private final AuthUser authUser;
    private final String password;

    // JWT 인증에서는 비밀번호를 사용하지 않으므로 password는 null
    public static MoplUserDetails forJwt(AuthUser authUser) {
        return new MoplUserDetails(authUser, null);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(
                new SimpleGrantedAuthority("ROLE_" + authUser.role())
        );
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return authUser.id().toString();
    }

    @Override
    public boolean isAccountNonLocked() {
        return !authUser.locked();
    }

    public UUID getId() {
        return authUser.id();
    }

    @Override
    public String getEmail() {
        return authUser.email();
    }

    public String getRole() {
        return authUser.role();
    }

    @Override
    public boolean isLocked() {
        return authUser.locked();
    }
}
