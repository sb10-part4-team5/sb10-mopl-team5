package com.codeit.team5.mopl.user.exception;

import com.codeit.team5.mopl.global.exception.ErrorCode;

public class SameRoleAssignmentException extends UserException {

    public SameRoleAssignmentException() {
        super(ErrorCode.INVALID_ROLE_CHANGE);
    }
}
