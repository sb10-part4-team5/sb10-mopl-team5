package com.codeit.team5.mopl.auth.exception;

public class OAuth2AuthorizationRequestCookieException extends RuntimeException {

    public OAuth2AuthorizationRequestCookieException(String message) {
        super(message);
    }

    public OAuth2AuthorizationRequestCookieException(String message, Throwable cause) {
        super(message, cause);
    }
}
