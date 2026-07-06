package com.codeit.team5.mopl.user.exception;

import java.util.Map;
import org.springframework.http.HttpStatus;

public class SameRoleAssignmentException extends UserException {

    public SameRoleAssignmentException(String currentRole) {
        super(HttpStatus.CONFLICT, "현재 사용자의 역할과 변경할 역할이 동일합니다.", Map.of("currentRole", currentRole));
    }
}
