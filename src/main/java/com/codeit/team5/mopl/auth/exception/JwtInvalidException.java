package com.codeit.team5.mopl.auth.exception;

import java.util.Map;
import org.springframework.http.HttpStatus;

public class JwtInvalidException extends AuthException {

    public JwtInvalidException(String message) {
        super(HttpStatus.UNAUTHORIZED, message);
    }

    public JwtInvalidException(String message, String detail) {
        super(HttpStatus.UNAUTHORIZED, message, Map.of("detail", detail));
    }
}
