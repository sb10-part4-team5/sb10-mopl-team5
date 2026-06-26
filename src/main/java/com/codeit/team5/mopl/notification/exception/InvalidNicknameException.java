package com.codeit.team5.mopl.notification.exception;

import com.codeit.team5.mopl.global.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class InvalidNicknameException extends BusinessException {

    public InvalidNicknameException(String nickname) {

        super(HttpStatus.BAD_REQUEST, "사용자 nickname이 유효하지 않음. nickname = {" + nickname + "}");
    }
}
