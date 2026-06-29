package com.codeit.team5.mopl.auth.exception;

import org.springframework.http.HttpStatus;

public class RefreshTokenInvalidException extends AuthException {

  public RefreshTokenInvalidException(String message) {
    super(HttpStatus.UNAUTHORIZED, message);
  }
}
