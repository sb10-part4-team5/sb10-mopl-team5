package com.codeit.team5.mopl.auth.security.details.oauth;

import com.codeit.team5.mopl.auth.security.details.AuthUser;
import com.codeit.team5.mopl.auth.security.details.MoplPrincipal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

@RequiredArgsConstructor
public class MoplOAuth2User implements OAuth2User, MoplPrincipal {

    private final AuthUser authUser;
    private final Map<String, Object> attributes;

    @Override
    public UUID getId() {
        return authUser.id();
    }

    @Override
    public String getEmail() {
        return authUser.email();
    }

    @Override
    public String getRole() {
        return authUser.role();
    }

    @Override
    public boolean isLocked() {
        return authUser.locked();
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + authUser.role()));
    }

    @Override
    public String getName() {
        return authUser.id().toString();
    }
}
