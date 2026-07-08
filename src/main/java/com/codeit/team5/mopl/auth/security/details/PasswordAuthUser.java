package com.codeit.team5.mopl.auth.security.details;

public record PasswordAuthUser(
        AuthUser authUser,
        String password
) { }
