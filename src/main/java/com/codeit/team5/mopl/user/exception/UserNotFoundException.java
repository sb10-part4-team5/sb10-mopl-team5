package com.codeit.team5.mopl.user.exception;

import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class UserNotFoundException extends UserException {

    public UserNotFoundException(UUID userId) {
        super(HttpStatus.NOT_FOUND, "사용자가 존재하지 않습니다.", Map.of("userId", userId));
    }

  public UserNotFoundException(String userInfo) {
    super(HttpStatus.NOT_FOUND, "사용자가 존재하지 않습니다.", Map.of("userInfo", userInfo));
  }
}
