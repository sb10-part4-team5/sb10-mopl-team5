package com.codeit.team5.mopl.user.exception;

import com.codeit.team5.mopl.global.exception.ErrorCode;

public class SameLockStatusException extends UserException {

    public SameLockStatusException() {
        super(ErrorCode.LOCK_STATUS_ALREADY_SET);
    }
}
