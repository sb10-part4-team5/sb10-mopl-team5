package com.codeit.team5.mopl.auth.exception;

public class RefreshTokenNotFoundException extends RefreshTokenException {

    public RefreshTokenNotFoundException() {
        super("refreshToken이 존재하지 않습니다.");
    }
}
