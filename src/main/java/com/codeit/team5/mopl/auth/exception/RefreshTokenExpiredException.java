package com.codeit.team5.mopl.auth.exception;

public class RefreshTokenExpiredException extends RefreshTokenException {

    public RefreshTokenExpiredException() {
        super("만료된 refreshToken 입니다.");
    }
}
