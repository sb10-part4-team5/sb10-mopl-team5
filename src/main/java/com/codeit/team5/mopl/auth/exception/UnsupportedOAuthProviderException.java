package com.codeit.team5.mopl.auth.exception;

import org.springframework.http.HttpStatus;

public class UnsupportedOAuthProviderException extends AuthException {

    public UnsupportedOAuthProviderException(String registrationId) {
        super(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "지원하지 않는 OAuth provider 입니다: " + registrationId
        );
    }
}
