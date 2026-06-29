package com.codeit.team5.mopl.auth.security.details;

import com.codeit.team5.mopl.user.dto.response.UserResponse;
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

    private final UserResponse userDto;
    private final String password;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(
                new SimpleGrantedAuthority("ROLE_" + userDto.role())
        );
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return userDto.email();
    }

    @Override
    public boolean isAccountNonLocked() {
        return !userDto.locked();
    }

    public UUID getId() {
        return userDto.id();
    }

    @Override
    public String getEmail() {
        return userDto.email();
    }

    public String getRole() {
        return userDto.role();
    }

    @Override
    public boolean isLocked() {
        return userDto.locked();
    }
}
